package com.pigicial.wikirenderer.mixin.world.particle;

import com.pigicial.wikirenderer.render.particle.ParticleRendererAndLooper;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ElderGuardianParticle;
import net.minecraft.client.particle.ElderGuardianParticleGroup;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ElderGuardianParticleGroup.class)
public class ElderGuardianParticleGroupMixin {

    // is this even needed? since elder guardian particles are the first-person ones, but oh well i already wrote this and probably no harm since it's an inject and not a replacement
    @Inject(
            method = "extractRenderState(Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)Lnet/minecraft/client/renderer/state/level/ParticleGroupRenderState;",
            at = @At("HEAD")
    )
    private void captureParticlesAndMap(Frustum frustum, Camera camera, float partialTickTime, CallbackInfoReturnable<ParticleGroupRenderState> cir) {
        if (ParticleRendererAndLooper.renderingAndSavingParticlesForLooping) {
            for (ElderGuardianParticle particle : ((ElderGuardianParticleGroup) (Object) this).getAll()) {
                ElderGuardianParticleGroup.ElderGuardianParticleRenderState renderState = ElderGuardianParticleGroup.ElderGuardianParticleRenderState.fromParticle(particle, camera, partialTickTime);
                ParticleRendererAndLooper.saveParticleData(particle, new ElderGuardianParticleGroup.State(List.of(renderState)));
            }
        }
    }
}
