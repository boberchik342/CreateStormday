package org.boberchik342.CreateStormday.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

public class WindDebugRenderer implements BlockEntityRenderer<WindDebugBlockEntity> {

    public WindDebugRenderer(BlockEntityRendererProvider.Context ignoredCtx) {}

    @Override
    public void render(
            WindDebugBlockEntity be,
            float partialTicks,
            @NotNull PoseStack poseStack,
            @NotNull MultiBufferSource buffer,
            int light,
            int overlay
    ) {
        Level level = be.getLevel();
        if (level == null) return;

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        poseStack.pushPose();

        // draw cube
        drawCube(vc, poseStack, be, level);

        poseStack.popPose();
    }

    private void drawCube(VertexConsumer vc, PoseStack pose, WindDebugBlockEntity be, Level level) {
        Matrix4f m = pose.last().pose();

        float min = 0f;
        float max = 1f;

        for (Direction dir : Direction.values()) {
            double exposure = be.getExposure(level, dir);

            float r = (float) exposure;
            float g = 0.2f;
            float b = 1.0f - r;

            switch (dir) {
                case NORTH -> // Z-
                        quad(vc, m, r, g, b, dir.getNormal(),
                                min, min, min,
                                max, min, min,
                                max, max, min,
                                min, max, min
                        );
                case SOUTH -> // Z+
                        quad(vc, m, r, g, b, dir.getNormal(),
                                min, min, max,
                                min, max, max,
                                max, max, max,
                                max, min, max
                        );
                case WEST -> // X-
                        quad(vc, m, r, g, b, dir.getNormal(),
                                min, min, min,
                                min, max, min,
                                min, max, max,
                                min, min, max
                        );
                case EAST -> // X+
                        quad(vc, m, r, g, b, dir.getNormal(),
                                max, min, min,
                                max, min, max,
                                max, max, max,
                                max, max, min
                        );
                case DOWN -> // Y-
                        quad(vc, m, r, g, b, dir.getNormal(),
                                min, min, min,
                                min, min, max,
                                max, min, max,
                                max, min, min
                        );
                case UP -> // Y+
                        quad(vc, m, r, g, b, dir.getNormal(),
                                min, max, min,
                                max, max, min,
                                max, max, max,
                                min, max, max
                        );
            }
        }
    }

    private void quad(VertexConsumer vc, Matrix4f m, float r, float g, float b, Vec3i n,
                      float x1, float y1, float z1,
                      float x2, float y2, float z2,
                      float x3, float y3, float z3,
                      float x4, float y4, float z4) {

        vc.addVertex(m, x1, y1, z1).setColor(r, g, b, 1).setNormal(n.getX(), n.getY(), n.getZ()).setUv(0, 0).setLight(0xF000F0);
        vc.addVertex(m, x4, y4, z4).setColor(r, g, b, 1).setNormal(n.getX(), n.getY(), n.getZ()).setUv(0, 0).setLight(0xF000F0);
        vc.addVertex(m, x3, y3, z3).setColor(r, g, b, 1).setNormal(n.getX(), n.getY(), n.getZ()).setUv(0, 0).setLight(0xF000F0);
        vc.addVertex(m, x2, y2, z2).setColor(r, g, b, 1).setNormal(n.getX(), n.getY(), n.getZ()).setUv(0, 0).setLight(0xF000F0);

    }
}
