package com.pigicial.wikirenderer.mixin.texture;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.render.screen.ContainerScreenPropertyBundle;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsExtractorMixin {

    @Redirect(
            method = "tooltip",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/inventory/tooltip/TooltipRenderUtil;extractTooltipBackground(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIIILnet/minecraft/resources/Identifier;)V"
            )
    )
    private void conditionalBackground(GuiGraphicsExtractor graphics, int x, int y, int w, int h, Identifier style) {
        if (!WikiRenderer.skipTooltipBackgroundRender) {
            TooltipRenderUtil.extractTooltipBackground(graphics, x, y, w, h, style);
        }
    }

    @Inject(method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$skipText(Font font, FormattedCharSequence formattedCharSequence, int i, int j, int k, boolean bl, CallbackInfo ci) {
        if (WikiRenderer.inContainerScreenDraw && ContainerScreenPropertyBundle.INSTANCE.hideText.get()) {
            ci.cancel();
        }
    }

    @Inject(method = "item(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;III)V", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$skipItem(LivingEntity livingEntity, Level level, ItemStack itemStack, int i, int j, int k, CallbackInfo ci) {
        if (WikiRenderer.inContainerScreenDraw && ContainerScreenPropertyBundle.INSTANCE.hideItems.get()) {
            ci.cancel();
        }
    }

    // see https://github.com/skyblock-wiki/WikiRenderer/issues/20
    @Inject(method = "componentHoverEffect(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Style;II)V", at = @At("HEAD"), cancellable = true)
    private void wikirenderer$onComponentHoverEffect(Font font, Style hoveredStyle, int xMouse, int yMouse, CallbackInfo ci) {
        if (hoveredStyle == null) {
            ci.cancel();
        }
    }
}
