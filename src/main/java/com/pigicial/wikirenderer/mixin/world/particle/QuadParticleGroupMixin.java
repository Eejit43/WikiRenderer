package com.pigicial.wikirenderer.mixin.world.particle;

import com.pigicial.wikirenderer.render.particle.ParticleRendererAndLooper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"
            )
    )
    private void onAfterExtract(SingleQuadParticle instance, QuadParticleRenderState particleTypeRenderState, Camera camera, float f) {

        if (ParticleRendererAndLooper.saving) {
            QuadParticleRenderState savedRenderState = new QuadParticleRenderState();

            instance.extract(savedRenderState, camera, 0);
            ParticleRendererAndLooper.saveParticleData(instance, savedRenderState);

            instance.extract(particleTypeRenderState, camera, 0);
        } else {

            instance.extract(particleTypeRenderState, camera, f);
        }
    }
}
