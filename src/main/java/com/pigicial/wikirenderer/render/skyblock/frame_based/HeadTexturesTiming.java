package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.textures.TextureData;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class HeadTexturesTiming {
    private final TreeMap<Integer, TextureData> textureTimings = new TreeMap<>();
    private final long startingTime;

    public HeadTexturesTiming(TextureData texturesPayload) {
        this.startingTime = System.currentTimeMillis();
        this.submitTimings(texturesPayload);
    }

    public void submitTimings(TextureData texturesPayload) {
        TextureData mostRecentTexture = getMostRecentTexture();
        if (mostRecentTexture == null || !Objects.equals(mostRecentTexture.profile(), texturesPayload.profile())) {
            int currentMsOffset = (int) (System.currentTimeMillis() - startingTime);
            textureTimings.put(currentMsOffset, texturesPayload);
        }
    }

    public TreeMap<Integer, TextureData> getTextureTimings() {
        return textureTimings;
    }

    @Nullable
    public TextureData getMostRecentTexture() {
        Map.Entry<Integer, TextureData> entry = this.textureTimings.lastEntry();
        return entry == null ? null : entry.getValue();
    }

    public void log() {
        int index = 0;
        for (Map.Entry<Integer, TextureData> entry : textureTimings.entrySet()) {
            index++;
            WikiRenderer.LOGGER.debug("{}. {} = {}", index, entry.getKey(), entry.getValue().payload().textures().get(MinecraftProfileTexture.Type.SKIN).getUrl());
        }
    }
}
