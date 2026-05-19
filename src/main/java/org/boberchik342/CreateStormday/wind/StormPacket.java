package org.boberchik342.CreateStormday.wind;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.boberchik342.CreateStormday.CreateStormday;
import org.jetbrains.annotations.NotNull;

public record StormPacket (boolean enabled) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StormPacket> TYPE = new CustomPacketPayload.Type<>(CreateStormday.id("storm"));
    public static final StreamCodec<ByteBuf, StormPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            StormPacket::enabled,
            StormPacket::new
    );
    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
