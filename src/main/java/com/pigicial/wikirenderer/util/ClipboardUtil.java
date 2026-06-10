package com.pigicial.wikirenderer.util;

import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class ClipboardUtil {
    private static final boolean HAS_CLIPBOARD;

    static {
        boolean hasClipboard;
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard();
            hasClipboard = true;
        } catch (HeadlessException e) {
            hasClipboard = false;
        }

        HAS_CLIPBOARD = hasClipboard;
    }

    public static boolean hasClipboardAccess() {
        return HAS_CLIPBOARD;
    }

    public static void setClipboard(String text) {
        if (HAS_CLIPBOARD) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), (clipboard, contents) -> {});
        }
    }

    public static void setClipboard(ImageTransferable imageTransferable) {
        if (HAS_CLIPBOARD) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imageTransferable, imageTransferable);
        }
    }
}
