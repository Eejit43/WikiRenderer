package com.pigicial.wikirenderer.render.export;

import com.mojang.blaze3d.platform.NativeImage;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.util.Translate;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class FileIO {

    private static final AtomicInteger TASK_COUNT = new AtomicInteger(0);

    public static CompletableFuture<File> saveImage(NativeImage image, ExportPathSpec path) {
        CompletableFuture<File> future = new CompletableFuture<>();

        TASK_COUNT.incrementAndGet();
        try (ForkJoinPool pool = ForkJoinPool.commonPool()) {
            pool.submit(() -> {
                File imageFile = path.resolveFile("png");
                imageFile.getParentFile().mkdirs();

                try {
                    image.writeToFile(imageFile);
                    WikiRenderer.LOGGER.info("Image {} saved", imageFile.getAbsolutePath());
                    future.complete(imageFile);
                } catch (IOException e) {
                    WikiRenderer.LOGGER.warn("Could not save image {}", imageFile.getAbsolutePath(), e);
                    future.completeExceptionally(e);
                } finally {
                    TASK_COUNT.decrementAndGet();
                }
            });
        }

        return future;
    }

    public static CompletableFuture<File> saveText(String text, ExportPathSpec path) {
        CompletableFuture<File> future = new CompletableFuture<>();

        TASK_COUNT.incrementAndGet();
        try (ForkJoinPool pool = ForkJoinPool.commonPool()) {
            pool.submit(() -> {
                File textFile = path.resolveFile("txt");
                textFile.getParentFile().mkdirs();

                try {
                    Files.writeString(
                            textFile.toPath(),
                            text,
                            StandardCharsets.UTF_8
                    );
                    future.complete(textFile);
                } catch (IOException e) {
                    WikiRenderer.LOGGER.warn("Could not save text {}", textFile.getAbsolutePath(), e);
                    future.completeExceptionally(e);
                } finally {
                    TASK_COUNT.decrementAndGet();
                }
            });
        }

        return future;
    }

    public static void saveTextAndNotify(String text, ExportPathSpec path, RenderScreen renderScreen, String key) {
        FileIO.saveText(text, path).whenComplete((textFile, _) -> Minecraft.getInstance().execute(() -> renderScreen.notify(
                () -> Util.getPlatform().openFile(textFile),
                Translate.gui(key),
                Component.literal(ExportPathSpec.exportRoot().relativize(textFile.toPath()).toString())
        )));
    }

    public static void deleteSequenceFilesFromPath(Path sequencePath) {
        if (GlobalProperties.get().saveIndividualFrames.get()) {
            return;
        }

        try (Stream<Path> p = Files.list(sequencePath)) {
            p.filter(path -> path.getFileName().toString().matches("seq_\\d+\\.png"))
                    .forEach(deletePath -> {
                        try {
                            Files.delete(deletePath);
                        } catch (IOException e) {
                            WikiRenderer.LOGGER.warn("Could not clean up sequence directory", e);
                        }
                    });
        } catch (IOException e) {
            WikiRenderer.LOGGER.warn("Could not clean up sequence directory", e);
        }

        try {
            Files.delete(sequencePath);
        } catch (IOException e) {
            WikiRenderer.LOGGER.warn("Could not delete up sequence directory", e);
        }
    }

    public static int taskCount() {
        return TASK_COUNT.get();
    }

    public static Component progressText() {
        int jobs = taskCount();
        if (jobs == 0) return Translate.gui("exporter.idle");
        return Translate.gui("exporter.jobs", jobs);
    }

    public static Path next(Path input) {
        String filename = input.getFileName().toString();

        int separatorIndex = filename.lastIndexOf('.');
        if (separatorIndex == -1) separatorIndex = filename.length();

        String name = filename.substring(0, separatorIndex);
        String extension = filename.substring(separatorIndex);

        Path path = input.getParent();

        Path currentPath = path.resolve(join(name, extension, 0));
        Path lastPath = currentPath;

        for (int i = 1; Files.exists(currentPath); i++) {
            lastPath = currentPath;
            currentPath = path.resolve(join(name, extension, i));
        }

        return GlobalProperties.get().overwriteLatest.get() ? lastPath : currentPath;
    }

    private static String join(String filename, String extension, int index) {
        return index == 0
                ? filename + extension
                : filename + "_" + index + (extension.isEmpty() ? "" : "_" + extension);
    }

}
