package com.pigicial.wikirenderer.render.particle;

import com.pigicial.wikirenderer.WikiRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.joml.Matrix4f;
import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class AlwaysTrueFrustum extends Frustum {
    public AlwaysTrueFrustum() {
        super(new Matrix4f(), new Matrix4f());
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Unsafe unsafe = (Unsafe) unsafeField.get(null);

            Field field = Frustum.class.getDeclaredField("intersection");
            long offset = unsafe.objectFieldOffset(field);
            unsafe.putObject(this, offset, new AlwaysTrueFrustumIntersection());
            WikiRenderer.LOGGER.info("Successfully replaced frustum particle intersection instance to fix Polytone mod particle overriding issues.");
        } catch (Exception e) {
            WikiRenderer.LOGGER.error("Failed to replace frustum particle intersection instance. If you have the Polytone mod installed, particles may not render properly.", e);
        }
    }

    @Override
    public boolean pointInFrustum(double d, double e, double f) {
        return true;
    }
}
