package org.boberchik342.CreateStormday.raycast;

import net.minecraft.world.level.Level;

import java.util.WeakHashMap;

public class RaycastHelper {
    private static final WeakHashMap<Level, RaycastOctree> octrees = new WeakHashMap<>();

    public static RaycastOctree get(Level level) {
        return octrees.computeIfAbsent(level, k -> new RaycastOctree(level));
    }
}
