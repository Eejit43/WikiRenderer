package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.render.area.AreaRenderable;
import com.pigicial.wikirenderer.render.area.AreaSelectionHelper;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.util.Translate;
import fi.dy.masa.litematica.data.DataManager;
import fi.dy.masa.litematica.selection.AreaSelection;
import fi.dy.masa.litematica.selection.Box;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RenderAreaSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "area";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        LiteralArgumentBuilder<FabricClientCommandSource> baseCommand = source.executes(context -> {
            this.renderAreaSelection(context);
            return 0;
        });
        baseCommand.then(literal("island")
                        .then(argument("chunk_cube_size", IntegerArgumentType.integer(4, 64))
                                .then(argument("distance_limit", IntegerArgumentType.integer(1, 2000))
                                        .executes(context -> {
                                            this.renderSurroundingConnectedMiniChunks(context);
                                            return 0;
                                        }))))
                .then(literal("pos")
                        .then(argument("start", BlockPosArgument.blockPos())
                                .then(argument("end", BlockPosArgument.blockPos())
                                        .executes(context -> {
                                            this.renderAreaWithArguments(context);
                                            return 0;
                                        }))));

        if (FabricLoader.getInstance().isModLoaded("litematica")) {
            baseCommand.then(literal("litematica").executes(context -> {
                this.renderLitematicaSelection(context);
                return 0;
            }));
        }

        return baseCommand;
    }

    private void renderAreaSelection(CommandContext<FabricClientCommandSource> context) {
        if (!AreaSelectionHelper.tryOpenScreen()) {
            Translate.commandError(context, "incomplete_selection");
        }
    }

    private void renderSurroundingConnectedMiniChunks(CommandContext<FabricClientCommandSource> context) {
        Integer chunkSize = context.getArgument("chunk_cube_size", Integer.class);
        Integer distanceLimit = context.getArgument("distance_limit", Integer.class);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }

        Translate.commandFeedback(context, "scanning_chunks", Component.literal(String.valueOf(chunkSize)), Component.literal(String.valueOf(chunkSize)), Component.literal(String.valueOf(distanceLimit)));
        AreaRenderable area = AreaRenderable.of(context, player.blockPosition(), chunkSize, distanceLimit);
        if (area == null) {
            return;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(area));
    }

    private void renderAreaWithArguments(CommandContext<FabricClientCommandSource> context) {
        WorldCoordinates startArg = context.getArgument("start", WorldCoordinates.class);
        WorldCoordinates endArg = context.getArgument("end", WorldCoordinates.class);

        BlockPos pos1 = this.getPosFromArgument(startArg, context.getSource());
        BlockPos pos2 = this.getPosFromArgument(endArg, context.getSource());

        ScreenSchedulerAndSaver.schedule(new RenderScreen(AreaRenderable.of(pos1, pos2)));
    }

    private BlockPos getPosFromArgument(WorldCoordinates argument, FabricClientCommandSource source) {
        Vec3 pos = source.getPlayer().trackingPosition();
        return BlockPos.containing(argument.x().get(pos.x), argument.y().get(pos.y), argument.z().get(pos.z));
    }

    private void renderLitematicaSelection(CommandContext<FabricClientCommandSource> context) {
        AreaSelection selection = DataManager.getSelectionManager().getCurrentSelection();
        if (selection == null) {
            Translate.commandError(context, "no_litematica_selection");
            return;
        }

        Box box = selection.getSelectedSubRegionBox();
        if (box == null) {
            Translate.commandError(context, "invalid_litematica_selection");
            return;
        }

        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();
        if (pos1 == null || pos2 == null) {
            Translate.commandError(context, "invalid_litematica_selection");
            return;
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(AreaRenderable.of(pos1, pos2)));
    }
}
