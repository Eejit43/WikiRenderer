package com.pigicial.wikirenderer.mixin.entity;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.texture.AbstractTexture;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderSetup.class)
public abstract class RenderSetupMixin {

    @Final
    @Shadow
    RenderPipeline pipeline;

    // Fixes player skins sometimes having an extra line on the top (only appears at certain scales/rotations/positions)
    @Redirect(
            method = "getTextures",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/AbstractTexture;getSampler()Lcom/mojang/blaze3d/textures/GpuSampler;")
    )
    private GpuSampler wikirenderer$overrideSampler(AbstractTexture instance) {
        GpuSampler original = instance.getSampler();
        if (WikiRenderer.inEntityDraw && original == RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST) && pipeline != RenderPipelines.ENERGY_SWIRL) {
            return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
        }

        return original;
    }
}
