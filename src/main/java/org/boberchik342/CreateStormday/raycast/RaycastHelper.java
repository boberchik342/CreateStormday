package org.boberchik342.CreateStormday.raycast;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class RaycastHelper {
    private static final Map<Level, RaycastOctree> clientOctrees = new WeakHashMap<>();
    private static final Map<Level, RaycastOctree> serverOctrees = new WeakHashMap<>();

    public static RaycastOctree getOctree(Level level) {
        return getOctreeMap(level.isClientSide).computeIfAbsent(level, k -> new RaycastOctree(level));
    }

    private static Map<Level, RaycastOctree>  getOctreeMap(boolean isClient) {
        return isClient ? clientOctrees : serverOctrees;
    }

    public static boolean raycast(Level level, Vec3 pos, Vec3 direction) {
        return getOctree(level).raycast(pos, direction, level);
    }

    public static int chunksLoaded = 0;

    public static void tick(boolean isClient) {
        LogUtils.getLogger().info(String.valueOf(chunksLoaded));
        chunksLoaded = 0;
        for (var entry : getOctreeMap(isClient).entrySet()) {
            Player player = null;
            Vec3 pos;
            if (isClient) {
                player = Minecraft.getInstance().player;
            } else {
                List<Player> players = (List<Player>) entry.getKey().players();
                if (!players.isEmpty()) player = players.get(entry.getKey().random.nextInt(players.size()));
            }
            if (player != null) {
                pos = player.getEyePosition();
                entry.getValue().loadChunkNear(pos);
            }
        }
    }
}
