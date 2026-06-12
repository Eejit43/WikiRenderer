package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.util.UISounds;
import net.minecraft.client.input.MouseButtonEvent;

public class MiniEditBoxComponent extends TextBoxComponent {
    public MiniEditBoxComponent(Sizing horizontalSizing, String text) {
        super(horizontalSizing);
        this.setMaxLength(Integer.MAX_VALUE);
        this.text(text);
        this.verticalSizing(Sizing.fixed(14));
        this.horizontalSizing(horizontalSizing);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent mouseButtonEvent, boolean bl) {
        if (super.mouseClicked(mouseButtonEvent, bl)) {
            UISounds.playButtonSound();
            return true;
        }
        return false;
    }
}
