package com.pigicial.wikirenderer.render.skyblock.misc;

import com.pigicial.wikirenderer.render.batch.BatchRenderable;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.screen.RenderScreen;
import com.pigicial.wikirenderer.screen.ScreenSchedulerAndSaver;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.fish.TropicalFish;
import net.minecraft.world.item.DyeColor;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class TropicalFishBatchRender {

    public static void renderFish() {
        List<EntityRenderable> renderables = new ArrayList<>();
        for (DyeColor baseColor : DyeColor.values()) {
            for (DyeColor patternColor : DyeColor.values()) {
                for (TropicalFish.Pattern variant : TropicalFish.Pattern.values()) {
                    CompoundTag tag = new CompoundTag();
                    tag.put("Variant", IntTag.valueOf(new TropicalFish.Variant(variant, baseColor, patternColor).getPackedId()));
                    EntityRenderable fishy = EntityRenderable.fromEntityType(EntityType.TROPICAL_FISH, tag);
                    if (fishy != null) {
                        fishy.getUsedEntity().tick();
                        fishy.setCustomFileName("tropical_fish_" + variant.getSerializedName() + "_" + baseColor.getName() + "_" + patternColor.getName());
                        renderables.add(fishy);
                    }
                }
            }
        }

        ScreenSchedulerAndSaver.schedule(new RenderScreen(BatchRenderable.of("fish", renderables)));
    }
}
