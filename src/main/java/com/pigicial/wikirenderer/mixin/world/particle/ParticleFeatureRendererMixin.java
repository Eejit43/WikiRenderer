package com.pigicial.wikirenderer.mixin.world.particle;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.feature.ParticleFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ParticleFeatureRenderer.class, priority = 2000)
public class ParticleFeatureRendererMixin {

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getMainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget overrideMainTarget(Minecraft instance, Operation<RenderTarget> original) {
        if (WikiRenderer.mainTargetOverride != null) {
            return WikiRenderer.mainTargetOverride;
        }
        return original.call(instance);
    }

    @WrapOperation(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getParticlesTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;")
    )
    private RenderTarget overrideParticlesTarget(LevelRenderer instance, Operation<RenderTarget> original) {
        if (WikiRenderer.mainTargetOverride != null) {
            return WikiRenderer.mainTargetOverride;
        }
        return original.call(instance);
    }
}
