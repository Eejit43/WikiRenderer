package com.pigicial.wikirenderer.mixin.world.particle;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.particle.ParticleDisplayCondition;
import com.pigicial.wikirenderer.render.particle.ParticleRendererAndLooper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.QuadParticleGroup;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(QuadParticleGroup.class)
public class QuadParticleGroupMixin {

    // separated from vanilla to avoid conflicts
    @Unique
    private final QuadParticleRenderState renderScreenRenderState = new QuadParticleRenderState();

    @Redirect(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/SingleQuadParticle;extract(Lnet/minecraft/client/renderer/state/level/QuadParticleRenderState;Lnet/minecraft/client/Camera;F)V"
            )
    )
    private void wikirenderer$saveQuadParticleData(SingleQuadParticle instance, QuadParticleRenderState particleTypeRenderState, Camera camera, float partialTickTime) {

        if (ParticleRendererAndLooper.renderingParticles) {
            ParticleDisplayCondition restriction = WikiRenderer.particleDisplayCondition;
            if (!restriction.test(instance)) {
                return;
            }

            instance.extract(this.renderScreenRenderState, camera, partialTickTime);

            if (ParticleRendererAndLooper.renderingAndSavingParticlesForLooping) {
                QuadParticleRenderState savedRenderState = new QuadParticleRenderState();

                instance.extract(savedRenderState, camera, 0);
                ParticleRendererAndLooper.saveParticleData(instance, savedRenderState);
            }
        } else {
            instance.extract(particleTypeRenderState, camera, partialTickTime);
        }
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"), cancellable = true)
    public void wikirenderer$setCustomRenderState(CallbackInfoReturnable<ParticleGroupRenderState> cir) {
        if (ParticleRendererAndLooper.renderingParticles) {
            cir.setReturnValue(this.renderScreenRenderState);
        }
    }
}
