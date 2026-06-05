package com.pigicial.wikirenderer.render.skyblock.misc;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.pigicial.wikirenderer.render.batch.BatchRenderable;
import com.pigicial.wikirenderer.render.item.ItemRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class SkyBlockItemsBatchRender {

    public static void renderSBItems() {
        getSBItems().whenComplete((renderables, throwable) -> {
            if (throwable != null) {
                return;
            }
            BatchRenderable<?> renderable = BatchRenderable.of("skyblock_items", renderables);
            Minecraft.getInstance().executeBlocking(() -> ScreenSchedulerAndSaver.schedule(new RenderScreen(renderable)));
        });
    }

    public static CompletableFuture<List<ItemRenderable>> getSBItems() {
        return CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<ItemRenderable>> futures = new ArrayList<>();

            try (HttpClient client = HttpClient.newHttpClient()) {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://api.hypixel.net/v2/resources/skyblock/items"))
                        .header("Accept", "application/json")
                        .build();

                HttpResponse<String> response =
                        client.send(request, HttpResponse.BodyHandlers.ofString());

                JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
                JsonArray items = root.getAsJsonArray("items");

                for (JsonElement element : items) {
                    JsonObject item = element.getAsJsonObject();
                    if (!item.has("skin")) continue;

                    String name = item.get("name").getAsString();
                    JsonObject skin = item.getAsJsonObject("skin");
                    String value = skin.get("value").getAsString();
                    String signature = skin.has("signature") ? skin.get("signature").getAsString() : null;

                    UUID profileId = UUID.nameUUIDFromBytes(value.getBytes());

                    Multimap<String, Property> profileDataMap = ArrayListMultimap.create();
                    profileDataMap.put("textures", new Property("textures", value, signature));
                    GameProfile gameProfile = new GameProfile(profileId, name, new PropertyMap(profileDataMap));

                    ResolvableProfile profile = ResolvableProfile.createResolved(gameProfile);

                    CompletableFuture<ItemRenderable> future = Minecraft.getInstance().getSkinManager()
                            .get(gameProfile)
                            .thenApply(s -> {
                                ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
                                stack.set(DataComponents.PROFILE, profile);
                                stack.set(DataComponents.ITEM_NAME, Component.literal(name));
                                stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                                return new ItemRenderable(stack);
                            });

                    futures.add(future);
                }

            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream().map(CompletableFuture::join).toList())
                    .join();
        });
    }

}
