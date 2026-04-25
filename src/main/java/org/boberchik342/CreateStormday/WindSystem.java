package org.boberchik342.CreateStormday;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class WindSystem {
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
    public final WeakHashMap<LevelChunk, Map<BlockPos, Float>> windExposureCache = new WeakHashMap<>();

    protected float strength = 0;
    protected float direction = 0;

    public WindSystem(Level level) {
        windSystems.put(level, this);
    }

    public boolean isBlockExposed(Level level, BlockPos pos) {
        return level.canSeeSky(pos.above());
    }

    public abstract Vec2 getWind();

    public Vector3d getWindVelocity() {
        return new Vector3d(
                Math.cos(direction) * strength,
                0,
                Math.sin(direction) * strength
        );
    }
}
