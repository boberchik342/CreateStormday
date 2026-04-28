package org.boberchik342.CreateStormday.mixin;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.Pose3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.boberchik342.CreateStormday.wind.WindSystem;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockSubLevelLiftProvider.class)
public interface BlockSubLevelLiftProviderMixin {
    @Shadow
    Vector3d LIFT_VELO = null;

    @Inject(method = "sable$contributeLiftAndDrag", at = @At(value = "INVOKE", target = "dev/ryanhcode/sable/companion/math/Pose3d.transformNormalInverse(Lorg/joml/Vector3d;)Lorg/joml/Vector3d;", shift = At.Shift.BEFORE))
    default void makeVelocityRelative(BlockSubLevelLiftProvider.LiftProviderContext ctx, ServerSubLevel subLevel, Pose3d localPose, double timeStep, Vector3dc linearVelocity, Vector3dc angularVelocity, Vector3d linearImpulse, Vector3d angularImpulse, BlockSubLevelLiftProvider.LiftProviderGroup group, CallbackInfo ci) {
        LIFT_VELO.sub(WindSystem.get(subLevel.getLevel()).getWindVelocity());
//        LogUtils.getLogger().error("Stack trace:", new Exception());
    }
}
