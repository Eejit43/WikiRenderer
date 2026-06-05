package com.pigicial.wikirenderer.components;

import com.pigicial.wikirenderer.property.Property;
import io.wispforest.owo.ui.component.CheckboxComponent;
import io.wispforest.owo.ui.core.Size;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;

public class PropertyCheckboxComponent extends CheckboxComponent {

    private final Property<Boolean> property;

    public PropertyCheckboxComponent(Component message, Property<Boolean> property) {
        super(message);

        this.property = property;
        this.checked(this.property.get());
    }

    @Override
    public void onPress(InputWithModifiers input) {
        super.onPress(input);
        property.set(this.selected());
    }

    @Override
    public void inflate(Size space) {
        this.setWidth(Math.max(space.width() - 29, 20));
        super.inflate(space);
    }
}
