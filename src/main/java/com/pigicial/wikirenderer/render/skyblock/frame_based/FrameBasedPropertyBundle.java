package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.pigicial.wikirenderer.components.DynamicItemsListComponent;
import com.pigicial.wikirenderer.property.*;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.WikiRendererUI;
import com.pigicial.wikirenderer.util.Translate;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.UIComponents;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.MutableComponent;
import org.joml.Matrix4fStack;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class FrameBasedPropertyBundle<S, R extends Renderable<P>, P extends PropertyBundle> extends DefaultCroppablePropertyBundle {

    protected static final Property<Boolean> ITEM_EXPORT_PROFILE_DATA = Property.of(false);

    private final FrameBasedRenderable<S, R, P> frameBasedRenderable;
    private final PropertyBundle actualProperties;

    public FrameBasedPropertyBundle(FrameBasedRenderable<S, R, P> frameBasedRenderable, PropertyBundle actualProperties) {
        this.frameBasedRenderable = frameBasedRenderable;
        this.actualProperties = actualProperties;

        // sync up properties to make things way easier to work with
        if (this.actualProperties instanceof DefaultPropertyBundle clonedFrom) {
            this.scale = clonedFrom.scale;
            this.rotation = clonedFrom.rotation;
            this.slant = clonedFrom.slant;
            this.xOffset = clonedFrom.xOffset;
            this.yOffset = clonedFrom.yOffset;
            this.rotationSpeed = clonedFrom.rotationSpeed;
            this.allowRotatingWithMouse = clonedFrom.allowRotatingWithMouse;
        }

        if (this.actualProperties instanceof DefaultCroppablePropertyBundle clonedFrom) {
            this.crop = clonedFrom.getCropProperty();
            this.ffmpegCrop = clonedFrom.getFFmpegCropProperty();
            this.rescaleMode = clonedFrom.getRescaleMode();
        }
    }

    @Override
    public void modifySlant(double amount) {
        if (this.actualProperties instanceof DefaultPropertyBundle defaultPropertyBundle) {
            defaultPropertyBundle.modifySlant(amount);
        } else {
            super.modifySlant(amount);
        }
    }

    @Override
    public void modifyRotation(int amount) {
        if (this.actualProperties instanceof DefaultPropertyBundle delegate) {
            delegate.modifyRotation(amount);
        } else {
            super.modifySlant(amount);
        }
    }

    @Override
    public void onRenderStart() {
        this.actualProperties.onRenderStart();
    }

    @Override
    public void applyToViewMatrix(Renderable<?> ignored, Matrix4fStack modelViewStack) {
        this.actualProperties.applyToViewMatrix(this.frameBasedRenderable.currentFrame.renderable(), modelViewStack);
    }

    @Override
    public void buildMainGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        WikiRendererUI.text(container, "frame_data", false);

        if (frameBasedRenderable.getTimingData() != null) {
            InterpolatedTimings timingData = frameBasedRenderable.getTimingData();
            WikiRendererUI.booleanControl(container, timingData.getUseCustomFrameTimeProperty(), "use_manual_frame_timing");
            timingData.getUseCustomFrameTimeProperty().addRebuildListener(screen);

            if (timingData.getUseCustomFrameTimeProperty().get()) {
                WikiRendererUI.labelledTextField(screen, container, timingData.getOrSetupCustomFrameTimeProperty(), "manual_frame_timing", Sizing.fixed(30));
            }
        }

        WikiRendererUI.dynamicText(container, () -> {
            InterpolatedTimings timingData = frameBasedRenderable.getTimingData();
            if (timingData != null) {
                int amountOfLoops = timingData.getAmountOfLoops();
                MutableComponent data = Translate.gui("ticks_amount_with_data",
                        timingData.getTotalTickDuration(),
                        timingData.getTickValues(),
                        amountOfLoops,
                        amountOfLoops == 1 ? "" : "s"
                ).withStyle(ChatFormatting.GRAY);

                if (timingData.getUseCustomFrameTimeProperty().get()) {
                    data.withStyle(ChatFormatting.STRIKETHROUGH);
                    data = Translate.gui("ticks_amount", timingData.getTotalTickDurationWithOverriding()).append(" ").append(data.withStyle(ChatFormatting.DARK_GRAY));
                }

                return Translate.gui("animation_timing_data", data);
            } else {
                return Translate.gui("incomplete_animation");
            }

        }).margins(Insets.vertical(7));

        WikiRendererUI.dynamicText(container, () -> Translate.gui(
                "batch.amount",
                frameBasedRenderable.currentIndex + 1,
                frameBasedRenderable.currentDataSet.size(),
                Math.max(0, frameBasedRenderable.currentDataSet.size() - frameBasedRenderable.currentIndex - 1)
        )).margins(Insets.vertical(7).withTop(4));

        container.child(new DynamicItemsListComponent<>(this.frameBasedRenderable));

        if (renderable instanceof ItemFrameBasedRenderable) {
            WikiRendererUI.booleanControl(container, ITEM_EXPORT_PROFILE_DATA, "export_profile_data");
        }

        if (!GraphicsEnvironment.isHeadless()) {
            try (WikiRendererUI.RowBuilder builder = WikiRendererUI.autoNewLineRow(container)) {
                ButtonComponent copyAnimationDataButton = UIComponents.button(Translate.gui("copy_animation_data"), _ -> {
                    screen.notify(Translate.gui("copied_animation_data_to_clipboard"));

                    String text = String.join("\n", frameBasedRenderable.generateWikiTextFile(frameBasedRenderable.currentDataSet));
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), (_, _) -> {
                    });
                });
                copyAnimationDataButton.margins(Insets.bottom(5));
                builder.row.child(copyAnimationDataButton);

                if (renderable instanceof ItemFrameBasedRenderable itemFrameBasedRenderable) {
                    ButtonComponent copyProfilesButton = UIComponents.button(Translate.gui("copy_profile_data"), _ -> {
                        screen.notify(Translate.gui("copied_profile_data_to_clipboard"));

                        String text = String.join("\n", itemFrameBasedRenderable.generateProfileJson());
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), (_, _) -> {
                        });
                    });
                    copyProfilesButton.margins(Insets.bottom(5));
                    builder.row.child(copyProfilesButton);
                }
            }
        }

        this.actualProperties.buildMainGUIControls(frameBasedRenderable.currentFrame.renderable(), screen, container);
    }

    @Override
    public void buildRenderOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        this.actualProperties.buildRenderOptionGUIControls(this.frameBasedRenderable.currentFrame.renderable(), screen, container);
    }

    @Override
    public int getExportResolution(Renderable<?> ignored) {
        return this.actualProperties.getExportResolution(this.frameBasedRenderable.currentFrame.renderable());
    }

    @Override
    public void buildExportResolutionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        this.actualProperties.buildExportResolutionGUIControls(this.frameBasedRenderable.currentFrame.renderable(), screen, container);
    }

    @Override
    public void buildFileNameGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        this.actualProperties.buildFileNameGUIControls(renderable, screen, container);
    }

    @Override
    public void buildExportOptionGUIControls(Renderable<?> renderable, RenderScreen screen, FlowLayout container) {
        if (this.actualProperties instanceof CroppablePropertyBundle croppablePropertyBundle) {
            croppablePropertyBundle.buildExportOptionGUIControls(renderable, screen, container);
        } else {
            super.buildRegularExportOptions(renderable, screen, container);
        }
    }
}
