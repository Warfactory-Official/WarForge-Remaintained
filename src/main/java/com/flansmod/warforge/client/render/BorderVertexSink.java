package com.flansmod.warforge.client.render;

import org.joml.Matrix4f;

/**
 * Receives the vertices of the claim border mesh (format: position, texture, colour; quads). The mesh code writes
 * through this so it stays the same on every version; BorderGpu turns the vertices into the version's GPU calls.
 */
@FunctionalInterface
public interface BorderVertexSink {
    /** One vertex: position (transformed by matrix), colour (ARGB), texture coordinates. */
    void vertex(Matrix4f matrix, float x, float y, float z, int argb, float u, float v);
}
