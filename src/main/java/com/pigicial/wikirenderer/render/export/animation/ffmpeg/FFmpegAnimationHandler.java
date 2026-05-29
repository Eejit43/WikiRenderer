package com.pigicial.wikirenderer.render.export.animation.ffmpeg;

import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.render.export.ExportPathSpec;
import com.pigicial.wikirenderer.render.export.FileIO;
import com.pigicial.wikirenderer.render.export.ImageCropper;
import com.pigicial.wikirenderer.render.export.animation.AnimationHandler;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import net.minecraft.client.Minecraft;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class FFmpegAnimationHandler extends AnimationHandler {
    protected FFmpegAnimationHandler(RenderScreen screen, Renderable<?> renderable, int framesToRender) {
        super(screen, renderable, framesToRender);
    }

    protected final void mergeFilesIntoFinalResult(List<CompletableFuture<File>> fileFutures, boolean overwriteValue) {
        this.finished = true;
        ExportPathSpec defaultExportPath = this.renderable.getExportPath();
        ExportPathSpec exportPath = defaultExportPath.differentFileName(renderable.getCustomFileName());

        CompletableFuture.allOf(fileFutures.toArray(CompletableFuture[]::new))
                .whenComplete((v_, throwable) -> {
                    GlobalProperties globalProperties = GlobalProperties.get();
                    globalProperties.overwriteLatest.set(overwriteValue);

                    boolean keepingFiles = globalProperties.saveIndividualFrames.get();
                    if (throwable != null || closed) {
                        FileIO.deleteSequenceFilesFromPath(this.framesFolder);
                        return;
                    }

                    this.screen.exportAnimationButton.setMessage(Translate.gui("converting"));
                    Minecraft.getInstance().execute(() -> screen.notify(Translate.gui("converting_image_sequence")));

                    FFmpegDispatcher.exportAnimation(
                            exportPath,
                            this.framesFolder,
                            globalProperties.animationFormat,
                            this,
                            ImageCropper.getFFmpegCropSize(renderable, collectedCropData)
                    ).whenComplete((animationFile, animationThrowable) -> this.finishAndCleanup(animationFile, animationThrowable, keepingFiles ? this.framesFolder : null));
                });
    }
}
