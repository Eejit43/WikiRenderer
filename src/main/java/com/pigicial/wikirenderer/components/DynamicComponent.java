package com.pigicial.wikirenderer.components;

import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.OwoUIGraphics;
import io.wispforest.owo.ui.core.Size;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;

import java.util.function.Supplier;

public class DynamicComponent extends FlowLayout {
    private final UIComponent component;
    private final Supplier<Boolean> displayCondition;
    private boolean exists = true;

    public DynamicComponent(UIComponent component, Supplier<Boolean> displayCondition) {
        super(Sizing.content(), Sizing.content(), Algorithm.VERTICAL);
        this.component = component;
        this.displayCondition = displayCondition;

        this.child(component);
    }

    @Override
    public void inflate(Size space) {
        if (!exists) {
            // not doing this can cause the component to sometimes not appear
            this.space = space;
            component.inflate(this.calculateChildSpace(space));
        } else {
            super.inflate(space);
        }
    }

    @Override
    public Size fullSize() {
        if (!exists) {
            int gap = 0;
            if (this.parent() instanceof FlowLayout flow) {
                gap = flow.gap();
            }
            return Size.of(-gap, -gap);
        }
        return super.fullSize();
    }

    @Override
    protected int determineVerticalContentSize(Sizing sizing) {
        return exists ? component.fullSize().height() : 0;
    }

    @Override
    protected int determineHorizontalContentSize(Sizing sizing) {
        return exists ? component.fullSize().width() : 0;
    }

    private void update() {
        boolean shouldShow = displayCondition.get();
        if (exists != shouldShow) {
            exists = shouldShow;
            if (this.parent() != null) {
                this.parent().onChildMutated(this);
            }
        }
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        update();
        super.parentUpdate(delta, mouseX, mouseY);
    }

    @Override
    public void draw(OwoUIGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        if (!exists) return;
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
    }
}