package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.pigicial.wikirenderer.property.IntProperty;
import com.pigicial.wikirenderer.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class InterpolatedTimings {
    private final Property<Boolean> useCustomFrameTime = Property.of(false);
    @Nullable
    private IntProperty customFrameTime = null;

    private final List<FrameTime> frameTimes;
    private final int frameCount;

    public InterpolatedTimings(int frameCount) {
        this.frameTimes = new ArrayList<>();
        this.frameCount = frameCount;

        for (int i = 0; i < frameCount; i++) {
            frameTimes.add(new FrameTime());
        }
    }

    public int getFrameCount() {
        return frameCount;
    }

    public void submit(UUID entityID, int index, int millisecondDuration) {
        this.frameTimes.get(index).addMillisecondTiming(entityID, millisecondDuration);
    }

    public int getTickTimingMinimized(int index) {
        int gcd = this.getGreatestCommonDivisor();
        int raw = this.frameTimes.get(index).getAverageTickTime();
        if (useCustomFrameTime.get() && customFrameTime != null) {
            raw = customFrameTime.get();
        }
        return gcd > 1 ? raw / gcd : raw;
    }

    private int getGreatestCommonDivisor() {
        if (true) return 1;
        int divisor = frameTimes.stream()
                .mapToInt(time -> {
                    int averageTickTime = time.getAverageTickTime();
                    return useCustomFrameTime.get() && customFrameTime != null ? customFrameTime.get() : averageTickTime;
                })
                .reduce(0, InterpolatedTimings::gcd);
        if (20D / divisor != (int) (20D / divisor)) {
            return 1;
        }
        return divisor;
    }

    private static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public int getTotalTickDuration() {
        int ticks = 0;
        for (FrameTime frameTime : frameTimes) {
            ticks += frameTime.getAverageTickTime();
        }
        return ticks;
    }

    public int getTotalTickDurationWithOverriding() {
        int ticks = 0;
        for (FrameTime frameTime : frameTimes) {
            if (useCustomFrameTime.get() && customFrameTime != null) {
                ticks += customFrameTime.get();
            } else {
                ticks += frameTime.getAverageTickTime();
            }
        }
        return ticks;
    }

    public int getExportTickDuration() {
        int ticks = 0;
        for (FrameTime frameTime : frameTimes) {
            if (useCustomFrameTime.get() && customFrameTime != null) {
                ticks += customFrameTime.get();
            } else {
                ticks += frameTime.getAverageTickTime();
            }
        }
        int gcd = this.getGreatestCommonDivisor();
        return gcd > 1 ? ticks / gcd : ticks;
    }

    public int getFPS() {
        return 20 / getGreatestCommonDivisor();
    }

    public String getTickValues() {
        List<String> values = new ArrayList<>();
        for (FrameTime frameTime : frameTimes) {
            values.add(String.valueOf(frameTime.getAverageTickTime()));
        }
        return String.join(", ", values);
    }

    public void resetForEntity(UUID entityID) {
        for (FrameTime frameTime : frameTimes) {
            frameTime.clear(entityID);
        }
    }

    public Property<Boolean> getUseCustomFrameTimeProperty() {
        return useCustomFrameTime;
    }

    public IntProperty getOrSetupCustomFrameTimeProperty() {
        if (customFrameTime == null) {
            Map<Integer, Integer> frameTimeFrequencies = new HashMap<>();
            for (FrameTime frameTime : frameTimes) {
                int averageTickTime = frameTime.getAverageTickTime();
                frameTimeFrequencies.put(averageTickTime, frameTimeFrequencies.getOrDefault(averageTickTime, 0) + 1);
            }

            int mostRecurringEntry = 0;
            int mostRecurringEntryAmounts = 0;
            for (Map.Entry<Integer, Integer> entry : frameTimeFrequencies.entrySet()) {
                if (entry.getValue() > mostRecurringEntryAmounts) {
                    mostRecurringEntryAmounts = entry.getValue();
                    mostRecurringEntry = entry.getKey();
                }
            }
            if (mostRecurringEntry == 0) mostRecurringEntry = 1;

            customFrameTime = IntProperty.of(mostRecurringEntry, 1, 1000);
        }
        return customFrameTime;
    }

    public int getAmountOfLoops() {
        return frameTimes.stream().mapToInt(FrameTime::getAmountOfTimings).max().orElse(0);
    }
}
