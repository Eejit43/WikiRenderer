package com.pigicial.wikirenderer.render.export.animation;

import com.pigicial.wikirenderer.render.Renderable;
import com.pigicial.wikirenderer.util.Translate;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.opengl.*;

import java.util.ArrayList;
import java.util.List;

public class MemoryGuard {

    private final float maximumLoadFactor;
    private int availableRamMB = 0;

    public MemoryGuard(float maximumLoadFactor) {
        this.maximumLoadFactor = maximumLoadFactor;
    }

    public void update() {
        int[] data = new int[4];
        GLCapabilities capabilities = GL.getCapabilities();

        if (capabilities.GL_ATI_meminfo) GL11.glGetIntegerv(ATIMeminfo.GL_TEXTURE_FREE_MEMORY_ATI, data);
        if (capabilities.GL_NVX_gpu_memory_info) GL11.glGetIntegerv(NVXGPUMemoryInfo.GL_GPU_MEMORY_INFO_CURRENT_AVAILABLE_VIDMEM_NVX, data);
        GL11.glGetError();

        this.availableRamMB = (int) ((Runtime.getRuntime().maxMemory() - Runtime.getRuntime().totalMemory() + Runtime.getRuntime().freeMemory()) / 1024 / 1024);
    }

    public List<Component> getStatusTooltip(int memoryMB) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(this.usageText(memoryMB, this.availableRamMB(), this.canFitInRam(memoryMB)));

        if (!canFitInRam(memoryMB)) {
            tooltip.add(Translate.gui("ram_ignore").withStyle(ChatFormatting.GRAY));
        }

        return tooltip;
    }

    private MutableComponent usageText(int usage, int available, boolean fits) {
        if (available == 0) available = 1;

        if (fits) {
            return Translate.gui(
                    "ram",
                    Component.literal(usage + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(available + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(usage * 100 / available + "%").withStyle(ChatFormatting.GRAY)
            );
        } else {
            return Translate.gui(
                    "ram",
                    Component.literal(usage + "").withStyle(ChatFormatting.RED),
                    Component.literal(available + "").withStyle(ChatFormatting.GRAY),
                    Component.literal(usage * 100 / available + "%").withStyle(ChatFormatting.RED)
            );
        }
    }

    public int estimateMemoryMBUsage(Renderable<?> renderable, int frames) {
        return (int) ((renderable.getExportResolution() * renderable.getExportResolution() * 4L * frames) / 1024L / 1024L);
    }

    public int availableRamMB() {
        return this.availableRamMB;
    }

    public boolean canFitInRam(int memoryMB) {
        return memoryMB / (float) this.availableRamMB <= this.maximumLoadFactor;
    }

}