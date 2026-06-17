package org.boberchik342.CreateStormday.raycast;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.wind.WindAirflowProvider;

public class RaycastHelper {
    public static boolean raycast(Level level, Vec3 pos, Vec3 direction) {
        BlockPos.MutableBlockPos blockPos = BlockPos.containing(pos).mutable();
        ChunkPos chunkPos = null;
        LevelChunk chunk = null;
        LevelChunkSection section = null;
        int chunkSection = -1;
        int sectionY = 0;
        boolean airSection = false;

        while (blockPos.getY() < level.getMaxBuildHeight() && blockPos.getY() >= level.getMinBuildHeight()) {
            ChunkPos nChunkPos = new ChunkPos(blockPos);
            int s = -1;
            if (chunk != null) {
                s = chunk.getSectionIndex(blockPos.getY());
                sectionY = (chunk.getMinSection() + s) << 4;
            }
            if (!nChunkPos.equals(chunkPos)) {
                chunkPos = nChunkPos;
                chunk = level.getChunkSource().getChunk(chunkPos.x, chunkPos.z, false);
                if (chunk != null) {
                    chunkSection = chunk.getSectionIndex(blockPos.getY());
                    sectionY = (chunk.getMinSection() + chunkSection) << 4;
                    section = chunk.getSection(chunkSection);
                    airSection = section.hasOnlyAir() || !section.maybeHas(state -> !WindAirflowProvider.isBlockWindPassable(state));
                } else {
                    return false;
                }
            } else if (s != chunkSection) {
                chunkSection = s;
                section = chunk.getSection(s);
                airSection = section.hasOnlyAir() || !section.maybeHas(state -> !WindAirflowProvider.isBlockWindPassable(state));
            }

            if (!airSection && !WindAirflowProvider.isBlockWindPassable(chunk.getBlockState(blockPos))) {
                return true;
            }

            int xBound;
            int zBound;
            int yBound;
            if (airSection) {
                xBound = direction.x > 0 ? chunkPos.getMaxBlockX() + 1 : chunkPos.getMinBlockX();
                zBound = direction.z > 0 ? chunkPos.getMaxBlockZ() + 1 : chunkPos.getMinBlockZ();
                yBound = direction.y > 0 ? sectionY + 16 : sectionY;
            } else {
                xBound = blockPos.getX() + (direction.x > 0 ? 1 : 0);
                zBound = blockPos.getZ() + (direction.z > 0 ? 1 : 0);
                yBound = blockPos.getY() + (direction.y > 0 ? 1 : 0);
            }
            double a = direction.x == 0 ? Double.POSITIVE_INFINITY : (xBound - pos.x) / direction.x;
            double b = direction.y == 0 ? Double.POSITIVE_INFINITY : (yBound - pos.y) / direction.y;
            double c = direction.z == 0 ? Double.POSITIVE_INFINITY : (zBound - pos.z) / direction.z;

            if (a < b && a < c) {
                pos = pos.add(direction.multiply(a, a, a));
                pos = new Vec3(xBound, pos.y, pos.z);
                blockPos.set(BlockPos.containing(pos));
                blockPos.set(direction.x > 0 ? xBound : xBound - 1, blockPos.getY(), blockPos.getZ());
            } else if (b < c) {
                pos = pos.add(direction.multiply(b, b, b));
                pos = new Vec3(pos.x, yBound, pos.z);
                blockPos.set(BlockPos.containing(pos));
                blockPos.set(blockPos.getX(), direction.y > 0 ? yBound : yBound - 1, blockPos.getZ());
            } else {
                pos = pos.add(direction.multiply(c, c, c));
                pos = new Vec3(pos.x, pos.y, zBound);
                blockPos.set(BlockPos.containing(pos));
                blockPos.set(blockPos.getX(), blockPos.getY(), direction.z > 0 ? zBound : zBound - 1);
            }
        }
        return false;
    }
}
