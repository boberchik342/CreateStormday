package org.boberchik342.CreateStormday.wind;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.Config;

import java.util.*;

public class WindSystem {
    public static long windComputeTime;
    public WindAirflowProvider windProvider;
    private final List<AirflowProvider> airflowProviders = new ArrayList<>();

    public static class CacheEntry<T> {
        public CacheEntry(long created, T value) {
            this.created = created;
            this.value = value;
        }
        public long created;
        public T value;
    }
    private static final WeakHashMap<Level, WindSystem> windSystems = new WeakHashMap<>();

    public void addAirflowProvider(AirflowProvider provider) {
        airflowProviders.add(provider);
    }

    public static WindSystem get(Level level) {
        WindSystem system = windSystems.get(level);
        if (system != null) return system;
        return new WindSystem(level);
    }

    public final WeakHashMap<LevelChunk, Set<BlockPos>> crops = new WeakHashMap<>();

    public WindSystem(Level level) {
        windSystems.put(level, this);
        windProvider = level.isClientSide ? new ClientWindAirflowProvider() : new ServerWindAirflowProvider(level);
    }

    public Vec3 getWind(Level level, Vec3 pos) {
        Vec3 wind = windProvider.getWind(level, pos);
        for (var ap : airflowProviders) {
            wind = wind.add(ap.getWind(level, pos));
        }
        return wind;
    }

    private double getMaxWind() {
        double maxWind = windProvider.getMaxWindSpeed();
        for (AirflowProvider ap : airflowProviders) {
            maxWind += ap.getMaxWindSpeed();
            if (Double.isInfinite(maxWind)) return maxWind;
        }
        return maxWind;
    }

    public static void tickWind(Iterable<ServerLevel> levels) {
        for (var level : levels) {
            WindSystem system = WindSystem.get(level);
            if (system.windProvider instanceof ServerWindAirflowProvider serverWind) {
                serverWind.tick(level);
            }
            if (system.getMaxWind() > 10 && Config.windBreaksCrops) {
                List<BlockPos> snapshot = new ArrayList<>();

                for (Set<BlockPos> set : system.crops.values()) {
                    snapshot.addAll(set);
                }
                for (var pos : snapshot) {
                    if (system.getWind(level, pos.getCenter()).length() <= 10) continue;
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }

    public static void onChunkLoad(LevelChunk chunk) {
        var system = WindSystem.get(chunk.getLevel());
        Set<BlockPos> crops = system.crops.computeIfAbsent(chunk, k -> new HashSet<>());

        int minY = chunk.getMinBuildHeight();
        int maxY = chunk.getMaxBuildHeight();

        ChunkPos cp = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y < maxY; y++) {

                    BlockPos pos = new BlockPos(
                            cp.getMinBlockX() + x,
                            y,
                            cp.getMinBlockZ() + z
                    );

                    BlockState state = chunk.getBlockState(pos);

                    if (state.getBlock() instanceof CropBlock) {
                        crops.add(pos);
                    }
                }
            }
        }
    }
}
