package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.pigicial.wikirenderer.WikiRenderer;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.TreeMap;

public class DyeColorTiming {
    private static final int DUPLICATE_THRESHOLD_MS = 40;

    private final TreeMap<Integer, DyedArmorColorData> textureTimings = new TreeMap<>();
    private final long startingTime;

    public DyeColorTiming(DyedArmorColorData texturesPayload) {
        this.startingTime = System.currentTimeMillis();
        this.submitTimings(texturesPayload);
    }

    public void submitTimings(DyedArmorColorData armorColorData) {
        DyedArmorColorData mostRecentColors = getMostRecentTexture();
        int currentMsOffset = (int) (System.currentTimeMillis() - startingTime);

        if (mostRecentColors != null) {
            boolean isIdentical = mostRecentColors.helmetColor() == armorColorData.helmetColor()
                                  && mostRecentColors.chestplateColor() == armorColorData.chestplateColor()
                                  && mostRecentColors.leggingsColor() == armorColorData.leggingsColor()
                                  && mostRecentColors.bootsColor() == armorColorData.bootsColor();
            if (isIdentical) return;

            boolean hasMatchingPieces = mostRecentColors.helmetColor() == armorColorData.helmetColor()
                                  || mostRecentColors.chestplateColor() == armorColorData.chestplateColor()
                                  || mostRecentColors.leggingsColor() == armorColorData.leggingsColor()
                                  || mostRecentColors.bootsColor() == armorColorData.bootsColor();

            int lastMsOffset = textureTimings.lastKey();
            boolean isWithinTickWindow = (currentMsOffset - lastMsOffset) < DUPLICATE_THRESHOLD_MS;
            if (hasMatchingPieces && isWithinTickWindow) {
                textureTimings.remove(lastMsOffset); // override previous similar entry, probably from delays packets
                currentMsOffset = lastMsOffset;
            }
        }

        textureTimings.put(currentMsOffset, armorColorData);
    }

    public TreeMap<Integer, DyedArmorColorData> getColorTimings() {
        return textureTimings;
    }

    @Nullable
    public DyedArmorColorData getMostRecentTexture() {
        Map.Entry<Integer, DyedArmorColorData> entry = this.textureTimings.lastEntry();
        return entry == null ? null : entry.getValue();
    }

    public void log() {
        int index = 0;
        for (Map.Entry<Integer, DyedArmorColorData> entry : textureTimings.entrySet()) {
            index++;
            WikiRenderer.LOGGER.debug("{}. {} = {}", index, entry.getKey(), entry.getValue());
        }
    }
}
