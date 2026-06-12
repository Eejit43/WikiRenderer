package com.pigicial.wikirenderer.command.subcommands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.GlobalProperties;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.render.entity.player.ProfileFetchMode;
import com.pigicial.wikirenderer.render.entity.player.RenderablePlayerEntity;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import com.pigicial.wikirenderer.textures.PlayerTextureUtils;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public class RenderPlayerSubCommand extends WikiRendererSubCommand {
    @Override
    public String getName() {
        return "player";
    }

    @Override
    public LiteralArgumentBuilder<FabricClientCommandSource> register(LiteralArgumentBuilder<FabricClientCommandSource> source, CommandBuildContext access) {
        return source.executes(_ -> {
                    this.renderSelf();
                    return 0;
                })
                .then(literal("self").executes(_ -> {
                    this.renderSelf();
                    return 0;
                }))
                .then(literal("name")
                        .then(argument("name", StringArgumentType.string())
                                .executes(c -> {
                                    this.renderPlayerByName(c, false);
                                    return 0;
                                })
                                .then(argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(c -> {
                                            this.renderPlayerByName(c, true);
                                            return 0;
                                        }))))
                .then(literal("texture")
                        .then(argument("texture", StringArgumentType.string())
                                .executes(c -> {
                                    this.renderPlayerFromTexture(c, false);
                                    return 0;
                                })
                                .then(argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(c -> {
                                            this.renderPlayerFromTexture(c, true);
                                            return 0;
                                        }))))
                .then(literal("uuid")
                        .then(argument("uuid", UuidArgument.uuid())
                                .executes(c -> {
                                    this.renderPlayerByUuid(c, false);
                                    return 0;
                                })
                                .then(argument("nbt", CompoundTagArgument.compoundTag())
                                        .executes(c -> {
                                            this.renderPlayerByUuid(c, true);
                                            return 0;
                                        }))));
    }

    private void renderSelf() {
        if (GlobalProperties.get().sbFrameRenderingKeybindOverrides.get()) {
            RenderEntitySubCommand.tryToRenderFrameDataInstead(Minecraft.getInstance().player, null);
            return;
        }

        LocalPlayer clientPlayer = Minecraft.getInstance().player;
        ScreenSchedulerAndSaver.schedule(new RenderScreen(EntityRenderable.fromEntity(clientPlayer)));
    }

    private void renderPlayerByName(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        String name = StringArgumentType.getString(context, "name");
        GameProfile gameProfile = new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name);

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.NAME);
        this.applyNBTIfPossible(player, context, useNbt);

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));
    }

    private void renderPlayerByUuid(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        UUID id = context.getArgument("uuid", UUID.class);
        GameProfile gameProfile = new GameProfile(id, id.toString());

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.UUID);
        this.applyNBTIfPossible(player, context, useNbt);

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));
    }

    private void renderPlayerFromTexture(CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        String texture = StringArgumentType.getString(context, "texture");
        GameProfile gameProfile = PlayerTextureUtils.createTexturedGameProfileFromID(texture);

        RenderablePlayerEntity player = new RenderablePlayerEntity(gameProfile, ProfileFetchMode.TEXTURE);
        this.applyNBTIfPossible(player, context, useNbt);

        ScreenSchedulerAndSaver.schedule(new RenderScreen(
                new EntityRenderable(null, player)
        ));
    }

    private void applyNBTIfPossible(RenderablePlayerEntity player, CommandContext<FabricClientCommandSource> context, boolean useNbt) {
        if (useNbt) {
            CompoundTag playerNbt = CompoundTagArgument.getCompoundTag(context, "nbt");
            try (ProblemReporter.ScopedCollector logging = new ProblemReporter.ScopedCollector(player.problemPath(), WikiRenderer.LOGGER)) {
                player.load(TagValueInput.create(logging, player.registryAccess(), playerNbt));
            }
        }
    }
}
