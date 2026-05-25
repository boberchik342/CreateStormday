package org.boberchik342.CreateStormday;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.boberchik342.CreateStormday.wind.ClientWindAirflowProvider;
import org.boberchik342.CreateStormday.wind.WindPacket;
import org.boberchik342.CreateStormday.wind.WindSystem;

public class ClientPayloadHandler {
    public static void handleWindPacket(final WindPacket packet, final IPayloadContext context) {
        WindSystem windSystem = WindSystem.get(Minecraft.getInstance().level);
        if (windSystem.windProvider instanceof ClientWindAirflowProvider clientWindSystem) {
            clientWindSystem.handlePacket(packet);
        }
    }
}