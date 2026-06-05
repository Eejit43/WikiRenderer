package com.pigicial.wikirenderer.render.item;

import com.mojang.math.Axis;
import com.pigicial.wikirenderer.property.DefaultCroppablePropertyBundle;
import com.pigicial.wikirenderer.property.Property;
import com.pigicial.wikirenderer.property.SerializablePropertyBundle;
import com.pigicial.wikirenderer.property.config.WikiRendererConfigs;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ImageRescaleMode;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.ItemBlockUtil;
import io.wispforest.owo.ui.container.FlowLayout;
import net.minecraft.world.item.Items;
import org.joml.Matrix4fStack;

public class ItemRenderablePropertyBundle extends DefaultCroppablePropertyBundle implements SerializablePropertyBundle {

    public static final ItemRenderablePropertyBundle INSTANCE = WikiRendererConfigs.loadOrDefault(new ItemRenderablePropertyBundle());

    public final Property<Boolean> allowScalingWithMouse = Property.of(false);
    public final Property<Boolean> forceEnchantmentGlints = Property.of(false);
    protected int blockItemsExportResolution = 300;

    @Override
    public String getConfigFileName() {
        return "item_render_settings";
    }

    @Override
    protected int getDefaultExportResolution() {
        return 160;
    }

    @Override
    public void setExportResolution(Renderable<?> renderable, int exportResolution) {
        if (ItemBlockUtil.doesItemUseBlockLight(((ItemRenderable) renderable).stack)) {
            blockItemsExportResolution = exportResolution;
        } else {
            super.setExportResolution(renderable, exportResolution);
        }
    }

    @Override
    public int getExportResolution(Renderable<?> renderable) {
        if (ItemBlockUtil.doesItemUseBlockLight(((ItemRenderable) renderable).stack)) {
            return blockItemsExportResolution;
        } else {
            return super.getExportResolution(renderable);
        }
    }

    @Override
    protected boolean shouldCropByDefault() {
        return false;
    }

    @Override
    protected int getDefaultRotation() {
        return 0;
    }

    @Override
    protected double getDefaultSlant() {
        return 0;
    }

    @Override
    public void modifyScale(double amount) {
        if (allowScalingWithMouse.get() || (crop.get() && rescaleMode.get() != ImageRescaleMode.DISABLED)) {
            super.modifyScale(amount);
        }
    }

    @Override
    public void applyToViewMatrix(Renderable<?> renderable, Matrix4fStack modelViewStack) {
        float scale = (this.scale.get() / 100f) * 2f;
        modelViewStack.scale(scale, scale, scale);

        modelViewStack.translate(this.xOffset.get() / 26000f, this.yOffset.get() / -26000f, 0);

        modelViewStack.rotate(Axis.XP.rotationDegrees(this.slant.get().floatValue()));
        modelViewStack.rotate(Axis.YP.rotationDegrees(this.rotation.get() + this.updateAndGetSpinningRotationOffset()));
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.text(container, "transform_options", false);
        WikiRendererUI.intControl(screen, container, scale, "scale");
        if (!crop.get() || rescaleMode.get() == ImageRescaleMode.DISABLED) {
            WikiRendererUI.booleanControl(container, this.allowScalingWithMouse, "allow_scaling_with_mouse");
        }

        WikiRendererUI.text(container, "item_scale_warning_1", 10);
        WikiRendererUI.text(container, "item_scale_warning_2", false);
        WikiRendererUI.intControl(screen, container, rotation, "rotation");
        WikiRendererUI.doubleControl(screen, container, slant, "slant");
        WikiRendererUI.intControl(screen, container, rotationSpeed, "rotation_speed");
        WikiRendererUI.booleanControl(container, this.allowRotatingWithMouse, "allow_rotating_with_mouse");
        container.child(this.buildResetButton());

        // todo figure out a better way to check for glint support
        if (!((ItemRenderable) renderable).stack.is(Items.PLAYER_HEAD)) {
            WikiRendererUI.text(container, "item_options", true);
            WikiRendererUI.booleanControl(container, forceEnchantmentGlints, "force_enchanted");
        }
    }
}
