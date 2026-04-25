package org.boberchik342.CreateStormday.mixin;

import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerSubLevel.class)
public class ServerSubLevelMixin {
    public Vector3d lastWakeUpForce = new Vector3d();
    public Vector3d lastWakeUpTorque = new Vector3d();
}
