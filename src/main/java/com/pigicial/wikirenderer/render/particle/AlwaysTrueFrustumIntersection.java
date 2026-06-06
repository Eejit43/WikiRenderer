package com.pigicial.wikirenderer.render.particle;

import org.joml.FrustumIntersection;

public class AlwaysTrueFrustumIntersection extends FrustumIntersection {

    @Override
    public boolean testAab(float minX, float minY, float minZ,
                           float maxX, float maxY, float maxZ) {
        return true;
    }

    @Override
    public int intersectAab(float minX, float minY, float minZ,
                            float maxX, float maxY, float maxZ) {
        return FrustumIntersection.INSIDE;
    }

    @Override
    public boolean testPoint(float x, float y, float z) {
        return true;
    }
}
