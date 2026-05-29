package com.pigicial.wikirenderer.render.export.animation;

public enum AnimationFormat {
    GIF("gif", new String[]{"-plays", "0"}),
    MP4("mp4", new String[]{"-preset", "slow", "-crf", "20", "-pix_fmt", "yuv420p"}),
    APNG("apng", new String[]{"-plays", "0", "-pix_fmt", "rgba"}),
    WEBP("webp", new String[]{"-plays", "0", "-loop", "0", "-pix_fmt", "rgba"}),
    MOV("mov", new String[]{"-c:v", "prores_ks", "-profile:v", "4444", "-q:v", "1", "-pix_fmt", "yuva444p10le"});

    public final String extension;
    public final String[] ffmpegArguments;

    AnimationFormat(String extension, String[] ffmpegArguments) {
        this.extension = extension;
        this.ffmpegArguments = ffmpegArguments;
    }

    public AnimationFormat next() {
        return switch (this) {
            case GIF -> MP4;
            case MP4 -> APNG;
            case APNG -> WEBP;
            case WEBP -> MOV;
            case MOV -> GIF;
        };
    }
}
