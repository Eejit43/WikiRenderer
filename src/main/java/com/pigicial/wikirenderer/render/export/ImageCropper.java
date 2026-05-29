package com.pigicial.wikirenderer.render.export;

import com.mojang.blaze3d.platform.NativeImage;
import com.pigicial.wikirenderer.render.Renderable;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class ImageCropper {
    public static NativeImage cropTransparentAndCloseSource(NativeImage source) {
        CropData cropData = getCropData(source);
        return cropTransparentAndCloseSource(source, cropData);
    }

    public static NativeImage cropTransparentAndCloseSource(NativeImage source, @Nullable CropData cropData) {
        if (cropData == null) {
            return source;
        }

        int croppedWidth = cropData.maxX() - cropData.minX() + 1;
        int croppedHeight = cropData.maxY() - cropData.minY() + 1;
        NativeImage cropped = new NativeImage(source.format(), croppedWidth, croppedHeight, false);
        source.copyRect(cropped, cropData.minX(), cropData.minY(), 0, 0, croppedWidth, croppedHeight, false, false);
        source.close();
        return cropped;
    }

    @Nullable
    public static CropData getCropData(NativeImage source) {
        int width = source.getWidth();
        int height = source.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte alphaByte = source.getLuminanceOrAlpha(x, y);
                int alpha = alphaByte & 0xFF;

                if (alpha > 0) {
                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }
            }
        }

        if (maxX == -1) return null;
        return new CropData(minX, maxX, minY, maxY);
    }

    @Nullable
    public static CropData combineCropDataIfNecessary(Renderable<?> renderable, List<CropData> dataList) {
        if (!renderable.shouldCropForFFmpeg() || dataList.isEmpty()) {
            return null;
        }

        int minX = dataList.stream().mapToInt(CropData::minX).min().orElseThrow();
        int maxX = dataList.stream().mapToInt(CropData::maxX).max().orElseThrow();
        int minY = dataList.stream().mapToInt(CropData::minY).min().orElseThrow();
        int maxY = dataList.stream().mapToInt(CropData::maxY).max().orElseThrow();
        return new CropData(minX, maxX, minY, maxY);
    }

    public static String getFFmpegCropSize(Renderable<?> renderable, List<CropData> dataList) {
        dataList.removeIf(Objects::isNull);
        if (!renderable.shouldCropForFFmpeg() || dataList.isEmpty()) {
            return "";
        }
        int exportResolution = renderable.getProperties().getExportResolution(renderable);

        int offsetFromLeft = dataList.stream().mapToInt(CropData::minX).min().orElseThrow();
        int offsetFromRight = exportResolution - dataList.stream().mapToInt(CropData::maxX).max().orElseThrow();
        // min y is a little confusing since to my brain it implies from the bottom, but its from the top instead
        int offsetFromTop = dataList.stream().mapToInt(CropData::minY).min().orElseThrow();
        int offsetFromBottom = exportResolution - dataList.stream().mapToInt(CropData::maxY).max().orElseThrow();

        int width = exportResolution - offsetFromRight - offsetFromLeft;
        int height = exportResolution - offsetFromBottom - offsetFromTop;

        if (width % 2 != 0) width++;
        if (height % 2 != 0) height++;

        // fFmpeg syntax: crop=w:h:x:y
        return "crop="+ width + ":" + height + ":" + offsetFromLeft + ":" + offsetFromTop;
    }

}
