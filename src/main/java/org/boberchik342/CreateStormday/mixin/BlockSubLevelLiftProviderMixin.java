package org.boberchik342.CreateStormday.mixin;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockSubLevelLiftProvider.class)
public interface BlockSubLevelLiftProviderMixin {
    @Shadow
    Vector3d LIFT_VELO = new Vector3d();
    @Shadow
    Vector3d LIFT_POS = new Vector3d();

    @Inject(method = "sable$contributeLiftAndDrag", at = @At(value = "INVOKE", target = "dev/ryanhcode/sable/companion/math/Pose3d.transformNormalInverse(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;", shift = At.Shift.BEFORE))
    default void makeVelocityRelative(BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel subLevel, Pose3d localPose, double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity, Vector3d linearImpulse, Vector3d angularImpulse, BlockSubLevelLiftProvider.LiftProviderGroup group, CallbackInfo ci) {
        WindSystem windSystem = WindSystem.get(subLevel.getLevel());
        Vector3d worldPos = new Vector3d();
        subLevel.logicalPose().transformPosition(LIFT_POS, worldPos);
        BlockPos pos = BlockPos.containing(worldPos.x, worldPos.y, worldPos.z);
        LogUtils.getLogger().info(pos.toString());
        Vec3 windVel = windSystem.getWindVelocityAt(subLevel.getLevel(), pos);
        LIFT_VELO.sub(new Vector3d(windVel.x, windVel.y, windVel.z));
//        LIFT_VELO.sub(windSystem.getWindVelocity());

//        LogUtils.getLogger().error("Stack trace:", new Exception());
    }
}
