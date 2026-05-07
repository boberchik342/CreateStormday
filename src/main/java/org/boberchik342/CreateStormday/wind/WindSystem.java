package org.boberchik342.CreateStormday.wind;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.ChunkEvent;
import org.boberchik342.CreateStormday.Config;
import org.joml.Vector3d;

import java.util.*;
import java.util.logging.Logger;

public abstract class WindSystem {
    public static class WindEntry<T> {
        public WindEntry(long created, T value) {
            this.created = created;
            this.value = value;
        }
        public long created;
        public T value;
    }
    public static int windComputations = 0;
    private static final WeakHashMap<Level, WindSystem> windSystems = new WeakHashMap<>();
    public static WindSystem get(Level level) {
        WindSystem system = windSystems.get(level);
        if (system != null) return system;
        if (level.isClientSide()) {
            return new ClientWindSystem(level);
        } else {
            return new ServerWindSystem(level);
        }
    }

    public final WeakHashMap<LevelChunk, Set<BlockPos>> crops = new WeakHashMap<>();
    public final WeakHashMap<LevelChunk, Map<BlockPos, WindEntry<Boolean>>> directWindExposureCache = new WeakHashMap<>();
    public final WeakHashMap<LevelChunk, Map<BlockPos, WindEntry<Double>>> interpolatedWindExposureCache = new WeakHashMap<>();


    protected float strength = 0;
    protected float direction = 0;

    public WindSystem(Level level) {
        windSystems.put(level, this);
    }

    public WindEntry<Double> getBlockWindExposure(Level level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry<Double>> directExposure = interpolatedWindExposureCache.get((LevelChunk) level.getChunk(pos));
            if (directExposure != null) {
                WindEntry<Double> exposed = directExposure.get(pos);
                if (exposed != null && level.getGameTime() - exposed.created < 40) {
                    return exposed;
                }
            }
        }
        BlockPos origin = new BlockPos(Math.floorDiv(pos.getX(), Config.windSampleInterval) * Config.windSampleInterval, Math.floorDiv(pos.getY(), Config.windSampleInterval) * Config.windSampleInterval, Math.floorDiv(pos.getZ(), Config.windSampleInterval) * Config.windSampleInterval);
        BlockPos b000 = origin;
        BlockPos b001 = new BlockPos(origin.getX(), origin.getY(), origin.getZ() + Config.windSampleInterval);
        BlockPos b010 = new BlockPos(origin.getX(), origin.getY() + Config.windSampleInterval, origin.getZ());
        BlockPos b011 = new BlockPos(origin.getX(), origin.getY() + Config.windSampleInterval, origin.getZ() + Config.windSampleInterval);
        BlockPos b100 = new BlockPos(origin.getX() + Config.windSampleInterval, origin.getY(), origin.getZ());
        BlockPos b101 = new BlockPos(origin.getX() + Config.windSampleInterval, origin.getY(), origin.getZ() + Config.windSampleInterval);
        BlockPos b110 = new BlockPos(origin.getX() + Config.windSampleInterval, origin.getY() + Config.windSampleInterval, origin.getZ());
        BlockPos b111 = new BlockPos(origin.getX() + Config.windSampleInterval, origin.getY() + Config.windSampleInterval, origin.getZ() + Config.windSampleInterval);
        WindEntry<Boolean> e000 = getDirectWindExposure(level, b000);
        WindEntry<Boolean> e001 = getDirectWindExposure(level, b001);
        WindEntry<Boolean> e010 = getDirectWindExposure(level, b010);
        WindEntry<Boolean> e011 = getDirectWindExposure(level, b011);
        WindEntry<Boolean> e100 = getDirectWindExposure(level, b100);
        WindEntry<Boolean> e101 = getDirectWindExposure(level, b101);
        WindEntry<Boolean> e110 = getDirectWindExposure(level, b110);
        WindEntry<Boolean> e111 = getDirectWindExposure(level, b111);

        long[] times = {
                e000.created,
                e001.created,
                e010.created,
                e011.created,
                e100.created,
                e101.created,
                e110.created,
                e111.created,
        };
        long earliestRaycast = Arrays.stream(times).min().orElse(level.getGameTime());

        int localX = Math.floorMod(pos.getX(), Config.windSampleInterval);
        int localY = Math.floorMod(pos.getY(), Config.windSampleInterval);
        int localZ = Math.floorMod(pos.getZ(), Config.windSampleInterval);
        double tX00 = getT(level, b000, Config.windSampleInterval, new Vec3i(1, 0, 0), localX);
        double tX01 = getT(level, b001, Config.windSampleInterval, new Vec3i(1, 0, 0), localX);
        double tX10 = getT(level, b010, Config.windSampleInterval, new Vec3i(1, 0, 0), localX);
        double tX11 = getT(level, b011, Config.windSampleInterval, new Vec3i(1, 0, 0), localX);
        double tY0 = getT(level, new BlockPos(pos.getX(), b000.getY(), b000.getZ()), Config.windSampleInterval, new Vec3i(0, 1, 0), localY);
        double tY1 = getT(level, new BlockPos(pos.getX(), b001.getY(), b001.getZ()), Config.windSampleInterval, new Vec3i(0, 1, 0), localY);
        double tZ = getT(level, new BlockPos(pos.getX(), pos.getY(), b000.getZ()), Config.windSampleInterval, new Vec3i(0, 0, 1), localZ);
        double e00 = interpolate(e000.value ? 1 : 0, e100.value ? 1 : 0, tX00);
        double e01 = interpolate(e001.value ? 1 : 0, e101.value ? 1 : 0, tX01);
        double e10 = interpolate(e010.value ? 1 : 0, e110.value ? 1 : 0, tX10);
        double e11 = interpolate(e011.value ? 1 : 0, e111.value ? 1 : 0, tX11);
        double e0 = interpolate(e00, e10, tY0);
        double e1 = interpolate(e01, e11, tY1);
        WindEntry<Double> entry = new WindEntry<>(earliestRaycast, interpolate(e0, e1, tZ));
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry<Double>> directExposure = interpolatedWindExposureCache.computeIfAbsent((LevelChunk) level.getChunk(pos), k -> new HashMap<>());
            directExposure.put(pos, entry);
        }
        return entry;
    }

    public abstract Vec2 getWind();

    private static double getT(Level level, BlockPos a, int b, Vec3i dir, int c) {
        int solidsA = 0;
        int solidsB = 0;
        for (int t = 0; t <= c; t++) {
            BlockPos pos = a.offset(dir.multiply(t));
            BlockState s = level.getBlockState(pos);
            if (!isBlockWindPassable(s)) solidsA++;
        }
        for (int t = c; t <= b; t++) {
            BlockPos pos = a.offset(dir.multiply(t));
            BlockState s = level.getBlockState(pos);
            if (!isBlockWindPassable(s)) solidsB++;
        }
        if (solidsA > 0 && solidsB > 0) {
            return (double) solidsA / (solidsA + solidsB);
        } else if (solidsA > 0) {
            return 1;
        } else if (solidsB > 0) {
            return 0;
        } else {
            return (double) c / b;
        }
    }

    public Vector3d getWindVelocity() {
        return new Vector3d(
                Math.cos(direction) * strength,
                0,
                Math.sin(direction) * strength
        );
    }

    public boolean computeDirectWindExposure(Level level, BlockPos pos) {
        Vec3 p = pos.getCenter();
        Vector3d vel = getWindVelocity().mul(-1).add(0, 1.5, 0);
        Vec3 dir = new Vec3(vel.x, vel.y, vel.z);
        boolean hit = false;
        BlockPos bPos = pos;
        while (p.y < level.getMaxBuildHeight() && p.y > level.getMinBuildHeight()) {
            if (!level.isLoaded(bPos)) {
                break;
            }
            BlockState state = level.getBlockState(bPos);
            if (!isBlockWindPassable(state)) {
                hit = true;
                break;
            }

            int xBound = bPos.getX() + (dir.x > 0 ? 1 : 0);
            int yBound = bPos.getY() + (dir.y > 0 ? 1 : 0);
            int zBound = bPos.getZ() + (dir.z > 0 ? 1 : 0);
            double a = vel.x == 0 ? Double.POSITIVE_INFINITY : (xBound - p.x) / vel.x;
            double b = vel.y == 0 ? Double.POSITIVE_INFINITY : (yBound - p.y) / vel.y;
            double c = vel.z == 0 ? Double.POSITIVE_INFINITY : (zBound - p.z) / vel.z;

            if (a < b && a < c) {
                p = p.add(dir.multiply(a, a, a));
                p = new Vec3(xBound, p.y, p.z);
                bPos = new BlockPos(bPos.getX() + (dir.x > 0 ? 1 : -1), bPos.getY(), bPos.getZ());
            } else if (b < c) {
                p = p.add(dir.multiply(b, b, b));
                p = new Vec3(p.x, yBound, p.z);
                bPos = new BlockPos(bPos.getX(), bPos.getY() + (dir.y > 0 ? 1 : -1), bPos.getZ());
            } else {
                p = p.add(dir.multiply(c, c, c));
                p = new Vec3(p.x, p.y, zBound);
                bPos = new BlockPos(bPos.getX(), bPos.getY(), bPos.getZ() + (dir.z > 0 ? 1 : -1));
            }
        }
//        LogUtils.getLogger().info("Computed wind");
        windComputations++;
        return !hit;
    }

    public WindEntry<Boolean> getDirectWindExposure(Level level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry<Boolean>> directExposure = directWindExposureCache.get((LevelChunk) level.getChunk(pos));
            if (directExposure != null) {
                WindEntry<Boolean> exposed = directExposure.get(pos);
                if (exposed != null && level.getGameTime() - exposed.created < 40) {
                    return exposed;
                }
            }
        }
        boolean hit = computeDirectWindExposure(level, pos);
        WindEntry<Boolean> entry = new WindEntry<>(level.getGameTime() + (pos.hashCode() & 7), hit);
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry<Boolean>> directExposure = directWindExposureCache.computeIfAbsent((LevelChunk) level.getChunk(pos), k -> new HashMap<>());
            directExposure.put(pos, entry);
        }
        return entry;
    }

    private static double interpolate(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }

    public static boolean isBlockWindPassable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof CropBlock;
    }

    public static void tickWind(Iterable<ServerLevel> levels) {
        for (var level : levels) {
            WindSystem system = WindSystem.get(level);
            if (system instanceof ServerWindSystem serverSystem) {
                serverSystem.tick(level);
            }
            if (system.getWind().x > 10 && Config.windBreaksCrops) {
                List<BlockPos> snapshot = new ArrayList<>();

                for (Set<BlockPos> set : system.crops.values()) {
                    snapshot.addAll(set);
                }
                for (var pos : snapshot) {
                    if (system.getBlockWindExposure(level, pos).value * system.getWind().x <= 10) continue;
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
                        LogUtils.getLogger().info("found crop");
                        crops.add(pos);
                    }
                }
            }
        }
    }

    public Vec3 getWindVelocityAt(Level level, BlockPos pos) {
        Vector3d sped = getWindVelocity();
        sped.mul(getBlockWindExposure(level, pos).value);
        return new Vec3(sped.x, sped.y, sped.z);
    }
}
