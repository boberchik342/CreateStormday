package org.boberchik342.CreateStormday;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class ClientWindSystem extends WindSystem{
    public ClientWindSystem(Level level) {
        super(level);
    }

    @Override
    public Vec2 getWind() {
        return new Vec2(strength, direction);
    }

    public void handlePacket(WindPacket packet) {
        strength = packet.strength();
        direction = packet.direction();
    }
}
