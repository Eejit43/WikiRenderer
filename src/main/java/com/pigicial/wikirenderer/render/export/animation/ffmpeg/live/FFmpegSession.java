package com.pigicial.wikirenderer.render.export.animation.ffmpeg.live;

import com.mojang.blaze3d.platform.NativeImage;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.NativeImageAccessor;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.export.animation.ffmpeg.FFmpegDispatcher;
import org.lwjgl.system.MemoryUtil;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FFmpegSession implements AutoCloseable {

    private final Process process;
    private final WritableByteChannel channel;
    private final ByteBuffer buffer;
    private final int width;
    private final int height;

    public FFmpegSession(Path tempMasterFile, int width, int height) throws IOException {
        this.width = width;
        this.height = height;
        this.buffer = ByteBuffer.allocateDirect(width * height * 4);
        this.buffer.order(ByteOrder.nativeOrder());

        String ffmpegPath = FFmpegDispatcher.getResolvedOrFallbackFFmpegPath();
        List<String> args = new ArrayList<>(List.of(
                ffmpegPath,
                "-y",
                "-f", "rawvideo",
                "-pixel_format", "rgba",
                "-video_size", width + "x" + height,
                "-framerate", String.valueOf(GlobalProperties.get().exportFramerate.get()),
                "-i", "-",

                "-c:v", "prores_ks",
                "-profile:v", "4",
                "-vendor", "apl0",
                "-bits_per_mb", "8000",
                "-pix_fmt", "yuva444p10le",

                "-threads", String.valueOf(Math.max(1, Runtime.getRuntime().availableProcessors() / 2)),
                tempMasterFile.toString()
        ));

        ProcessBuilder pb = new ProcessBuilder(args);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        this.process = pb.start();
        BufferedOutputStream bos = new BufferedOutputStream(this.process.getOutputStream(), 1024 * 1024);
        this.channel = Channels.newChannel(bos);

        WikiRenderer.LOGGER.info("Streaming pipe opened to: {}", tempMasterFile.getFileName());
    }

    public void pushFrame(NativeImage image) throws IOException {
        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IllegalArgumentException("Image dimensions mismatch video settings");
        }

        NativeImageAccessor accessor = (NativeImageAccessor) (Object) image;
        long pointer = accessor.getPixelsPointer();

        if (pointer == 0L) throw new IllegalStateException("NativeImage has been closed!");


        long size = (long) width * height * 4;
        buffer.clear();
        MemoryUtil.memCopy(pointer, MemoryUtil.memAddress(buffer), size);

        buffer.position(0);
        buffer.limit((int) size);

        while (buffer.hasRemaining()) {
            channel.write(buffer);
        }
    }

    @Override
    public void close() throws Exception {
        if (channel != null) channel.close();
        this.buffer.clear();
        if (process != null) {
            process.waitFor();
        }
    }
}
