package com.pigicial.wikirenderer.render.export;

import com.pigicial.wikirenderer.property.GlobalProperties;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public record ExportPathSpec(String rootOffset, String filename, boolean ignoreSaveIntoRoot) {

    public static ExportPathSpec of(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, false);
    }

    public static ExportPathSpec forced(String rootOffset, String filename) {
        return new ExportPathSpec(rootOffset, filename, true);
    }

    public static ExportPathSpec ofIdentified(Identifier id, String type) {
        return new ExportPathSpec(id.getNamespace() + "/" + type, id.getPath(), false);
    }

    // -----

    public Path resolveOffset() {
        return exportRoot().resolve(this.effectiveOffset()).resolve(resolvedFilenamePath()).getParent();
    }

    private Path resolvedFilenamePath() {
        Path result = Path.of("");
        String[] parts = this.filename.split("/");
        for (String part : parts) {
            result = result.resolve(part);
        }
        return result;
    }

    public File resolveFile(String extension) {
        Path resolved = exportRoot().resolve(this.effectiveOffset()).resolve(resolvedFilenamePath());
        String actualFileName = resolved.getFileName().toString();
        Path parent = resolved.getParent();
        return FileIO.next(parent.resolve(actualFileName + "." + extension)).toFile();
    }

    public ExportPathSpec relocate(String newOffset) {
        return new ExportPathSpec(newOffset, this.filename, this.ignoreSaveIntoRoot);
    }

    public ExportPathSpec differentFileName(@Nullable String newFileName) {
        if (newFileName == null || newFileName.isEmpty()) {
            return this;
        }
        return new ExportPathSpec(this.rootOffset, newFileName, this.ignoreSaveIntoRoot);
    }

    private String effectiveOffset() {
        return rootOffset.isEmpty() || (!this.ignoreSaveIntoRoot && GlobalProperties.get().saveIntoRoot.get()) ? "./" : rootOffset + "/";
    }

    // -----

    public static Path exportRoot() {
        return FabricLoader.getInstance().getGameDir().resolve("renders");
    }
}
