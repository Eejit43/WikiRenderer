package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.authlib.properties.PropertyMap;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.render.item.ItemRenderablePropertyBundle;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.textures.PlayerTextureUtils;
import com.pigicial.wikirenderer.textures.TextureData;
import com.pigicial.wikirenderer.util.NullSafeUUIDTypeAdapter;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.UIComponents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class ItemFrameBasedRenderable extends FrameBasedRenderable<TextureData, ItemRenderable, ItemRenderablePropertyBundle> {
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(UUID.class, new NullSafeUUIDTypeAdapter())
            .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
            .create();

    public ItemFrameBasedRenderable(UUID entityID, Supplier<TreeMap<Integer, TextureData>> dataSourceSupplier) {
        super(entityID, dataSourceSupplier, ItemRenderablePropertyBundle.INSTANCE);
    }

    @Override
    protected ItemRenderable createRenderableFromData(TextureData data) {
        ItemStack item = PlayerTextureUtils.createPlayerHead(data.profile());
        return new ItemRenderable(item);
    }

    @Override
    public ItemComponent createItemComponentForPreview(FrameData<TextureData, ItemRenderable, ItemRenderablePropertyBundle> frameData) {
        return UIComponents.item(frameData.renderable().stack);
    }

    @Override
    protected boolean sourceDataMatches(TextureData data1, TextureData data2) {
        return Objects.equals(data1.profile(), data2.profile());
    }

    @Override
    protected TextureData getMatchingFirstMarkedData() {
        return SkyBlockTimingDataCacher.INSTANCE.getMarkedFirstTexture();
    }

    @Override
    @NotNull
    public InterpolatedTimings getTimings(List<FrameData<TextureData, ItemRenderable, ItemRenderablePropertyBundle>> currentDataSet, int framesCount) {
        return SkyBlockTimingDataCacher.INSTANCE.getTextureTimings(currentDataSet, framesCount);
    }

    @Override
    protected List<String> generateWikiTextFile(List<FrameData<TextureData, ItemRenderable, ItemRenderablePropertyBundle>> currentDataSet) {
        List<String> hashes = new ArrayList<>();
        for (FrameData<TextureData, ItemRenderable, ItemRenderablePropertyBundle> frame : currentDataSet) {
            String hash = frame.sourceData().payload().textures().get(MinecraftProfileTexture.Type.SKIN).getHash();
            hashes.add(hash);
        }
        return List.of(
                "{{HeadRender|",
                String.join(";\n", hashes),
                "}}"
        );
    }

    // ic request
    protected List<String> generateProfileJson() {
        List<String> lines = new ArrayList<>();
        for (FrameData<TextureData, ItemRenderable, ItemRenderablePropertyBundle> frame : currentDataSet) {
            GameProfile profile = frame.sourceData().profile();
            lines.add(" " + GSON.toJson(profile));
        }
        return List.of(
                "[",
                String.join(",\n", lines),
                "]"
        );
    }

    @Override
    protected void saveFileData(RenderScreen screen) {
        super.saveFileData(screen);

        if (FrameBasedPropertyBundle.ITEM_EXPORT_PROFILE_DATA.get()) {
            ExportPathSpec defaultExportPath = this.getExportPath();
            ExportPathSpec exportPath = defaultExportPath.differentFileName(this.getCustomFileName());
            String profileJsonFile = String.join("\n", this.generateProfileJson());

            ExportPathSpec profileDataExportPath = exportPath.filename().isBlank()
                    ? defaultExportPath.differentFileName("Base64 Data")
                    : defaultExportPath.differentFileName(exportPath.filename() + " Base64 Data");

            FileIO.saveTextAndNotify(profileJsonFile, profileDataExportPath, screen, "exported_profile_json_data_as");
        }
    }
}
