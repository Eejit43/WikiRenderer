package com.pigicial.wikirenderer.util;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtil {
    private static final boolean HAS_TEXT_CLIPBOARD;
    private static final boolean IS_SYSTEM_MAC;

    static {
        boolean hasClipboard;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard();
            hasClipboard = true;
        } catch (HeadlessException e) {
            hasClipboard = false;
        }
        HAS_TEXT_CLIPBOARD = hasClipboard;
        IS_SYSTEM_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    }

    public static boolean hasTextClipboardAccess() {
        return HAS_TEXT_CLIPBOARD;
    }

    public static boolean hasImageClipboardAccess() {
        return !GraphicsEnvironment.isHeadless() && !IS_SYSTEM_MAC;
    }

    public static void setClipboard(String text) {
        if (HAS_TEXT_CLIPBOARD) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), (clipboard, contents) -> {});
        }
    }

    public static void setClipboard(ImageTransferable imageTransferable) {
        if (hasImageClipboardAccess()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imageTransferable, imageTransferable);
        }
    }
}
