package org.boberchik342.CreateStormday.raycast;

import com.mojang.logging.LogUtils;
import net.minecraft.world.level.Level;

import java.util.WeakHashMap;

public class RaycastHelper {
    private static final WeakHashMap<Level, RaycastOctree> octrees = new WeakHashMap<>();

    public static RaycastOctree get(Level level) {
        return octrees.computeIfAbsent(level, k -> new RaycastOctree(level));
    }

    public static int chunksLoaded = 0;

    public static void tick() {
        LogUtils.getLogger().info(String.valueOf(chunksLoaded));
        chunksLoaded = 0;
    }
}
