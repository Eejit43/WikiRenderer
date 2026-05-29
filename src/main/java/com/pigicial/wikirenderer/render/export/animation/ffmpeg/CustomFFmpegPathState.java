package com.pigicial.wikirenderer.render.export.animation.ffmpeg;

import net.minecraft.ChatFormatting;

public enum CustomFFmpegPathState {
    NOT_CHECKED(ChatFormatting.GRAY),
    CHECKING(ChatFormatting.YELLOW),
    FOUND(ChatFormatting.GREEN),
    NOT_FOUND(ChatFormatting.RED);

    private final ChatFormatting color;

    CustomFFmpegPathState(ChatFormatting color) {
        this.color = color;
    }

    public ChatFormatting getColor() {
        return color;
    }
}
