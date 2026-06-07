package com.pigicial.wikirenderer.util;

import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

public class Translate {

    public static final Component PREFIX = generatePrefix("WikiRenderer", 190, 155);

    public static MutableComponent make(String key, Object... args) {
        return Component.translatable("message.wikirenderer." + key, args);
    }

    public static MutableComponent gui(String key, Object... args) {
        return Component.translatable("gui.wikirenderer." + key, args);
    }

    @Nullable
    public static MutableComponent guiIfExists(String key, Object... args) {
        String fullKey = "gui.wikirenderer." + key;
        return I18n.exists(fullKey) ? Component.translatable(fullKey, args) : null;
    }

    public static MutableComponent msg(String key, Object... args) {
        return prefixed(make(key, args).withStyle(ChatFormatting.GRAY));
    }

    public static void commandFeedback(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        context.getSource().sendFeedback(msg(key, args));
    }

    public static void commandError(CommandContext<FabricClientCommandSource> context, String key, Object... args) {
        if (context != null) {
            context.getSource().sendError(msg(key, args));
        } else {
            sendMessage(key, args);
        }
    }

    public static void sendMessage(String key, Object... args) {
        Component message = msg(key, args);
        Minecraft.getInstance().gui.getChat().addClientSystemMessage(message);
        Minecraft.getInstance().getNarrator().saySystemChatQueued(message);
    }

    public static MutableComponent prefixed(Component text) {
        return Component.empty()
                .append(PREFIX)
                .append(Component.literal(" > ").withStyle(ChatFormatting.DARK_GRAY))
                .append(text);
    }

    @SuppressWarnings("SameParameterValue")
    private static Component generatePrefix(String text, int startHue, int endHue) {
        int hueSpan = endHue - startHue;
        char[] chars = text.toCharArray();

        MutableComponent prefixText = Component.empty();

        for (int i = 0; i < chars.length; i++) {
            float index = i;
            prefixText.append(Component.literal(String.valueOf(chars[i])).withStyle(style ->
                    style.withColor(Mth.hsvToRgb((startHue + (index / chars.length) * hueSpan) / 360, 1, 0.96f))
            ));
        }

        return prefixText;
    }

    public static void actionBar(String key, Object... args) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.sendOverlayMessage(make(key, args));
        }
    }
}
