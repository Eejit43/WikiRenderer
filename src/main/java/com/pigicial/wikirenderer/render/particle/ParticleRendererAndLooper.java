package com.pigicial.wikirenderer.render.particle;

import com.mojang.blaze3d.systems.RenderSystem;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.CameraInvoker;
import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.CameraOrientationUtil;
import com.pigicial.wikirenderer.render.Renderable;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

import java.util.ArrayList;
import java.util.HashMap;

public class ParticleRendererAndLooper {
    public static final Frustum ALWAYS_TRUE_PARTICLE_FRUSTUM = new Frustum(new Matrix4f(), new Matrix4f()) {
        @Override
        public boolean pointInFrustum(double d, double e, double f) {
            return true;
        }
    };

    private static final HashMap<Particle, SavedParticleData> SAVED_PARTICLES = new HashMap<>();
    public static boolean saving = false;
    public static boolean looping = false;

    public static void drawParticles(Renderable<? extends DefaultPropertyBundle> renderable, Matrix4f transform, float tickDelta) {
        if (!GlobalProperties.get().tickParticles.get()) {
            return;
        }

        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.mul(transform);

        Minecraft client = Minecraft.getInstance();
        // present in vanilla

        Camera camera = Minecraft.getInstance().getEntityRenderDispatcher().camera;
        if (camera == null) return;

        float previousYaw = camera.yRot();
        float previousPitch = camera.xRot();

        ((CameraInvoker) camera).wikirenderer$setRotation(renderable.getProperties().getUsedRotation() + 180, (float) renderable.getProperties().getUsedSlant());
        ParticlesRenderState particleBatch = new ParticlesRenderState();

        looping = WikiRenderer.currentAnimationHandler != null && canLoopParticles();
        saving = true;
        // todo make this work for non-quad-particles

        client.particleEngine.extract(
                particleBatch,
                ALWAYS_TRUE_PARTICLE_FRUSTUM,
                camera,
                looping ? 0 : tickDelta // 0 for looping to ensure tickDelta consistency
        );

        /* create render state from camera object; (mostly) mirrors GameRenderer.updateCameraState */
        CameraRenderState cameraRenderState = CameraOrientationUtil.createRenderState(renderable);
        cameraRenderState.initialized = true;
        cameraRenderState.pos = camera.position();
        cameraRenderState.blockPos = camera.blockPosition();
        cameraRenderState.pos = camera.entity().getPosition(tickDelta);

        /* submit and render to vertexconsumers */
        SubmitNodeStorage submitNodeStorage = client.gameRenderer.getSubmitNodeStorage();
        particleBatch.submit(submitNodeStorage, cameraRenderState);

        handleCompletedParticles();
        if (looping) {

            int animationLifespan = WikiRenderer.currentAnimationHandler.getAnimationFrames(); // guaranteed to be 20 per second
            int animationCurrentFrame = animationLifespan - WikiRenderer.currentAnimationHandler.getRemainingFrames();

            for (SavedParticleData savedParticle : SAVED_PARTICLES.values()) {
                if (!savedParticle.getParticle().isAlive() && savedParticle.getAnimationTimeFinishedAt() != null) {
                    int particleAnimationTimeFinishedAt = savedParticle.getAnimationTimeFinishedAt();
                    int particleLifespan = savedParticle.getParticle().getLifetime();

                    int loopStartTime = animationLifespan - particleLifespan + particleAnimationTimeFinishedAt;
                    if (animationCurrentFrame >= loopStartTime) {
                        int loopIndex = animationCurrentFrame - loopStartTime;
                        ParticleGroupRenderState renderDataAtTick = savedParticle.getRenderDataAtTick(loopIndex);
                        if (renderDataAtTick != null) {
                            renderDataAtTick.submit(submitNodeStorage, cameraRenderState);
                        }
                    }
                }
            }
        }

        renderable.drawSubmittedRenderFeatures();
        particleBatch.reset();

        looping = false;
        saving = false;


        ((CameraInvoker) camera).wikirenderer$setRotation(previousYaw, previousPitch);

        modelView.popMatrix();
    }

    public static boolean canLoopParticles() {
        return GlobalProperties.get().loopParticles.get() && GlobalProperties.get().exportFramerate.get() == 20 && GlobalProperties.get().setAnimationFpsCap.get();
    }

    public static void saveParticleData(Particle particle, ParticleGroupRenderState renderState) {
        if (!particle.isAlive()) return;

        SavedParticleData savedParticleData = SAVED_PARTICLES.get(particle);
        if (savedParticleData == null && looping) {
            // don't bother saving if it's mid-loop already
            return;
        }

        if (savedParticleData == null) savedParticleData = new SavedParticleData(particle);
        SAVED_PARTICLES.put(particle, savedParticleData);

        savedParticleData.saveRenderData(renderState);
    }

    private static void handleCompletedParticles() {
        if (WikiRenderer.currentAnimationHandler == null) {
            for (Particle particle : new ArrayList<>(SAVED_PARTICLES.keySet())) {
                if (!particle.isAlive()) {
                    SAVED_PARTICLES.remove(particle);
                }
            }
            return;
        }

        for (SavedParticleData savedParticle : SAVED_PARTICLES.values()) {
            Particle particle = savedParticle.getParticle();
            if (!particle.isAlive() && savedParticle.getAnimationTimeFinishedAt() == null) {
                int animationLifespan = WikiRenderer.currentAnimationHandler.getAnimationFrames(); // guaranteed to be 20 per second
                int frameIndex = animationLifespan - WikiRenderer.currentAnimationHandler.getRemainingFrames();
                savedParticle.setAnimationTimeFinishedAt(frameIndex);
            }
        }
    }
}
