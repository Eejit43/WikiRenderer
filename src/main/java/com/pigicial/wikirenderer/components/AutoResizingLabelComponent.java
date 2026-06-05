package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.core.Size;
import net.minecraft.network.chat.Component;

public class AutoResizingLabelComponent extends LabelComponent {
    private final int horizontalSubtraction;

    public AutoResizingLabelComponent(Component text, int horizontalSubtraction) {
        super(text);
        this.horizontalSubtraction = horizontalSubtraction;
    }

    public AutoResizingLabelComponent(Component text) {
        this(text, 5);
    }

    @Override
    public void inflate(Size space) {
        this.maxWidth(Math.max(space.width() - horizontalSubtraction, 20));
        super.inflate(space);
    }
}
