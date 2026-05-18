package com.pigicial.wikirenderer.render;

import com.mojang.blaze3d.vertex.CompactVectorArray;
import com.mojang.blaze3d.vertex.VertexSorting;
import it.unimi.dsi.fastutil.ints.IntArrays;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.jspecify.annotations.NonNull;

public class OrthographicSort implements VertexSorting {

    private final float m02;
    private final float m12;
    private final float m22;
    private final float m32;

    public OrthographicSort(Matrix4f projectionMatrix, Matrix4fStack modelViewStack) {
        Matrix4f modelViewProjectionMatrix = new Matrix4f(projectionMatrix).mul(modelViewStack);
        this.m02 = modelViewProjectionMatrix.m02();
        this.m12 = modelViewProjectionMatrix.m12();
        this.m22 = modelViewProjectionMatrix.m22();
        this.m32 = modelViewProjectionMatrix.m32();
    }

    @Override
    public int @NonNull [] sort(@NonNull CompactVectorArray blockPosVector) {
        int size = blockPosVector.size();
        float[] zDepths = new float[size];
        int[] indices = new int[size];

        for (int i = 0; i < size; i++) {
            float x = blockPosVector.getX(i);
            float y = blockPosVector.getY(i);
            // mojang did a dumb and made getZ() return the same as getY(), so this is a workaround
            float z = blockPosVector.get(i, new org.joml.Vector3f()).z;
            zDepths[i] = x * m02 + y * m12 + z * m22 + m32;
            indices[i] = i;
        }

        IntArrays.mergeSort(indices, (a, b) -> Float.compare(zDepths[b], zDepths[a]));
        return indices;
    }

    public float projectDepth(float x, float y, float z) {
        return x * m02 + y * m12 + z * m22 + m32;
    }
}
