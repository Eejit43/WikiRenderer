package com.pigicial.wikirenderer.render;

import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public class CameraUtil {

    public static CameraRenderState createRenderState(Renderable<? extends DefaultPropertyBundle> renderable) {
        return createRenderState(renderable.getProperties());
    }

    public static CameraRenderState createRenderState(DefaultPropertyBundle properties) {
        CameraRenderState state = new CameraRenderState();
        state.orientation.rotationYXZ(
                (float) Math.PI - (float) Math.toRadians(properties.getUsedRotation()),
                (float) Math.PI + (float) Math.toRadians(properties.getUsedSlant()),
                (float) Math.PI);
        return state;
    }

    public static Camera getCamera() {
        return Minecraft.getInstance().getEntityRenderDispatcher().camera;
    }

    public static Vec3 getEntityPositionForParticles(Entity entity, float tickDelta) {
        double x = Mth.lerp(tickDelta, entity.xo, entity.getX());
        double y = Mth.lerp(tickDelta, entity.yo, entity.getY());
        double z = Mth.lerp(tickDelta, entity.zo, entity.getZ());
        return new Vec3(x, y, z);
    }
}
