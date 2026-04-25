package org.boberchik342.CreateStormday;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record WindPacket(float strength, float direction) implements CustomPacketPayload {
    public static final Type<WindPacket> TYPE = new Type<WindPacket>(CreateStormday.id("wind"));
    public static final StreamCodec<ByteBuf, WindPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            WindPacket::strength,
            ByteBufCodecs.FLOAT,
            WindPacket::direction,
            WindPacket::new
    );
    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
