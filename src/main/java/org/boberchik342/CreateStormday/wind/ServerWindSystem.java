package org.boberchik342.CreateStormday.wind;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.synth.ImprovedNoise;
import net.minecraft.world.phys.Vec2;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerWindSystem extends WindSystem {
    private final ImprovedNoise noise;
    private boolean custom = false;

    public ServerWindSystem(Level level) {
        super(level);
        MinecraftServer server = level.getServer();
        long seed = 342342342;
        if (server != null) {
            ServerLevel overworld = server.getLevel(Level.OVERWORLD);
            if (overworld instanceof WorldGenLevel worldGenLevel) {
                seed = worldGenLevel.getSeed();
                LogUtils.getLogger().info("level is not WorldGenLevel");
            }
        } else {
            LogUtils.getLogger().info("server is null");
        }
        noise = new ImprovedNoise(RandomSource.create(seed));
    }

    @Override
    public Vec2 getWind() {
        return new Vec2(strength, direction);
    }

    public void setWind(Level level, float strength, float direction) {
        this.strength = strength;
        this.direction = direction;
        custom = true;
        PacketDistributor.sendToPlayersInDimension((ServerLevel) level, new WindPacket(strength, direction));
    }

    public void resetWind() {
        custom = false;
    }

    public void tick(Level level) {
        if (custom) return;
        double strengthNoise = (noise.noise((double) level.getGameTime() / 500, 0, 0) + 1) / 2;
        strength = (float) (Math.pow(strengthNoise, 4) * 20);
        double directionNoise = noise.noise((double) level.getGameTime() / 500, 100, 100);
        direction = (float) ((directionNoise + 1) * Math.PI);
        PacketDistributor.sendToPlayersInDimension((ServerLevel) level, new WindPacket(strength, direction));
    }
}
