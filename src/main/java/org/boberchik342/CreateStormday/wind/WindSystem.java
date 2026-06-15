package org.boberchik342.CreateStormday.wind;

import com.google.common.collect.MapMaker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.Config;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WindSystem {
    public static long windComputeTime;
    public WindAirflowProvider windProvider;
    private final List<AirflowProvider> airflowProviders = new ArrayList<>();
    private transient Iterator<Set<BlockPos>> cropSetIterator = null;
    private transient Iterator<BlockPos> posIterator = null;

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

    public final Map<LevelChunk, Set<BlockPos>> crops = new MapMaker().weakKeys().makeMap();

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
                if (system.crops.isEmpty()) {
                    continue;
                }

                if (system.posIterator == null || !system.posIterator.hasNext()) {
                    if (system.cropSetIterator == null || !system.cropSetIterator.hasNext()) {
                        system.cropSetIterator = system.crops.values().iterator();
                    }

                    while (system.cropSetIterator.hasNext()) {
                        Set<BlockPos> nextSet = system.cropSetIterator.next();
                        if (nextSet != null && !nextSet.isEmpty()) {
                            system.posIterator = nextSet.iterator();
                            break;
                        }
                    }
                }

                if (system.posIterator != null && system.posIterator.hasNext()) {
                    int cropsToProcess = 1;
                    for (int i = 0; i < cropsToProcess && system.posIterator.hasNext(); i++) {
                        BlockPos pos = system.posIterator.next();

                        if (!level.isLoaded(pos)) {
                            continue;
                        }

                        BlockState state = level.getBlockState(pos);

                        if (!(state.getBlock() instanceof CropBlock)) {
                            system.posIterator.remove();
                            continue;
                        }

                        if (system.getWind(level, pos.getCenter()).lengthSqr() > 100) {
                            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                            system.posIterator.remove();
                        }
                    }
                }
            }
        }
    }

    public static void onChunkLoad(LevelChunk chunk) {
        var system = WindSystem.get(chunk.getLevel());
        Set<BlockPos> crops = system.crops.computeIfAbsent(chunk, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()));
        crops.clear();

        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();

        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir() || !section.maybeHas(state -> state.getBlock() instanceof CropBlock)) {
                continue;
            }

            int sectionBaseY = chunk.getSectionYFromSectionIndex(i) * 16;

            for (int yOffset = 0; yOffset < 16; yOffset++) {
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockState state = section.getBlockState(x, yOffset, z);
                        if (state.getBlock() instanceof CropBlock) {
                            BlockPos pos = new BlockPos(chunkMinX + x, sectionBaseY + yOffset, chunkMinZ + z);
                            crops.add(pos);
                        }
                    }
                }
            }
        }
    }
}