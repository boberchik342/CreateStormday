package org.boberchik342.CreateStormday.wind;

import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;

public class ClientWindAirflowProvider extends WindAirflowProvider {
    public void handlePacket(WindPacket packet) {
        strength = packet.strength();
        direction = packet.direction();
    }
}
