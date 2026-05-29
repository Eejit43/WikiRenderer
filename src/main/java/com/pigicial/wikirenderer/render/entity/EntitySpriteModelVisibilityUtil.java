package com.pigicial.wikirenderer.render.entity;

import com.pigicial.wikirenderer.WikiRenderer;
import com.pigicial.wikirenderer.mixin.access.ModelPartAccessor;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class EntitySpriteModelVisibilityUtil {

    public static void hideNonHeadParts(LivingEntityRenderer<?, ?, ?> livingEntityRenderer, List<Runnable> toggleCallbacks) {
        EntityModel<?> model = livingEntityRenderer.getModel();
        EntitySpriteModelVisibilityUtil.hideNonHeadParts(model, toggleCallbacks);
    }

    public static void hideNonHeadParts(EntityModel<?> model, List<Runnable> toggleCallbacks) {
        ModelPart root = model.root();

        Function<String, @Nullable ModelPart> partLookup = root.createPartLookup();

        String[] partsToFind = {"head_parts" /* Horse */, "head", "center_head" /* Wither */, "body"};
        for (String part : partsToFind) {
            if (partLookup.apply(part) != null) {
                EntitySpriteModelVisibilityUtil.hideNonHeadParts(toggleCallbacks, root, part);
                break;
            }
        }
    }

    public static void hideNonHeadParts(List<Runnable> toggleCallbacks, ModelPart part, String headType) {
        Map<String, ModelPart> childParts = ((ModelPartAccessor) (Object) part).wikirenderer$getChildren();
        childParts.forEach((identifier, modelPart) -> {
            if (!identifier.equals(headType)) {
                boolean previouslySkippedDraw = modelPart.skipDraw;
                modelPart.skipDraw = true;
                toggleCallbacks.add(() -> modelPart.skipDraw = previouslySkippedDraw);
                hideNonHeadParts(toggleCallbacks, modelPart, headType);
            }
        });
    }

    public static void hideOrShowNonHeadParts(EntityModel<?> model, List<Runnable> toggleCallbacks) {
        if (model == null) return;
        if (WikiRenderer.inSpriteEntityDraw) {
            if (toggleCallbacks.isEmpty()) {
                EntitySpriteModelVisibilityUtil.hideNonHeadParts(model, toggleCallbacks);
            }
        } else {
            toggleCallbacks.forEach(Runnable::run);
            toggleCallbacks.clear();
        }
    }
}
