package com.pigicial.wikirenderer.property.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.property.SerializablePropertyBundle;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class WikiRendererConfigs {
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new PropertyBundleAdapterFactory())
            .setPrettyPrinting()
            .create();

    public static void save(SerializablePropertyBundle bundle) {
        try {
            Path path = getConfigPath(bundle.getConfigFileName() + ".json");
            String json = GSON.toJson(bundle);
            Files.writeString(path, json);
        } catch (Exception e) {
            WikiRenderer.LOGGER.error("Failed to save {}.json config file :(", bundle.getConfigFileName(), e);
        }
    }

    public static <T extends SerializablePropertyBundle> T loadOrDefault(T defaultInstance) {
        try {
            Path path = getConfigPath(defaultInstance.getConfigFileName() + ".json");
            if (!Files.exists(path)) {
                WikiRenderer.LOGGER.info("Couldn't find {}.json config (either not generated yet or was deleted), using and saving default", defaultInstance.getConfigFileName());
                save(defaultInstance);
                return defaultInstance;
            }

            // noinspection unchecked
            return (T) GSON.fromJson(Files.newBufferedReader(path), defaultInstance.getClass());
        } catch (Exception e) {
            WikiRenderer.LOGGER.error("Failed to load {}.json config file, using default as backup (maybe it was incorrectly modified with?)", defaultInstance.getConfigFileName(), e);
            return defaultInstance;
        }
    }

    private static Path getConfigPath(String fileName) {
        Path path = FabricLoader.getInstance().getGameDir().resolve("config").resolve("wikirenderer").resolve(fileName);
        File directoryFile = path.toFile().getParentFile();
        if (directoryFile.mkdirs()) {
            WikiRenderer.LOGGER.info("Made config directory {}", directoryFile);
        }
        return path;
    }
}
