package org.boberchik342.CreateStormday.pinwheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.jetbrains.annotations.NotNull;

import java.util.WeakHashMap;

public class PinwheelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static class State {
        public State() {

        }

        public void next() {
            prevRotation = rotation;
        }

        public float getRotation(float pt) {
            return rotation * pt + prevRotation * (1 - pt);
        }

        public float prevRotation;
        public float speed;
        public float rotation;
    }
    private static final State fpState = new State();
    private static final WeakHashMap<Level, State> states = new WeakHashMap<>();

    public PinwheelItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet set) {
        super(dispatcher, set);
    }

    public static void tick() {
        // ts prob works fr fr no cap i think maybe hopefully probably
        if (Minecraft.getInstance().level == null) return;
        Vec3 windVel = WindSystem.get(Minecraft.getInstance().level).getWind(
                Minecraft.getInstance().level,
                Minecraft.getInstance().player != null ? Minecraft.getInstance().player.getPosition(1) : Vec3.ZERO
        );
        Vec3 playerVel = new Vec3(0, 0, 0);
        if (Minecraft.getInstance().player != null) {
            playerVel = Minecraft.getInstance().player.getDeltaMovement().scale(5);
        }
        Vec3 relativeVel = windVel.subtract(playerVel);
        float targetSpeed = (float) relativeVel.length();
        if (Minecraft.getInstance().player != null) {
            targetSpeed = (float) Minecraft.getInstance().player.getViewVector(1).dot(relativeVel) / 10;
        }
        fpState.next();
        fpState.speed = (float) (fpState.speed * 0.9 + targetSpeed * 0.1);
        fpState.rotation += fpState.speed;
        if (Minecraft.getInstance().level == null) return;
        for (var state : states.values()) {
            state.next();
            state.speed = (float) (state.speed * 0.9 + WindSystem.get(Minecraft.getInstance().level).windProvider.getMaxWindSpeed() * 0.1);
            state.rotation += state.speed;
        }
    }

    @Override
    public void renderByItem(@NotNull ItemStack stack,
                             @NotNull ItemDisplayContext context,
                             @NotNull PoseStack poseStack,
                             @NotNull MultiBufferSource buffer,
                             int light,
                             int overlay) {
        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();

        float rot = 0;
        boolean fp = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        if (!fp) {
            if (Minecraft.getInstance().level != null) {
                rot = states.computeIfAbsent(Minecraft.getInstance().level, k -> new State()).getRotation(pt);
            }
        } else {
            rot = fpState.getRotation(pt);
        }

        BakedModel blades = modelManager.getModel(
                ModelResourceLocation.standalone(CreateStormday.id("pinwheel_blades"))
        );
        BakedModel handle = modelManager.getModel(
                ModelResourceLocation.standalone(CreateStormday.id("pinwheel_handle"))
        );
        poseStack.pushPose();
        poseStack.translate(0.5, (double) 12/16, 0.5);
        poseStack.mulPose(Axis.ZP.rotation(rot));
        poseStack.translate(-0.5, -(double) 12/16, -0.5);


        itemRenderer.renderModelLists(
                blades,
                ItemStack.EMPTY,
                light,
                overlay,
                poseStack,
                buffer.getBuffer(RenderType.solid())
        );

        poseStack.popPose();

        itemRenderer.renderModelLists(
                handle,
                ItemStack.EMPTY,
                light,
                overlay,
                poseStack,
                buffer.getBuffer(RenderType.solid())
        );
    }
}
