package org.boberchik342.CreateStormday.wind;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.Config;
import org.joml.Vector3d;

public class WindGraphics {
    public static void spawnParticles(Minecraft mc) {
        ClientLevel level = mc.level;
        if (mc.level == null || mc.player == null) return;

        Vector3d windVelocity = WindSystem.get(level).windProvider.getWindVelocity();
        Vec2 wind = new Vec2((float) windVelocity.x, (float) windVelocity.z);

        Vec3 playerPos = mc.player.position();

        if (Config.enableWindParticles) {
            spawnWindParticles(wind, playerPos, level);
        }

        if (Config.enableGroundParticles) {
            spawnGroundParticles(wind, playerPos, level);
        }
    }

    private static void spawnWindParticles(Vec2 wind, Vec3 playerPos, Level level) {
        int volume = Config.windParticleSpawnAreaSize * Config.windParticleSpawnAreaSize * Config.windParticleSpawnAreaSize;
        for (int i = 0; i < Math.min(Math.pow(Math.max(wind.length() - 0.5, 0), 1.5) * volume / 2000, 1000); i++) {
            double x = playerPos.x + (level.random.nextDouble() - 0.5) * 200;
            double y = playerPos.y + (level.random.nextDouble() - 0.5) * 200;
            double z = playerPos.z + (level.random.nextDouble() - 0.5) * 200;

            double jitterX = (level.random.nextDouble() - 0.5) * 0.2;
            double jitterZ = (level.random.nextDouble() - 0.5) * 0.2;

            level.addParticle(
                    ParticleTypes.CLOUD,
                    x, y, z,
                    wind.x + jitterX,
                    0,
                    wind.y + jitterZ
            );
        }
    }

    private static void spawnGroundParticles(Vec2 wind, Vec3 playerPos, Level level) {
        int volume = (int) Math.pow(Config.groundParticleSpawnAreaSize * 2 + 1, 3);
        for (int i = 0; i < volume * wind.length() / 64; i++) {
            BlockPos pos = new BlockPos(
                    (int) playerPos.x + level.random.nextInt(1 + 2 * Config.groundParticleSpawnAreaSize) - Config.groundParticleSpawnAreaSize,
                    (int) playerPos.y + level.random.nextInt(1 + 2 * Config.groundParticleSpawnAreaSize) - Config.groundParticleSpawnAreaSize,
                    (int) playerPos.z + level.random.nextInt(1 + 2 * Config.groundParticleSpawnAreaSize) - Config.groundParticleSpawnAreaSize
            );
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) continue;
            boolean windCalculated = false;
            for (var dir : Direction.values()) {
                BlockPos sidePos = pos.offset(dir.getNormal());
                BlockState s = level.getBlockState(sidePos);
                if (s.isAir()) continue;
                if (!windCalculated) {
                    if (WindSystem.get(level).getWind(level, pos.getCenter()).length() > 5) {
                        windCalculated = true;
                    } else {
                        break;
                    }
                }
                Vec3 offset = new Vec3(
                        level.random.nextDouble(),
                        level.random.nextDouble(),
                        level.random.nextDouble()
                );
                Vec3i n = dir.getNormal().multiply(-1);
                Vec3 a = new Vec3(1 - Math.abs(dir.getNormal().getX()), 1 - Math.abs(dir.getNormal().getY()), 1 - Math.abs(dir.getNormal().getZ()));
                Vec3 b = new Vec3(Math.max(n.getX(), 0), Math.max(n.getY(), 0), Math.max(n.getZ(), 0));
                offset = offset.multiply(a).add(b);
                level.addParticle(
                        new BlockParticleOption(ParticleTypes.BLOCK, s),
                        sidePos.getX() + offset.x,
                        sidePos.getY() + offset.y,
                        sidePos.getZ() + offset.z,
                        wind.x, 10, wind.y
                );
            }
        }
    }

}
