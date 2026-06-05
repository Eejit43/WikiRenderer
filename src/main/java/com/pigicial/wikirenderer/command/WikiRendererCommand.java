package com.pigicial.wikirenderer.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.command.subcommands.*;
import com.pigicial.wikirenderer.util.Translate;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;

import java.net.URI;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class WikiRendererCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext access) {
        LiteralArgumentBuilder<FabricClientCommandSource> mainCommand = literal("wikirender").executes(context -> {
            showRootNodeHelp(context);
            return 0;
        });

        registerSubCommand(mainCommand, access, new ReopenSubCommand());
        registerSubCommand(mainCommand, access, new RenderAreaSubCommand());
        registerSubCommand(mainCommand, access, new RenderBlockSubCommand());
        registerSubCommand(mainCommand, access, new RenderEntitySubCommand());
        registerSubCommand(mainCommand, access, new RenderPlayerSubCommand());
        registerSubCommand(mainCommand, access, new RenderItemSubCommand());
        registerSubCommand(mainCommand, access, new RenderItemTooltipSubCommand());
        registerSubCommand(mainCommand, access, new GroupRenderSubCommand());
        registerSubCommand(mainCommand, access, new DebugSubCommand());
        registerSubCommand(mainCommand, access, new SkyBlockSubCommand());

        LiteralCommandNode<FabricClientCommandSource> registeredMainCommand = dispatcher.register(mainCommand);
        dispatcher.register(literal("wr").redirect(registeredMainCommand));
    }

    private static void registerSubCommand(LiteralArgumentBuilder<FabricClientCommandSource> builder, CommandBuildContext access, WikiRendererSubCommand subCommand) {
        builder.then(subCommand.register(literal(subCommand.getName()), access));
    }

    private static void showRootNodeHelp(CommandContext<FabricClientCommandSource> context) {
        FabricClientCommandSource source = context.getSource();

        source.sendFeedback(Translate.prefixed(Translate.make("version", Component.literal(WikiRenderer.VERSION).withStyle(ChatFormatting.DARK_GRAY)).withStyle(ChatFormatting.GRAY)));
        source.sendFeedback(Translate.prefixed(Translate.make("command_hint").withStyle(style -> style
                .withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/Pigicial/WikiRenderer")))
                .applyFormat(ChatFormatting.UNDERLINE)
                .applyFormat(ChatFormatting.GRAY)
        )));
    }

}
