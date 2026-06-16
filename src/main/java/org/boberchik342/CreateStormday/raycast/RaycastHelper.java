package org.boberchik342.CreateStormday.raycast;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.WeakHashMap;

public class RaycastHelper {
    private static final WeakHashMap<Level, RaycastOctree> octrees = new WeakHashMap<>();

    public static RaycastOctree getOctree(Level level) {
        return octrees.computeIfAbsent(level, k -> new RaycastOctree(level));
    }

    public static boolean raycast(Level level, Vec3 pos, Vec3 direction) {
        return getOctree(level).raycast(pos, direction, level);
    }

    public static int chunksLoaded = 0;

    public static void tick() {
        LogUtils.getLogger().info(String.valueOf(chunksLoaded));
        chunksLoaded = 0;
    }
}
