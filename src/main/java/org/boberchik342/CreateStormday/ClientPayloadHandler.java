package org.boberchik342.CreateStormday;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler {
    public static void handleWindPacket(final WindPacket packet, final IPayloadContext context) {
        WindSystem windSystem = WindSystem.get(Minecraft.getInstance().level);
        if (windSystem instanceof ClientWindSystem clientWindSystem) {
            clientWindSystem.handlePacket(packet);
        }
    }
}