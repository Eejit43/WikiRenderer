package com.pigicial.wikirenderer.render.particle;

import com.pigicial.wikirenderer.mixin.access.ParticleAccessor;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;

import java.util.HashMap;
import java.util.Map;

public class SavedParticleData {
    private final Particle particle;
    private final Map<Integer, ParticleGroupRenderState> savedParticleData = new HashMap<>();
    private Integer animationTimeFinishedAt = null;

    public SavedParticleData(Particle particle) {
        this.particle = particle;
    }

    public Particle getParticle() {
        return particle;
    }

    public void saveRenderData(ParticleGroupRenderState renderState) {
        int tick = ((ParticleAccessor) particle).wikirenderer$getAge();
        savedParticleData.put(tick, renderState);
    }

    public ParticleGroupRenderState getRenderDataAtTick(int tick) {
        return savedParticleData.get(tick);
    }

    public void setAnimationTimeFinishedAt(int animationTimeFinishedAt) {
        this.animationTimeFinishedAt = animationTimeFinishedAt;
    }

    public Integer getAnimationTimeFinishedAt() {
        return animationTimeFinishedAt;
    }
}
