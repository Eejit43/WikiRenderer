package com.pigicial.wikirenderer.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public class ScreenSchedulerAndSaver {

    private static RenderScreen SCHEDULED_SCREEN = null;
    private static RenderScreen SAVED_SCREEN = null;

    public static void schedule(RenderScreen screen) {
        if (Minecraft.getInstance().screen == null) {
            SCHEDULED_SCREEN = screen;
            openScheduledScreen();
        } else {
            SCHEDULED_SCREEN = screen;
        }
    }

    public static void setSavedScreen(RenderScreen screen) {
        SAVED_SCREEN = screen;
    }

    public static RenderScreen getSavedScreen() {
        return SAVED_SCREEN;
    }

    public static void openImmediately(RenderScreen screen) {
        SCHEDULED_SCREEN = screen;
        openScheduledScreen();
    }

    public static boolean hasScheduled() {
        return SCHEDULED_SCREEN != null;
    }

    public static RenderScreen getScheduledScreen() {
        return SCHEDULED_SCREEN;
    }

    public static void openScheduledScreen() {
        if (SAVED_SCREEN != null && SAVED_SCREEN != SCHEDULED_SCREEN) {
            SAVED_SCREEN.removed();
            SAVED_SCREEN = null;
        }

        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
            SCHEDULED_SCREEN.setPreviouslyOpenedContainerScreen(containerScreen);
        }

        Minecraft.getInstance().setScreen(SCHEDULED_SCREEN);
        SCHEDULED_SCREEN = null;
    }

}
