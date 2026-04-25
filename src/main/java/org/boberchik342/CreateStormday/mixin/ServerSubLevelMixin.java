package org.boberchik342.CreateStormday.mixin;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ServerSubLevel.class)
public abstract class ServerSubLevelMixin {
    @Unique
    public Vector3d createStormday$lastWakeUpForce = new Vector3d();
    @Unique
    public Vector3d createStormday$lastWakeUpTorque = new Vector3d();

    @Redirect(method = "prePhysicsTick", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/api/physics/handle/RigidBodyHandle;applyLinearAndAngularImpulse(Lorg/joml/Vector3dc;Lorg/joml/Vector3dc;Z)V"))
    void wakeUpper(RigidBodyHandle instance, Vector3dc impulse, Vector3dc torque, boolean wakeUp) {
        boolean forceChanged = impulse.distanceSquared(createStormday$lastWakeUpForce) > 1e-5;
        boolean torqueChanged = torque.distanceSquared(createStormday$lastWakeUpTorque) > 1e-5;
        wakeUp |= forceChanged || torqueChanged;
        if (wakeUp) {
            createStormday$lastWakeUpForce.set(impulse);
            createStormday$lastWakeUpTorque.set(torque);
            LogUtils.getLogger().info("Woken Up Object");
        }
        instance.applyLinearAndAngularImpulse(impulse, torque, wakeUp);
    }
}
