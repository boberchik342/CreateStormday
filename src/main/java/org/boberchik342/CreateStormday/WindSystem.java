package org.boberchik342.CreateStormday;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

import java.util.*;

public abstract class WindSystem {
    public static class WindEntry {
        public WindEntry(long created, double value) {
            this.created = created;
            this.value = value;
        }
        public long created;
        public double value;
    }
    public static int windComputations = 0;
    public static final int sampleInterval = 4;
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
    public final WeakHashMap<LevelChunk, Map<BlockPos, WindEntry>> directWindExposureCache = new WeakHashMap<>();

    protected float strength = 0;
    protected float direction = 0;

    public WindSystem(Level level) {
        windSystems.put(level, this);
    }

    public double getBlockWindExposure(Level level, BlockPos pos) {
        BlockPos origin = new BlockPos(Math.floorDiv(pos.getX(), sampleInterval) * sampleInterval, Math.floorDiv(pos.getY(), sampleInterval) * sampleInterval, Math.floorDiv(pos.getZ(), sampleInterval) * sampleInterval);
        double e000 = getDirectWindExposureD(level, origin);
        double e001 = getDirectWindExposureD(level, new BlockPos(origin.getX(), origin.getY(), origin.getZ() + sampleInterval));
        double e010 = getDirectWindExposureD(level, new BlockPos(origin.getX(), origin.getY() + sampleInterval, origin.getZ()));
        double e011 = getDirectWindExposureD(level, new BlockPos(origin.getX(), origin.getY() + sampleInterval, origin.getZ() + sampleInterval));
        double e100 = getDirectWindExposureD(level, new BlockPos(origin.getX() + sampleInterval, origin.getY(), origin.getZ()));
        double e101 = getDirectWindExposureD(level, new BlockPos(origin.getX() + sampleInterval, origin.getY(), origin.getZ() + sampleInterval));
        double e110 = getDirectWindExposureD(level, new BlockPos(origin.getX() + sampleInterval, origin.getY() + sampleInterval, origin.getZ()));
        double e111 = getDirectWindExposureD(level, new BlockPos(origin.getX() + sampleInterval, origin.getY() + sampleInterval, origin.getZ() + sampleInterval));
        int localX = Math.floorMod(pos.getX(), sampleInterval);
        int localY = Math.floorMod(pos.getY(), sampleInterval);
        int localZ = Math.floorMod(pos.getZ(), sampleInterval);
        double tX = localX / (double) sampleInterval;
        double tY = localY / (double) sampleInterval;
        double tZ = localZ / (double) sampleInterval;
        double e00 = interpolate(e000, e100, tX);
        double e01 = interpolate(e001, e101, tX);
        double e10 = interpolate(e010, e110, tX);
        double e11 = interpolate(e011, e111, tX);
        double e0 = interpolate(e00, e10, tY);
        double e1 = interpolate(e01, e11, tY);
        return interpolate(e0, e1, tZ);
    }

    public abstract Vec2 getWind();

    public Vector3d getWindVelocity() {
        return new Vector3d(
                Math.cos(direction) * strength,
                0,
                Math.sin(direction) * strength
        );
    }

    public boolean computeDirectWindExposure(Level level, BlockPos pos) {
        Vec3 p = pos.getCenter();
        Vector3d vel = getWindVelocity().mul(-1).add(0, 0.5, 0);
        Vec3 dir = new Vec3(vel.x, vel.y, vel.z);
        boolean hit = false;
        BlockPos bPos = pos;
        while (p.y < level.getMaxBuildHeight() && p.y > level.getMinBuildHeight()) {
            if (!level.isLoaded(bPos)) {
                break;
            }
            BlockState state = level.getBlockState(bPos);
            if (!state.isAir()) {
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
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry> directExposure = directWindExposureCache.computeIfAbsent((LevelChunk) level.getChunk(pos), k -> new HashMap<>());
            directExposure.put(pos, new WindEntry(level.getGameTime(), hit ? 1 : 0));
        }
//        LogUtils.getLogger().info("Computed wind");
        windComputations++;
        return !hit;
    }

    public double getDirectWindExposureD(Level level, BlockPos pos) {
        if (level.isLoaded(pos)) {
            Map<BlockPos, WindEntry> directExposure = directWindExposureCache.get((LevelChunk) level.getChunk(pos));
            if (directExposure != null) {
                WindEntry exposed = directExposure.get(pos);
                if (exposed != null && level.getGameTime() - exposed.created < 40) {
//                    LogUtils.getLogger().info("Cache hitttttt!!!!!!!");
                    return exposed.value;
                }
            }
        }
        return computeDirectWindExposure(level, pos) ? 1 : 0;
    }

    private static double interpolate(double a, double b, double t) {
        return a * (1 - t) + b * t;
    }
}
