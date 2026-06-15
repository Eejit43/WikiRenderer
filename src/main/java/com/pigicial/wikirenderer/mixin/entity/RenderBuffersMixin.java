package com.pigicial.wikirenderer.mixin.entity;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.entity.EntityRenderBoundsUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// fixes https://github.com/skyblock-wiki/WikiRenderer/issues/22, for some reason entity model features / entity texture features
// will render the virtual entity (for bound data) even if I use the buffer sources listed below in other places, they also have to be defined here
@Mixin(RenderBuffers.class)
public class RenderBuffersMixin {

    @Inject(method = "bufferSource", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$redirectBufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
        if (WikiRenderer.inBoundsCalculation) {
            cir.setReturnValue(EntityRenderBoundsUtil.BUFFER_SOURCE);
        }
    }

    @Inject(method = "crumblingBufferSource", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$redirectCrumblingBufferSource(CallbackInfoReturnable<MultiBufferSource.BufferSource> cir) {
        if (WikiRenderer.inBoundsCalculation) {
            cir.setReturnValue(EntityRenderBoundsUtil.BUFFER_SOURCE);
        }
    }

    @Inject(method = "outlineBufferSource", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$redirectOutlineBufferSource(CallbackInfoReturnable<OutlineBufferSource> cir) {
        if (WikiRenderer.inBoundsCalculation) {
            cir.setReturnValue(EntityRenderBoundsUtil.OUTLINE_BUFFER_SOURCE);
        }
    }
}
