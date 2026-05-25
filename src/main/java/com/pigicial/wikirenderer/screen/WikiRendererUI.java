package com.pigicial.wikirenderer.screen;

import com.pigicial.wikirenderer.components.*;
import com.pigicial.wikirenderer.property.DoubleProperty;
import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.component.TextBoxComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.UIContainers;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.UIComponent;
import io.wispforest.owo.ui.core.VerticalAlignment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class WikiRendererUI {

    public static TextBoxComponent labelledTextField(FlowLayout container, String content, String key, Sizing sizing) {
        try (RowBuilder builder = rowBuilder(container)) {
            TextBoxComponent textBox = UIComponents.textBox(sizing, content);
            textBox.setMaxLength(100); // allow more characters
            textBox.text(content); // fixes if it truncates early
            builder.row.child(textBox);
            builder.row.child(UIComponents.label(Translate.gui(key)).margins(Insets.left(8)));
            return textBox;
        }
    }

    public static void labelledTextField(RenderScreen screen, FlowLayout container, IntProperty property, String key, Sizing sizing) {
        try (RowBuilder builder = rowBuilder(container)) {
            TextBoxComponent textBox = new IntegerPropertyTextFieldComponent(screen, sizing, property, false);

            builder.row.child(textBox);
            builder.row.child(UIComponents.label(Translate.gui(key)).margins(Insets.left(8)));
        }
    }

    public static void intControl(RenderScreen screen, FlowLayout container, IntProperty property, String name) {
        try (RowBuilder builder = rowBuilder(container)) {
            builder.row.child(new IntegerPropertyTextFieldComponent(screen, Sizing.fixed(45), property, false));
            builder.row.child(new PropertySliderComponent(screen, Sizing.expand(100), Translate.gui(name), property).margins(Insets.horizontal(5)));
            builder.row.child(new ResetPropertyButton(property).margins(Insets.right(5)));
        }
    }

    public static void conditionalIntControl(RenderScreen screen, FlowLayout container, IntProperty property, String name, Supplier<Boolean> displayCondition) {
        FlowLayout row = row();
        row.child(new IntegerPropertyTextFieldComponent(screen, Sizing.fixed(45), property, false));
        row.child(new PropertySliderComponent(screen, Sizing.expand(100), Translate.gui(name), property).margins(Insets.horizontal(5)));
        row.child(new ResetPropertyButton(property).margins(Insets.right(5)));
        container.child(new DynamicComponent(row, displayCondition));
    }

    public static void intPercentageControl(RenderScreen screen, FlowLayout container, IntProperty property, String name) {
        try (RowBuilder builder = rowBuilder(container)) {
            builder.row.child(new IntegerPropertyTextFieldComponent(screen, Sizing.fixed(45), property, true));
            builder.row.child(new PropertySliderComponent(screen, Sizing.expand(100), Translate.gui(name), property).margins(Insets.horizontal(5)));
            builder.row.child(new ResetPropertyButton(property).margins(Insets.right(5)));
        }
    }

    public static void doubleControl(RenderScreen screen, FlowLayout container, DoubleProperty property, String name) {
        try (RowBuilder builder = rowBuilder(container)) {
            builder.row.child(new DoublePropertyTextFieldComponent(screen, Sizing.fixed(45), property));
            builder.row.child(new PropertySliderComponent(screen, Sizing.expand(100), Translate.gui(name), property).margins(Insets.horizontal(5)));
            builder.row.child(new ResetPropertyButton(property).margins(Insets.right(5)));
        }
    }

    public static void booleanControl(FlowLayout container, Property<Boolean> property, String key, Object... args) {
        container.child(new PropertyCheckboxComponent(Translate.gui(key, args), property).margins(Insets.top(5)));
    }

    public static void conditionalBooleanControl(FlowLayout container, Property<Boolean> property, String key, Supplier<Boolean> displayCondition) {
        UIComponent checkbox = new PropertyCheckboxComponent(Translate.gui(key), property).margins(Insets.top(5));
        container.child(new DynamicComponent(checkbox, displayCondition));
    }

    public static LabelComponent text(FlowLayout container, String key, boolean extraVerticalMargins) {
        LabelComponent label = new AutoResizingLabelComponent(Translate.gui(key));
        label.shadow(true);
        if (extraVerticalMargins) {
            label.margins(Insets.top(20));
        }
        label.margins(label.margins().get().withBottom(5));

        container.child(label);
        return label;
    }

    public static LabelComponent text(FlowLayout container, String key, int topMargins) {
        LabelComponent label = new AutoResizingLabelComponent(Translate.gui(key));
        label.shadow(true);
        label.margins(Insets.top(topMargins));
        label.margins(label.margins().get().withBottom(5));

        container.child(label);
        return label;
    }

    public static LabelComponent text(FlowLayout container, Component component, int topMargins) {
        LabelComponent label = new AutoResizingLabelComponent(component);
        label.shadow(true);
        label.margins(Insets.top(topMargins));
        label.margins(label.margins().get().withBottom(5));

        container.child(label);
        return label;
    }

    public static DynamicLabelComponent dynamicText(FlowLayout container, Supplier<Component> content) {
        DynamicLabelComponent label = new DynamicLabelComponent(content).shadow(false);
        label.margins(Insets.bottom(5));

        container.child(label);
        return label;
    }

    public static void dynamicConditionalText(FlowLayout container, Supplier<Boolean> predicate, Supplier<Component> content) {
        DynamicLabelComponent label = new DynamicLabelComponent(content);
        label.shadow(false);
        label.margins(Insets.bottom(5));

        container.child(new DynamicComponent(label, predicate));
    }

    public static void drawExportProgressBar(GuiGraphicsExtractor context, int x, int y, int drawWidth, int barWidth, double speed) {
        int end = x + drawWidth + barWidth;

        int offset = (int) (System.currentTimeMillis() / speed % (drawWidth + barWidth));

        int endWithOffset = x + offset;
        if (endWithOffset > end) endWithOffset = end;

        context.fill(Math.max(x + offset - barWidth, x), y, Math.min(endWithOffset, x + drawWidth), y + 2, 0xFF00FF00);
    }

    public static RowBuilder rowBuilder(FlowLayout container) {
        return new RowBuilder(row(), container);
    }

    public static FlowLayout row() {
        FlowLayout layout = UIContainers.horizontalFlow(Sizing.fill(100), Sizing.content());
        layout.margins(Insets.of(5, 5, 0, 0)).verticalAlignment(VerticalAlignment.CENTER);
        return layout;
    }

    public static RowBuilder autoNewLineRow(FlowLayout container) {
        FlowLayout layout = UIContainers.ltrTextFlow(Sizing.fill(100), Sizing.content());
        layout.margins(Insets.of(5, 5, 0, 0)).verticalAlignment(VerticalAlignment.CENTER);
        return new RowBuilder(layout, container);
    }

    public static class RowBuilder implements AutoCloseable {

        public final FlowLayout row;
        private final FlowLayout container;

        private RowBuilder(FlowLayout row, FlowLayout container) {
            this.row = row;
            this.container = container;
        }

        @Override
        public void close() {
            this.container.child(this.row);
        }
    }
}
