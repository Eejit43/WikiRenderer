package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.mojang.authlib.GameProfile;
import com.pigicial.wikirenderer.render.entity.EntityPropertyBundle;
import com.pigicial.wikirenderer.render.entity.EntityRenderable;
import com.pigicial.wikirenderer.render.entity.player.ProfileFetchMode;
import com.pigicial.wikirenderer.render.entity.player.RenderablePlayerEntity;
import io.wispforest.owo.ui.component.ItemComponent;
import io.wispforest.owo.ui.component.UIComponents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Supplier;

public class DyedArmorFrameBasedRenderable extends FrameBasedRenderable<DyedArmorColorData, EntityRenderable, EntityPropertyBundle> {
    public DyedArmorFrameBasedRenderable(UUID entityID, Supplier<TreeMap<Integer, DyedArmorColorData>> dataSourceSupplier) {
        super(entityID, dataSourceSupplier, EntityPropertyBundle.INSTANCE);
    }

    @Override
    protected EntityRenderable createRenderableFromData(DyedArmorColorData data) {
        GameProfile profile = new GameProfile(new UUID(0, 0), "Steve");
        RenderablePlayerEntity entity = new RenderablePlayerEntity(profile, ProfileFetchMode.UUID);

        entity.setItemSlot(EquipmentSlot.HEAD, this.createItem(Items.LEATHER_HELMET, data.helmetColor()));
        entity.setItemSlot(EquipmentSlot.CHEST, this.createItem(Items.LEATHER_CHESTPLATE, data.chestplateColor()));
        entity.setItemSlot(EquipmentSlot.LEGS, this.createItem(Items.LEATHER_LEGGINGS, data.leggingsColor()));
        entity.setItemSlot(EquipmentSlot.FEET, this.createItem(Items.LEATHER_BOOTS, data.bootsColor()));
        return new EntityRenderable(null, entity);
    }

    @Override
    public ItemComponent createItemComponentForPreview(FrameData<DyedArmorColorData, EntityRenderable, EntityPropertyBundle> frameData) {
        return UIComponents.item(this.createItem(Items.LEATHER_HELMET, frameData.sourceData().helmetColor()));
    }

    @Override
    protected boolean sourceDataMatches(DyedArmorColorData data1, DyedArmorColorData data2) {
        return Objects.equals(data1, data2);
    }

    @Override
    protected DyedArmorColorData getMatchingFirstMarkedData() {
        return null; // no way to mark first for this
    }

    @Override
    @NotNull
    public InterpolatedTimings getTimings(List<FrameData<DyedArmorColorData, EntityRenderable, EntityPropertyBundle>> currentDataSet, int framesCount) {
        return SkyBlockTimingDataCacher.INSTANCE.getColorTimings(currentDataSet, framesCount);
    }

    @Override
    protected List<String> generateWikiTextFile(List<FrameData<DyedArmorColorData, EntityRenderable, EntityPropertyBundle>> currentDataSet) {
        List<String> helmetColorDisplays = new ArrayList<>();
        Map<String, List<String>> pieceMappings = new HashMap<>();

        for (FrameData<DyedArmorColorData, EntityRenderable, EntityPropertyBundle> frame : currentDataSet) {
            int helmetColor = frame.sourceData().helmetColor();
            String helmetColorDisplayText = "{{Color Display|" + String.format("%06x", helmetColor & 0xFFFFFF) + "}}";
            if (helmetColorDisplays.isEmpty() || !helmetColorDisplays.getLast().equals(helmetColorDisplayText)) {
                helmetColorDisplays.add(helmetColorDisplayText);
            }

            addColorEntryIfNecessary(pieceMappings, "head", String.format("%06x", helmetColor & 0xFFFFFF));
            addColorEntryIfNecessary(pieceMappings, "chest", String.format("%06x", frame.sourceData().chestplateColor() & 0xFFFFFF));
            addColorEntryIfNecessary(pieceMappings, "legs", String.format("%06x", frame.sourceData().leggingsColor() & 0xFFFFFF));
            addColorEntryIfNecessary(pieceMappings, "feet", String.format("%06x", frame.sourceData().bootsColor() & 0xFFFFFF));
        }

        return List.of(
                "{{ArmorRender/Set",
                "|headcolor = " + String.join("; ", pieceMappings.getOrDefault("head", List.of())),
                "|chestcolor = " + String.join("; ", pieceMappings.getOrDefault("chest", List.of())),
                "|legscolor = " + String.join("; ", pieceMappings.getOrDefault("legs", List.of())),
                "|feetcolor = " + String.join("; ", pieceMappings.getOrDefault("feet", List.of())),
                "}}",
                String.join(" → ", helmetColorDisplays)
        );
    }

    private void addColorEntryIfNecessary(Map<String, List<String>> map, String key, String text) {
        List<String> list = map.computeIfAbsent(key, _ -> new ArrayList<>());
        if (list.isEmpty() || !Objects.equals(list.getLast(), text)) {
            list.add(text);
        }
    }

    private ItemStack createItem(Item item, int dyeColor) {
        ItemStack itemStack = new ItemStack(item);
        itemStack.set(DataComponents.DYED_COLOR, new DyedItemColor(dyeColor));
        return itemStack;
    }
}

