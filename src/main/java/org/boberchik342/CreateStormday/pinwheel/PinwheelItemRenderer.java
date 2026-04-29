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
import org.boberchik342.CreateStormday.CreateStormday;
import org.boberchik342.CreateStormday.wind.WindSystem;

public class PinwheelItemRenderer extends BlockEntityWithoutLevelRenderer {
    public static float rotation = 0;
    public static float speed = 0;
    public PinwheelItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet set) {
        super(dispatcher, set);
    }
    @Override
    public void renderByItem(ItemStack stack,
                             ItemDisplayContext context,
                             PoseStack poseStack,
                             MultiBufferSource buffer,
                             int light,
                             int overlay) {
        ItemRenderer itemRenderer = Minecraft.getInstance().getItemRenderer();
        ModelManager modelManager = Minecraft.getInstance().getModelManager();
        BakedModel blades = modelManager.getModel(
                ModelResourceLocation.standalone(CreateStormday.id("pinwheel_blades"))
        );
        BakedModel handle = modelManager.getModel(
                ModelResourceLocation.standalone(CreateStormday.id("pinwheel_handle"))
        );
        poseStack.pushPose();
        if (Minecraft.getInstance().level != null) {
            speed = (float) (speed * 0.9 + WindSystem.get(Minecraft.getInstance().level).getWind().x * 0.1);
            rotation += (float) ((float) (Minecraft.getInstance().getFrameTimeNs() / 500000000.0 * speed));
        }
        poseStack.translate(0.5, (double) 12/16, 0.5);
        poseStack.mulPose(Axis.ZP.rotation(rotation));
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
