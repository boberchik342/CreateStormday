package org.boberchik342.CreateStormday.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.boberchik342.CreateStormday.Config;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.joml.Matrix4f;
import org.joml.Vector3f;

@EventBusSubscriber(modid = CreateStormday.MODID)
public class WindDebugger {
    private static boolean windDebug = false;
    private static long windTime = 0;
    private static double smoothed = 0;

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (!windDebug || event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;
        renderSamples(level, event.getCamera().getPosition(), event.getPoseStack());
    }

    private static void renderSamples(ClientLevel level, Vec3 cam, PoseStack poseStack) {
        WindSystem wind = WindSystem.get(level);

        VertexConsumer vc = Minecraft.getInstance()
                .renderBuffers()
                .bufferSource()
                .getBuffer(RenderType.translucent());

        poseStack.pushPose();

        int step = Config.windSampleInterval;

        BlockPos origin = new BlockPos(
                Mth.floor(cam.x / step) * step,
                Mth.floor(cam.y / step) * step,
                Mth.floor(cam.z / step) * step
        );

        for (int x = -8; x <= 8; x++) {
            for (int y = -8; y <= 8; y++) {
                for (int z = -8; z <= 8; z++) {

                    BlockPos pos = origin.offset(x * step, y * step, z * step);

                    double e = wind.getDirectWindExposure(level, pos).value ? 1 : 0;

                    float r = (float) e;
                    float g = 0.2f;
                    float b = 1.0f - r;

                    drawPoint(vc, poseStack, pos, cam, r, g, b);
                }
            }
        }
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch(RenderType.translucent());
        poseStack.popPose();
    }

    private static void drawPoint(VertexConsumer vc, PoseStack poseStack,
                                  BlockPos pos, Vec3 cam,
                                  float r, float g, float b) {

        Matrix4f m = poseStack.last().pose();

        float size = 0.1f;

        Vec3 center = new Vec3(
                pos.getX() - cam.x + 0.5,
                pos.getY() - cam.y + 0.5,
                pos.getZ() - cam.z + 0.5
        );

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();

        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f up = new Vector3f(0, 1, 0);
        Vector3f right = new Vector3f();

        look.cross(up, right).normalize();   // right = look × up
        up = new Vector3f();
        right.cross(look, up).normalize();   // up = right × look

        Vec3 rVec = new Vec3(right).scale(size);
        Vec3 uVec = new Vec3(up).scale(size);

        int light = 0xF000F0;

        // 4 corners of billboard
        Vec3 p1 = center.subtract(rVec).subtract(uVec);
        Vec3 p4 = center.subtract(rVec).add(uVec);
        Vec3 p3 = center.add(rVec).add(uVec);
        Vec3 p2 = center.add(rVec).subtract(uVec);



        vc.addVertex(m, (float)p1.x, (float)p1.y, (float)p1.z)
                .setColor(r, g, b, 1).setUv(0, 0).setLight(light).setNormal(poseStack.last(), look.x, look.y, look.z);

        vc.addVertex(m, (float)p2.x, (float)p2.y, (float)p2.z)
                .setColor(r, g, b, 1).setUv(0, 0).setLight(light).setNormal(poseStack.last(), look.x, look.y, look.z);

        vc.addVertex(m, (float)p3.x, (float)p3.y, (float)p3.z)
                .setColor(r, g, b, 1).setUv(0, 0).setLight(light).setNormal(poseStack.last(), look.x, look.y, look.z);

        vc.addVertex(m, (float)p4.x, (float)p4.y, (float)p4.z)
                .setColor(r, g, b, 1).setUv(0, 0).setLight(light).setNormal(poseStack.last(), look.x, look.y, look.z);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().isPaused()) return;
        windTime = WindSystem.windComputeTime;
        WindSystem.windComputeTime = 0;
        smoothed = smoothed * 0.95 + windTime * 0.05;
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        float strength = WindSystem.get(Minecraft.getInstance().level).getWind().x;
        event.getGuiGraphics().setColor(1, 1, 1, 1);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, String.valueOf(strength), 20, 20, 0xFFFFFFFF);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, String.valueOf(windTime), 20, 60, 0xFFFFFFFF);
        event.getGuiGraphics().drawString(Minecraft.getInstance().font, String.valueOf(smoothed), 20, 100, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("wind_debug")
                        .executes(ctx -> {
                            windDebug ^= true;
                            return 1;
                        })
        );
    }
}
