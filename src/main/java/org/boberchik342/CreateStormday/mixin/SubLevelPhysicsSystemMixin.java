package org.boberchik342.CreateStormday.mixin;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.PhysicsPipeline;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Iterator;

@Debug(export = true)
@Mixin(SubLevelPhysicsSystem.class)
public class SubLevelPhysicsSystemMixin {
    @Final
    @Shadow
    private PhysicsPipeline pipeline;

    @Inject(
            method = "tickPipelinePhysics",
            at = @At(
                    value = "INVOKE",
                    target = "dev/ryanhcode/sable/sublevel/ServerSubLevel.applyQueuedForces(Ldev/ryanhcode/sable/sublevel/system/SubLevelPhysicsSystem;Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;D)V",
                    shift = At.Shift.BEFORE
            ),
            locals = LocalCapture.CAPTURE_FAILSOFT
    )

    private void beforeApplyForces(
            ServerSubLevelContainer container, CallbackInfo ci, double substepTimeStep, Iterator var4, ServerSubLevel subLevel
    ) {
//        Vector3d totalForce = new Vector3d();
//        var groups = subLevel.getQueuedForceGroups();
//        if (groups != null) {
////            LogUtils.getLogger().info("Force groups exist");
//        } else {
////            LogUtils.getLogger().info("Force groups don't exist");
//        }
//        if (groups == null) return;
//        for (var group : groups.values()) {
//            totalForce.add(group.getForceTotal().getLocalForce());
//        }
        pipeline.wakeUp(subLevel);
    }
}
