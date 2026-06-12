package com.pigicial.wikirenderer.render.item;

import com.pigicial.wikirenderer.property.DefaultPropertyBundle;
import com.pigicial.wikirenderer.render.DefaultRenderable;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public abstract class ItemBasedRenderable<T extends DefaultPropertyBundle> extends DefaultRenderable<T> {

    private static final Vector3f DIFFUSE_LIGHT_0 = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    private static final Vector3f DIFFUSE_LIGHT_1 = new Vector3f(-0.2F, 1.0F, 0.7F).normalize();

    protected void setupLighting(ItemStackRenderState itemRenderState) {
        setupLighting(itemRenderState.usesBlockLight());
    }

    protected void setupLighting(boolean usesBlockLight) {
        float rotation = (float) Math.toRadians(getProperties().getUsedRotation());
        float slant = (float) Math.toRadians(getProperties().getUsedSlant());
        if (usesBlockLight) {

            // pulled from Lighting's first setup for ITEMS_3D, but with the scaling value of y changed from -1.0f to 1.0f
            // ngl i have absolutely no idea why this works, but it does - it might have something to do with their atlas sheets having an inverted Y value, idk, probably does
            // (for that, see CachedOrthoProjectionMatrixBuffer and how when it's created in GuiRendered, flip y is true)
            // also i changed the numbers here to use Math.toRadians() rather than harder to process numbers
            Matrix4f matrix4f2 = new Matrix4f()
                    .scaling(1.0F, 1.0F, 1.0F) // IMPORTANT: the y is changed from -1.0 to 1.08
                    .rotateYXZ((float) Math.toRadians(62), (float) Math.toRadians(185.5), 0.0F)
                    .rotateYXZ((float) Math.toRadians(-22.5), (float) (Math.toRadians(135)), 0.0F);

            Vector3f light0 = matrix4f2.transformDirection(DIFFUSE_LIGHT_0, new Vector3f()).rotateY(-rotation).normalize();
            Vector3f light1 = matrix4f2.transformDirection(DIFFUSE_LIGHT_1, new Vector3f()).rotateY(-rotation).normalize();

            this.setupLighting(light0, light1);
        } else {
            Matrix4f matrix4f = new Matrix4f().rotationY((float) (-Math.PI / 8)).rotateX((float) (Math.PI * 3.0 / 4.0));
            Vector3f light0 = matrix4f.transformDirection(DIFFUSE_LIGHT_0, new Vector3f());
            Vector3f light1 = matrix4f.transformDirection(DIFFUSE_LIGHT_1, new Vector3f());

            // didn't think 2d item lighting y scaling mattered, but custom models can make it matter
            // https://github.com/skyblock-wiki/WikiRenderer/issues/21
            light0.y = -light0.y;
            light1.y = -light1.y;

            light0.rotateX(-slant).rotateY(-rotation).normalize();
            light1.rotateX(-slant).rotateY(-rotation).normalize();

            this.setupLighting(light0, light1);
        }
    }
}
