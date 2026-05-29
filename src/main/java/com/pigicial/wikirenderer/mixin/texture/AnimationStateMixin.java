package com.pigicial.wikirenderer.mixin.texture;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.export.animation.AnimationHandler;
import com.pigicial.wikirenderer.screen.RenderScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.AnimationState.class)
public class AnimationStateMixin {

    @Shadow
    private int frame;

    @Shadow
    private int subFrame;

    @Shadow
    @Final
    private SpriteContents.AnimatedTexture animationInfo;

    @Shadow
    private boolean isDirty;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    public void tick(CallbackInfo ci) {
        GlobalProperties globalProperties = GlobalProperties.get();

        if (Minecraft.getInstance().screen instanceof RenderScreen && globalProperties.syncTextureAnimationsToAnimation.get()) {
            AnimationHandler animationHandler = WikiRenderer.currentAnimationHandler;
            List<SpriteContents.FrameInfo> frames = this.animationInfo.frames;
            if (animationHandler != null && !animationHandler.isFinished()) {
                int totalFrameCount = animationHandler.getAnimationFrames();
                int framesRenderedSoFar = totalFrameCount - animationHandler.getRemainingFrames();

                int frameRate = globalProperties.exportFramerate.get();
                double secondsIntoAnimation = (double) framesRenderedSoFar / (double) frameRate;
                int tick = (int) Math.floor(secondsIntoAnimation * 20);

                int frame = 0;
                int subFrame = tick;
                while (true) {
                    int frameTime = frames.get(frame).time();
                    if (subFrame >= frameTime) {
                        subFrame -= frameTime;
                        frame = (frame + 1) % frames.size();
                    } else {
                        break;
                    }
                }

                int previousFrameIndex = frames.get(this.frame).index();
                this.frame = frame;
                this.subFrame = subFrame;
                int currentFrameIndex = frames.get(this.frame).index();
                if (previousFrameIndex != currentFrameIndex) {
                    this.isDirty = true;
                }

                ci.cancel();
            } else {
                int previousFrameIndex = frames.get(this.frame).index();
                this.frame = 0;
                this.subFrame = -1;
                int currentFrameIndex = frames.get(this.frame).index();
                if (previousFrameIndex != currentFrameIndex) {
                    this.isDirty = true;
                }
            }
        }
    }
}
