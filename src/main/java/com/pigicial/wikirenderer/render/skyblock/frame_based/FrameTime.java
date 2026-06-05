package com.pigicial.wikirenderer.render.skyblock.frame_based;

import java.util.*;
import java.util.stream.Collectors;

public class FrameTime {
    private final Map<UUID, List<Integer>> millisecondTimings = new HashMap<>();
    private int averageTickTime;

    public void addMillisecondTiming(UUID entityID, int millisecondTime) {
        this.millisecondTimings.computeIfAbsent(entityID, _ -> new ArrayList<>()).add(millisecondTime);

        List<Integer> allTimings = this.millisecondTimings.values().stream()
                .flatMap(List::stream)
                .toList();

        this.averageTickTime = allTimings.stream()
                .map(ms -> (int) Math.round(ms / 50.0))
                .collect(Collectors.groupingBy(i -> i, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    public int getAverageTickTime() {
        return averageTickTime;
    }

    public void clear(UUID entityID) {
        millisecondTimings.remove(entityID);
    }

    public int getAmountOfTimings() {
        int amount = 0;
        for (List<Integer> values : millisecondTimings.values()) {
            amount += values.size();
        }
        return amount;
    }
}
