package com.flansmod.warforge.client.render;

import org.joml.Matrix4f;

@FunctionalInterface
public interface BorderVertexSink {
    void vertex(Matrix4f matrix, float x, float y, float z, int argb, float u, float v);
}
