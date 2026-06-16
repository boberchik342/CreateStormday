package org.boberchik342.CreateStormday.wind;

import dev.ryanhcode.sable.api.block.BlockSubLevelLiftProvider;
import dev.ryanhcode.sable.companion.math.JOMLConversion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.Config;
import org.boberchik342.CreateStormday.raycast.RaycastHelper;
import org.joml.Vector3d;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public abstract class WindAirflowProvider implements AirflowProvider {
    public final WeakHashMap<LevelChunk, Map<BlockPos, WindSystem.CacheEntry<Boolean>>> directWindExposureCache = new WeakHashMap<>();
    public final WeakHashMap<LevelChunk, Map<BlockPos, WindSystem.CacheEntry<Double>>> interpolatedWindExposureCache = new WeakHashMap<>();

    protected float strength = 0;
    protected float direction = 0;

    private static double interpolate(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }

    public static boolean isBlockWindPassable(BlockState state) {
        return state.isAir() || state.getBlock() instanceof CropBlock || state.getBlock() instanceof SnowLayerBlock || state.getBlock() instanceof BlockSubLevelLiftProvider;
    }

    private static double getT(net.minecraft.world.level.Level level, BlockPos a, int b, Vec3i dir, int c) {
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

    public boolean computeDirectWindExposure(Level level, BlockPos pos) {
        Vec3 p = pos.getCenter();
        Vector3d vel = getWindVelocity().mul(-1).add(0, 5, 0);
        Vec3 dir = new Vec3(vel.x, vel.y, vel.z);
        boolean hit = RaycastHelper.raycast(level, p, dir);
        return !hit;
    }

    public WindSystem.CacheEntry<Boolean> getDirectWindExposure(Level level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindSystem.CacheEntry<Boolean>> directExposure = directWindExposureCache.get((LevelChunk) level.getChunk(pos));
            if (directExposure != null) {
                WindSystem.CacheEntry<Boolean> exposed = directExposure.get(pos);
                if (exposed != null && level.getGameTime() - exposed.created < 40) {
                    return exposed;
                }
            }
        }
        boolean hit = computeDirectWindExposure(level, pos);
        WindSystem.CacheEntry<Boolean> entry = new WindSystem.CacheEntry<>(level.getGameTime() + (pos.hashCode() & 7), hit);
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindSystem.CacheEntry<Boolean>> directExposure = directWindExposureCache.computeIfAbsent((LevelChunk) level.getChunk(pos), k -> new HashMap<>());
            directExposure.put(pos, entry);
        }
        return entry;
    }

    public WindSystem.CacheEntry<Double> getBlockWindExposure(net.minecraft.world.level.Level level, BlockPos pos) {
        if (Config.windSampleInterval == 1) {
            return new WindSystem.CacheEntry<>(level.getGameTime(), getDirectWindExposure(level, pos).value ? 1. : 0.);
        }

        if (level.isLoaded(pos)) {
            Map<BlockPos, WindSystem.CacheEntry<Double>> directExposure = interpolatedWindExposureCache.get((LevelChunk) level.getChunk(pos));
            if (directExposure != null) {
                WindSystem.CacheEntry<Double> exposed = directExposure.get(pos);
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
        WindSystem.CacheEntry<Boolean> e000 = getDirectWindExposure(level, b000);
        WindSystem.CacheEntry<Boolean> e001 = getDirectWindExposure(level, b001);
        WindSystem.CacheEntry<Boolean> e010 = getDirectWindExposure(level, b010);
        WindSystem.CacheEntry<Boolean> e011 = getDirectWindExposure(level, b011);
        WindSystem.CacheEntry<Boolean> e100 = getDirectWindExposure(level, b100);
        WindSystem.CacheEntry<Boolean> e101 = getDirectWindExposure(level, b101);
        WindSystem.CacheEntry<Boolean> e110 = getDirectWindExposure(level, b110);
        WindSystem.CacheEntry<Boolean> e111 = getDirectWindExposure(level, b111);

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
        WindSystem.CacheEntry<Double> entry = new WindSystem.CacheEntry<>(earliestRaycast, interpolate(e0, e1, tZ));
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindSystem.CacheEntry<Double>> directExposure = interpolatedWindExposureCache.computeIfAbsent((LevelChunk) level.getChunk(pos), k -> new HashMap<>());
            directExposure.put(pos, entry);
        }
        return entry;
    }

    public Vector3d getWindVelocity() {
        return new Vector3d(
                Math.cos(direction) * strength,
                0,
                Math.sin(direction) * strength
        );
    }

    @Override
    public Vec3 getWind(Level level, Vec3 pos) {
        BlockPos blockPos = BlockPos.containing(pos);
        return JOMLConversion.toMojang(getWindVelocity()).scale(getBlockWindExposure(level, blockPos).value);
    }

    @Override
    public double getMaxWindSpeed() {
        return strength;
    }
}
