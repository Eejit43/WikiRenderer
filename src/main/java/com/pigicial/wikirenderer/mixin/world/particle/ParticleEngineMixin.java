package com.pigicial.wikirenderer.mixin.world.particle;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.particle.ParticleDisplayCondition;
import com.pigicial.wikirenderer.render.particle.ParticleRendererAndLooper;
import com.pigicial.wikirenderer.screen.RenderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public class ParticleEngineMixin {

    @Inject(method = "add(Lnet/minecraft/client/particle/Particle;)V", at = @At("HEAD"), cancellable = true)
    public void stopParticles(Particle particle, CallbackInfo ci) {
        if (!(Minecraft.getInstance().screen instanceof RenderScreen)) return;
        if (!GlobalProperties.get().tickParticles.get()) {
            ci.cancel();
            return;
        }

        ParticleDisplayCondition restriction = WikiRenderer.particleDisplayCondition;
        if (!restriction.test(particle)) {
            ci.cancel();
        }

        if (WikiRenderer.currentAnimationHandler != null && ParticleRendererAndLooper.canLoopParticles()) {
            int animationLifespan = WikiRenderer.currentAnimationHandler.getAnimationFrames();
            int animationFrameIndex = animationLifespan - WikiRenderer.currentAnimationHandler.getRemainingFrames();
            int particleLifespan = particle.getLifetime();

            boolean loopable = particleLifespan <= animationLifespan && (animationFrameIndex + particleLifespan) <= animationLifespan;

            if (!loopable) {
                ci.cancel();
            }
        }
    }
}
