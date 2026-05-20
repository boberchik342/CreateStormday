package org.boberchik342.CreateStormday.pinwheel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.boberchik342.CreateStormday.CreateStormday;
import org.jetbrains.annotations.NotNull;

public class PinwheelBlockEntityRenderer implements BlockEntityRenderer<PinwheelBlockEntity> {
    public PinwheelBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredCtx) {}

    @Override
    public void render(@NotNull PinwheelBlockEntity pinwheelBlockEntity, float v, @NotNull PoseStack poseStack, @NotNull MultiBufferSource multiBufferSource, int light, int overlay) {
        multiBufferSource.getBuffer(RenderType.solid());
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BakedModel blades = modelManager.getModel(ModelResourceLocation.standalone(CreateStormday.id("pinwheel_blades")));
        BakedModel handle = modelManager.getModel(ModelResourceLocation.standalone(CreateStormday.id("pinwheel_handle")));
        Level level = pinwheelBlockEntity.getLevel();
        BlockState state = pinwheelBlockEntity.getBlockState();
        VertexConsumer vc = multiBufferSource.getBuffer(RenderType.cutout());

        float pt = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true);

        poseStack.pushPose();
        poseStack.translate(0.5, 12f/16f, 0.5);
// global direction rotation (whole pinwheel orientation)
        poseStack.mulPose(Axis.YP.rotation(pinwheelBlockEntity.getDir(pt)));

// ===== BLADES (rotating part) =====
        poseStack.pushPose();
        poseStack.mulPose(Axis.ZP.rotation(pinwheelBlockEntity.getRotation(pt)));
        poseStack.translate(-0.5, -12f/16f, -0.5);

        dispatcher.getModelRenderer().tesselateBlock(
                level, blades, state,
                pinwheelBlockEntity.getBlockPos(),
                poseStack, vc,
                false, level.random,
                state.getSeed(pinwheelBlockEntity.getBlockPos()),
                overlay
        );

        poseStack.popPose();
        poseStack.translate(-0.5, -12f/16f, -0.5);

// ===== HANDLE (static part) =====
        dispatcher.getModelRenderer().tesselateBlock(
                level, handle, state,
                pinwheelBlockEntity.getBlockPos(),
                poseStack, vc,
                false, level.random,
                state.getSeed(pinwheelBlockEntity.getBlockPos()),
                overlay
        );

        poseStack.popPose();
    }
}
