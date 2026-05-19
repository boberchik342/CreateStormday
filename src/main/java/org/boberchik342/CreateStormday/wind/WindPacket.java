package org.boberchik342.CreateStormday.wind;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.boberchik342.CreateStormday.CreateStormday;
import org.jetbrains.annotations.NotNull;

public record WindPacket(float strength, float direction) implements CustomPacketPayload {
    public static final Type<WindPacket> TYPE = new Type<>(CreateStormday.id("wind"));
    public static final StreamCodec<ByteBuf, WindPacket> CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            WindPacket::strength,
            ByteBufCodecs.FLOAT,
            WindPacket::direction,
            WindPacket::new
    );
    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
