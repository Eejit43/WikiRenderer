package com.pigicial.wikirenderer.property;

import com.pigicial.wikirenderer.property.config.WikiRendererConfigs;
import com.pigicial.wikirenderer.render.export.ffmpeg.AnimationHandlingMode;
import com.pigicial.wikirenderer.render.export.ffmpeg.FFmpegDispatcher;

public class GlobalProperties implements SerializablePropertyBundle {

    private static final GlobalProperties INSTANCE = WikiRendererConfigs.loadOrDefault(new GlobalProperties());

    public static GlobalProperties get() {
        return INSTANCE;
    }

    public int backgroundColor = 0xFF000000;
    public final Property<Boolean> showBackgroundColorInExports = Property.of(false);
    public final Property<Boolean> tickTextureAnimations = Property.of(true);

    public final Property<Boolean> debugShowCollidingEntityBoundsForAreas = Property.of(false);
    public final Property<Boolean> unsafe = Property.of(false);
    public final Property<Boolean> saveIntoRoot = Property.of(false);
    public final Property<Boolean> overwriteLatest = Property.of(false);

    public final Property<Boolean> tickParticles = Property.of(true);

    public final Property<Boolean> useCustomFFmpegPath = Property.of(false);
    public String customFFmpegPath = "";

    public final Property<Boolean> speedUpEnchantmentGlints = Property.of(false);
    public final Property<Boolean> syncEnchantmentGlintsToExport = Property.of(false);
    public final Property<Boolean> syncRotationToAnimation = Property.of(false);
    public final Property<Boolean> syncTextureAnimationsToAnimation = Property.of(false);
    public final Property<Boolean> setAnimationFpsCap = Property.of(true);
    public final Property<Boolean> loopParticles = Property.of(false);

    public final IntProperty exportFramerate = IntProperty.of(20, 1, 300);
    public final IntProperty exportFrames = IntProperty.of(60, 1, 5000);
    public AnimationHandlingMode animationHandlingMode = AnimationHandlingMode.LIVE_FFMPEG;
    public FFmpegDispatcher.Format animationFormat = FFmpegDispatcher.Format.GIF;
    public Property<Boolean> saveIndividualFrames = Property.of(false);

    @Override
    public String getConfigFileName() {
        return "general_render_settings";
    }
}
