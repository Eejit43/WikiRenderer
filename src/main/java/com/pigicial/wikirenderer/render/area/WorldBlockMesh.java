package com.pigicial.wikirenderer.render.area;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.OrthographicSort;
import com.pigicial.wikirenderer.render.area.bounds.MeshBounds;
import com.pigicial.wikirenderer.render.area.side_view.WalkabilityFilter;
import com.pigicial.wikirenderer.util.compatibility.ShaderCheck;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.TextureFilteringMethod;
import net.minecraft.client.renderer.DynamicUniforms;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BeaconRenderState;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.chunk.*;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Util;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.Zone;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

// todo: not a fan of how entities are handled in AreaRenderable and blocks are here, maybe they should be merged
public class WorldBlockMesh {

    public static boolean overrideTerrainTransparencyRenderPipelines = false;
    public static GpuSampler terrainSampler = null;

    public final MeshWorldOverrides world;
    public final MeshBounds bounds;
    private AreaRenderable renderable;

    private final SectionRenderDispatcher sectionRenderDispatcher;
    protected final SectionBufferBuilderPack resortBufferPack = new SectionBufferBuilderPack();
    public final Map<Long, MeshRenderSection> subMeshes = new ConcurrentHashMap<>();

    private MeshState state = MeshState.NEW;
    protected OrthographicSort orthographicTransparencySorting = null;
    private volatile CompletableFuture<Void> sortFuture = null;

    private boolean currentlyFullyBuilding = false; // initial build or rebuilds, not automatic ones from chunk updates
    private float fullBuildProgress = 0;
    protected volatile boolean buildingCancelled = false;
    private boolean isAutomaticUpdateScheduled = false;

    private float lastUsedRotation;
    private double lastUsedSlant;

    public WorldBlockMesh(
            BlockAndTintGetter world,
            MeshBounds bounds
    ) {
        this.bounds = bounds;
        this.world = new MeshWorldOverrides(world, bounds);
        this.lastUsedRotation = Float.MAX_VALUE;
        this.lastUsedSlant = Double.MAX_VALUE;

        Minecraft client = Minecraft.getInstance();
        this.sectionRenderDispatcher = new SectionRenderDispatcher(client.level, client.levelRenderer, Util.backgroundExecutor(), client.renderBuffers(), null);
    }

    public void setRenderable(AreaRenderable renderable) {
        this.renderable = renderable;
    }

    public MeshState getMeshState() {
        return this.state;
    }

    public float getBuildProgress() {
        return this.fullBuildProgress;
    }

    public boolean canRebuild() {
        return !currentlyFullyBuilding;
    }

    public void drawBlocks(PoseStack matrices, Runnable preTranslucencyTask) {
        if (!this.getMeshState().canRender) {
            throw new IllegalStateException("World mesh not prepared!");
        }

        this.updateBuildingStatus();
        this.updateOutdatedMeshSections();

        if (terrainSampler == null) {
            Options options = Minecraft.getInstance().options;
            int maxAnisotropy = options.textureFiltering().get() == TextureFilteringMethod.ANISOTROPIC ? options.maxAnisotropyValue() : 1;
            terrainSampler = RenderSystem.getDevice().createSampler(AddressMode.CLAMP_TO_EDGE, AddressMode.CLAMP_TO_EDGE, FilterMode.NEAREST, FilterMode.NEAREST, maxAnisotropy, OptionalDouble.empty());
        }

        if (WikiRenderer.orthographicSorting != null) {
            this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        }

        float currentRotation = AreaPropertyBundle.INSTANCE.getUsedRotation();
        double currentSlant = AreaPropertyBundle.INSTANCE.getUsedSlant();
        if ((this.lastUsedRotation != currentRotation || this.lastUsedSlant != currentSlant) && this.orthographicTransparencySorting != null) {
            boolean isProbablyLargeSpinningObjectShrunkenDown = renderable.getProperties().scale.get() <= 15 && renderable.getProperties().rotationSpeed.get() > 0;
            if (!isProbablyLargeSpinningObjectShrunkenDown) {
                // anything smaller than 15 you probably wont see transparency issues (i.e. rendering the skyblock hub)
                this.lastUsedRotation = currentRotation;
                this.lastUsedSlant = currentSlant;
                scheduleReSort();
            }
        }

        ChunkSectionsToRender sections = prepareBlockLayers(matrices.last().pose());

        for (ChunkSectionLayerGroup sectionLayer : new ChunkSectionLayerGroup[]{ChunkSectionLayerGroup.OPAQUE, ChunkSectionLayerGroup.TRANSLUCENT}) {
            if (sectionLayer == ChunkSectionLayerGroup.TRANSLUCENT) {
                preTranslucencyTask.run();
            }
            overrideTerrainTransparencyRenderPipelines = sectionLayer == ChunkSectionLayerGroup.OPAQUE;
            sections.renderGroup(sectionLayer, terrainSampler);
        }
    }

    // Based on LevelRenderer#prepareChunkRenders
    private ChunkSectionsToRender prepareBlockLayers(Matrix4fc posMatrix) {
        EnumMap<ChunkSectionLayer, Int2ObjectOpenHashMap<List<RenderPass.Draw<GpuBufferSlice[]>>>> drawGroups = new EnumMap<>(ChunkSectionLayer.class);

        for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
            drawGroups.put(layer, new Int2ObjectOpenHashMap<>());
        }

        List<MeshRenderSection> sortedSections = new ArrayList<>(subMeshes.values());

        if (orthographicTransparencySorting instanceof OrthographicSort orthoSort) {
                sortedSections.sort(Comparator.comparingDouble(s -> {
                    BlockPos from = s.getFrom();
                    BlockPos to = s.getTo();
                    float cx = (from.getX() + to.getX()) / 2f;
                    float cy = (from.getY() + to.getY()) / 2f;
                    float cz = (from.getZ() + to.getZ()) / 2f;
                    return orthoSort.projectDepth(cx, cy, cz);
                }));
        }

        List<DynamicUniforms.ChunkSectionInfo> sectionInfos = new ArrayList<>();
        GpuTextureView gpuTextureView = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getTextureView();
        int width = gpuTextureView.getWidth(0);
        int height = gpuTextureView.getHeight(0);
        int largestIndexCount = 0;

        if (sectionRenderDispatcher != null) {
            sectionRenderDispatcher.lock();
            try {
                try (Zone ignored = Profiler.get().zone("Upload WikiRenderer Mesh Global Buffers")) {
                    sectionRenderDispatcher.uploadGlobalGeomBuffersToGPU();
                }

                for (MeshRenderSection section : sortedSections) {
                    SectionMesh sectionMesh = section.getSectionMesh();
                    int uboIndex = -1;

                    for (ChunkSectionLayer layer : ChunkSectionLayer.values()) {
                        SectionMesh.SectionDraw draw = sectionMesh.getSectionDraw(layer);
                        SectionRenderDispatcher.RenderSectionBufferSlice slice = sectionRenderDispatcher.getRenderSectionSlice(sectionMesh, layer);
                        if (slice != null && draw != null && (!draw.hasCustomIndexBuffer() || slice.indexBuffer() != null)) {
                            if (uboIndex == -1) {
                                uboIndex = sectionInfos.size();
                                sectionInfos.add(new DynamicUniforms.ChunkSectionInfo(new Matrix4f(posMatrix), 0, 0, 0, 1.0F, width, height));
                            }

                            int combinedHash = 173;
                            VertexFormat vertexFormat = layer.pipeline().getVertexFormat();
                            GpuBuffer vertexBuffer = slice.vertexBuffer();
                            if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                combinedHash = 31 * combinedHash + vertexBuffer.hashCode();
                            }

                            int firstIndex = 0;
                            GpuBuffer indexBuffer;
                            VertexFormat.IndexType indexType;
                            if (!draw.hasCustomIndexBuffer()) {
                                if (draw.indexCount() > largestIndexCount) {
                                    largestIndexCount = draw.indexCount();
                                }

                                indexBuffer = null;
                                indexType = null;
                            } else {
                                indexBuffer = slice.indexBuffer();
                                indexType = draw.indexType();
                                if (layer != ChunkSectionLayer.TRANSLUCENT) {
                                    combinedHash = 31 * combinedHash + indexBuffer.hashCode();
                                    combinedHash = 31 * combinedHash + indexType.hashCode();
                                }

                                firstIndex = (int) (slice.indexBufferOffset() / indexType.bytes);
                            }

                            int sectionIndex = uboIndex;
                            int baseVertex = (int) (slice.vertexBufferOffset() / vertexFormat.getVertexSize());
                            List<RenderPass.Draw<GpuBufferSlice[]>> draws = drawGroups.get(layer)
                                    .computeIfAbsent(combinedHash, (_ -> new ArrayList<>()));

                            draws.add(new RenderPass.Draw<>(
                                    0,
                                    vertexBuffer,
                                    indexBuffer,
                                    indexType,
                                    firstIndex,
                                    draw.indexCount(),
                                    baseVertex,
                                    (transforms, uniformUploader) -> uniformUploader.upload("ChunkSection", transforms[sectionIndex])
                            ));
                        }
                    }
                }
            } finally {
                sectionRenderDispatcher.unlock();
            }
        }

        GpuBufferSlice[] gpuBufferSlices = RenderSystem.getDynamicUniforms()
                .writeChunkSections(sectionInfos.toArray(new DynamicUniforms.ChunkSectionInfo[0]));
        return new ChunkSectionsToRender(gpuTextureView, drawGroups, largestIndexCount, gpuBufferSlices);
    }

    public void drawBlockEntities(PoseStack standardStack, SubmitNodeStorage nodeStorage, CameraRenderState cameraRenderState, float tickDelta) {
        BlockPos minCorner = bounds.getMinCorner();
        standardStack.pushPose();
        standardStack.translate(-minCorner.getX(), -minCorner.getY(), -minCorner.getZ());

        BlockEntityRenderDispatcher blockEntityDispatcher = Minecraft.getInstance().getBlockEntityRenderDispatcher();
        for (MeshRenderSection renderSection : this.subMeshes.values()) {
            renderSection.blockEntities.forEach((blockPos, entity) -> {
                BlockEntityRenderState state = blockEntityDispatcher.tryExtractRenderState(entity, tickDelta, null);
                if (state instanceof BeaconRenderState && AreaPropertyBundle.INSTANCE.hideBeaconBeams.get()) {
                    return;
                }

                standardStack.pushPose();
                standardStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());

                if (state != null) {
                    blockEntityDispatcher.submit(state, standardStack, nodeStorage, cameraRenderState);
                }

                standardStack.popPose();
            });
        }

        standardStack.popPose();
        renderable.drawSubmittedRenderFeatures();
    }

    public synchronized void scheduleRebuild(boolean async) {
        if (currentlyFullyBuilding) return;

        this.fullBuildProgress = 0;
        this.state = this.state != MeshState.NEW
                ? MeshState.REBUILDING
                : MeshState.BUILDING;

        this.subMeshes.values().forEach(MeshRenderSection::reset);
        this.subMeshes.clear();

        this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;

        this.currentlyFullyBuilding = true;
        if (ShaderCheck.isUsingShaders() || !async) {
            this.compileMesh();
        } else {
            CompletableFuture.runAsync(this::compileMesh);
        }
    }

    public synchronized void stopBuilding() {
        this.buildingCancelled = true;
        this.state = MeshState.CANCELLED;
    }

    public void setDirty(int sectionX, int sectionY, int sectionZ, boolean force) {
        long sectionIndex = MeshRenderSection.getSectionIndex(sectionX, sectionY, sectionZ);
        MeshRenderSection renderSection = this.subMeshes.get(sectionIndex);

        if (renderSection == null && force) {
            renderSection = createRenderSection(sectionX, sectionY, sectionZ);
            this.subMeshes.put(sectionIndex, renderSection);
        }

        if (renderSection != null) {
            renderSection.setDirty(force);
        }
    }

    private void updateBuildingStatus() {
        if (currentlyFullyBuilding) {
            int amountBuilt = 0;
            for (MeshRenderSection sections : this.subMeshes.values()) {
                if (!sections.isBuilding) amountBuilt++;
            }
            this.fullBuildProgress = (float) amountBuilt / this.subMeshes.size();

            if (amountBuilt == this.subMeshes.size()) {
                currentlyFullyBuilding = false;
                buildingCancelled = false;
                this.state = MeshState.READY;
            }
        }
    }

    private void updateOutdatedMeshSections() {
        if (isAutomaticUpdateScheduled || currentlyFullyBuilding) return;

        boolean allowAutoUpdate = !renderable.getProperties().freezeBlocks.get();
        boolean hasDirty = subMeshes.values().stream().anyMatch(section -> section.isDirty() && (allowAutoUpdate || section.isForceUpdate()));
        if (!hasDirty) return;

        isAutomaticUpdateScheduled = true;
        if (ShaderCheck.isUsingShaders()) {
            this.compileOutdatedMeshSections();
            isAutomaticUpdateScheduled = false;
        } else {
            CompletableFuture.runAsync(() -> {
                try {
                    this.compileOutdatedMeshSections();
                } finally {
                    isAutomaticUpdateScheduled = false;
                }
            });
        }
    }

    private void compileOutdatedMeshSections() {
        if (this.currentlyFullyBuilding) return;
        boolean allowAutoUpdate = !renderable.getProperties().freezeBlocks.get();

        for (MeshRenderSection section : this.subMeshes.values()) {
            if (section.isDirty() && (allowAutoUpdate || section.isForceUpdate())) {
                section.buildAndSubmit(this);
            }
        }
    }

    // Based on SectionCompiler#compile and then SectionRenderDispatcher.RenderSection.RebuildTask#doTask (which calls compile)
    private void compileMesh() {
        if (buildingCancelled) {
            currentlyFullyBuilding = false;
            return;
        }
        currentlyFullyBuilding = true;

        List<MeshRenderSection> unsortedSubMeshes = new ArrayList<>();

        BlockPos minCorner = this.bounds.getMinCorner();
        BlockPos maxCorner = this.bounds.getMaxCorner();

        int minSectionX = MeshRenderSection.getSection(minCorner.getX());
        int minSectionY = MeshRenderSection.getSection(minCorner.getY());
        int minSectionZ = MeshRenderSection.getSection(minCorner.getZ());
        int maxSectionX = MeshRenderSection.getSection(maxCorner.getX());
        int maxSectionY = MeshRenderSection.getSection(maxCorner.getY());
        int maxSectionZ = MeshRenderSection.getSection(maxCorner.getZ());

        for (int sectionX = minSectionX; sectionX <= maxSectionX; sectionX += 1) {
            for (int sectionY = minSectionY; sectionY <= maxSectionY; sectionY += 1) {
                for (int sectionZ = minSectionZ; sectionZ <= maxSectionZ; sectionZ += 1) {
                    long sectionIndex = MeshRenderSection.getSectionIndex(sectionX, sectionY, sectionZ);
                    int finalSectionX = sectionX;
                    int finalSectionY = sectionY;
                    int finalSectionZ = sectionZ;

                    MeshRenderSection section = this.subMeshes.computeIfAbsent(sectionIndex, _ -> createRenderSection(finalSectionX, finalSectionY, finalSectionZ));
                    unsortedSubMeshes.add(section);
                }
            }
        }

        List<MeshRenderSection> subMeshes = unsortedSubMeshes.stream().sorted(Comparator.comparing(MeshRenderSection::getDistanceFromCenter)).toList();

        if (buildingCancelled) {
            currentlyFullyBuilding = false;
            return;
        }

        this.world.setWalkabilityFilter(null);
        WalkabilityFilter walkabilityFilter = null;
        AreaPropertyBundle properties = AreaPropertyBundle.INSTANCE;
        if (properties.perPixel90DegreeRendering.get()) {
            if (properties.useWalkabilityFilter.get()) {
                walkabilityFilter = new WalkabilityFilter(this, renderable);
                walkabilityFilter.cacheData();
            }
        }
        this.world.setWalkabilityFilter(walkabilityFilter);

        BlockModelLighter.enableCaching();

        for (MeshRenderSection renderSection : subMeshes) {
            renderSection.buildAndSubmit(this);
        }

        BlockModelLighter.clearCache();
    }

    private MeshRenderSection createRenderSection(int sectionX, int sectionY, int sectionZ) {
        BlockPos minCorner = this.bounds.getMinCorner();
        BlockPos maxCorner = this.bounds.getMaxCorner();
        int middleX = maxCorner.getX() - (maxCorner.getX() - minCorner.getX()) / 2;
        int middleZ = maxCorner.getZ() - (maxCorner.getZ() - minCorner.getZ()) / 2;

        MeshRenderSection section = new MeshRenderSection(this.sectionRenderDispatcher, sectionX, sectionY, sectionZ);

        BlockPos from = section.getFrom();
        BlockPos to = section.getTo();

        int subMiddleX = to.getX() - (to.getX() - from.getX()) / 2;
        int subMiddleZ = to.getZ() - (to.getZ() - from.getZ()) / 2;
        double distance = Math.pow(middleX - subMiddleX, 2) + Math.pow(middleZ - subMiddleZ, 2);

        section.setDistanceFromCenter(distance);

        return section;
    }

    private synchronized void scheduleReSort() {
        if (this.sortFuture != null && !this.sortFuture.isDone()) return;

        this.orthographicTransparencySorting = WikiRenderer.orthographicSorting;
        if (ShaderCheck.isUsingShaders()) {
            this.sortFuture = CompletableFuture.completedFuture(null);
            this.reSortMeshSections();
            this.sortFuture = null;
        } else {
            this.sortFuture = CompletableFuture.runAsync(this::reSortMeshSections).whenComplete((_, _) -> this.sortFuture = null);
        }
    }

    // Based on SectionRenderDispatcher.RenderSection.ResortTransparencyTask#doTask
    private void reSortMeshSections() {
        for (MeshRenderSection section : subMeshes.values()) {
            section.reSortTransparencyAndSubmit(this);
        }
    }

    protected BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> startedLayers, SectionBufferBuilderPack buffers, ChunkSectionLayer layer) {
        return startedLayers.computeIfAbsent(layer, _ -> new BufferBuilder(buffers.buffer(layer), VertexFormat.Mode.QUADS, layer.vertexFormat()));
    }

    public Optional<List<Integer>> getAnimationCompletionTimings() {
        List<Integer> timings = new ArrayList<>();
        for (MeshRenderSection section : subMeshes.values()) {
            timings.addAll(section.animationCompletionTimings);
        }
        return timings.isEmpty() ? Optional.empty() : Optional.of(timings);
    }

    public void dispose() {
        subMeshes.values().forEach(MeshRenderSection::reset);
        subMeshes.clear();
        resortBufferPack.close();
    }

    public enum MeshState {
        NEW(false, false),
        CANCELLED(true, true),
        BUILDING(true, true),
        REBUILDING(true, true),
        READY(false, true),
        CORRUPT(false, false);

        public final boolean isBuildStage;
        public final boolean canRender;

        MeshState(boolean buildStage, boolean canRender) {
            this.isBuildStage = buildStage;
            this.canRender = canRender;
        }
    }
}
