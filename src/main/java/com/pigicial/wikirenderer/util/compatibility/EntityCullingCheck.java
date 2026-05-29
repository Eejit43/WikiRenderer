package com.pigicial.wikirenderer.util.compatibility;

import dev.tr7zw.entityculling.EntityCullingMod;
import net.fabricmc.loader.api.FabricLoader;

public class EntityCullingCheck {
    public static boolean isUsingEntityCulling() {
        if (!FabricLoader.getInstance().isModLoaded("entityculling")) {
            return false;
        }

        try {
            return EntityCullingMod.enabled;
        } catch (NoClassDefFoundError e) {
            return false;
        }
    }

    private static Boolean cullingValue = false;

    public static void disableBlockEntityCullingIfPossible() {
        if (isUsingEntityCulling()) {
            cullingValue = EntityCullingMod.instance.config.skipBlockEntityCulling;
            EntityCullingMod.instance.config.skipBlockEntityCulling = true;
        }
    }

    public static void reEnableBlockEntityCullingIfNecessary() {
        if (isUsingEntityCulling() && cullingValue != null) {
            EntityCullingMod.instance.config.skipBlockEntityCulling = cullingValue;
            cullingValue = null;
        }
    }
}
