package org.boberchik342.CreateStormday.raycast;

import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.wind.WindAirflowProvider;
import org.jetbrains.annotations.Nullable;
import dev.ryanhcode.sable.sublevel.SubLevel;

import java.util.ArrayList;
import java.util.List;

public class RaycastHelper {
    public static boolean raycast(Level level, Vec3 pos, Vec3 direction) {
        SubLevel currentSubLevel = Sable.HELPER.getContaining(level, pos);
        if (currentSubLevel != null) {
            pos = currentSubLevel.logicalPose().transformPosition(pos);
        }

        SubLevelContainer container = SubLevelContainer.getContainer(level);
        List<SubLevel> subLevels;
        if (container != null) subLevels = (List<SubLevel>) container.getAllSubLevels();
        else subLevels = new ArrayList<>();

        if (subLevels != null && !subLevels.isEmpty()) {
            for (SubLevel subLevel: subLevels) {
                Vec3 transformedDir = subLevel.logicalPose().transformNormalInverse(direction);
                Vec3 transformedPos = subLevel.logicalPose().transformPositionInverse(pos);
                BoundingBox3ic bb = subLevel.getPlot().getBoundingBox();
                if (raycastInBounds(level, transformedPos, transformedDir, bb)) return true;
            }
        }
        return raycastInBounds(level, pos, direction, null);
    }

    public static boolean raycastInBounds(Level level, Vec3 pos, Vec3 direction, @Nullable BoundingBox3ic bounds) {
        BlockPos.MutableBlockPos blockPos = BlockPos.containing(pos).mutable();
        ChunkPos chunkPos = null;
        LevelChunk chunk = null;
        LevelChunkSection section;
        int chunkSection = -1;
        int sectionY = 0;
        boolean airSection = false;

        if (bounds != null) {
            HitInfo entry = getRayEntry(pos, direction, bounds);
            if (entry == null) return false;
            blockPos.set(entry.blockPos);
            pos = entry.position;
        }

        while (blockPos.getY() < level.getMaxBuildHeight() && blockPos.getY() >= level.getMinBuildHeight() && (bounds == null || bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ()))) {
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

    public record HitInfo (Direction side, Vec3 position, BlockPos blockPos) {

    }

    @Nullable
    public static HitInfo getRayEntry(Vec3 pos, Vec3 direction, BoundingBox3ic bounds) {
        BlockPos blockPos = BlockPos.containing(pos);

        double maxDistance = 0;
        Direction intersectionSide = Direction.UP;

        if (blockPos.getX() < bounds.minX() || blockPos.getX() > bounds.maxX()) {
            if (direction.x == 0) return null;
            int xBound = direction.x > 0 ? bounds.minX() : bounds.maxX() + 1;
            double distance = (xBound - pos.x) / direction.x;
            if (distance < 0) return null;
            if (distance > maxDistance) {
                intersectionSide = direction.x > 0 ? Direction.WEST : Direction.EAST;
                maxDistance = distance;
            }
        }
        if (blockPos.getY() < bounds.minY() || blockPos.getY() > bounds.maxY()) {
            if (direction.y == 0) return null;
            int yBound = direction.y > 0 ? bounds.minY() : bounds.maxY() + 1;
            double distance = (yBound - pos.y) / direction.y;
            if (distance < 0) return null;
            if (distance > maxDistance) {
                intersectionSide = direction.y > 0 ? Direction.DOWN : Direction.UP;
                maxDistance = distance;
            }
        }
        if (blockPos.getZ() < bounds.minZ() || blockPos.getZ() > bounds.maxZ()) {
            if (direction.z == 0) return null;
            int zBound = direction.z > 0 ? bounds.minZ() : bounds.maxZ() + 1;
            double distance = (zBound - pos.z) / direction.z;
            if (distance < 0) return null;
            if (distance > maxDistance) {
                intersectionSide = direction.z > 0 ? Direction.NORTH : Direction.SOUTH;
                maxDistance = distance;
            }
        }
        pos = pos.add(direction.scale(maxDistance));

        blockPos = BlockPos.containing(pos);

        switch (intersectionSide) {
            case Direction.UP -> blockPos = new BlockPos(blockPos.getX(), bounds.maxY(), blockPos.getZ());
            case Direction.DOWN -> blockPos = new BlockPos(blockPos.getX(), bounds.minY(), blockPos.getZ());
            case Direction.EAST -> blockPos = new BlockPos(bounds.maxX(), blockPos.getY(), blockPos.getZ());
            case Direction.WEST -> blockPos = new BlockPos(bounds.minX(), blockPos.getY(), blockPos.getZ());
            case Direction.NORTH -> blockPos = new BlockPos(blockPos.getX(), blockPos.getY(), bounds.minZ());
            case Direction.SOUTH -> blockPos = new BlockPos(blockPos.getX(), blockPos.getY(), bounds.maxZ());
        }

        if (!bounds.contains(blockPos.getX(), blockPos.getY(), blockPos.getZ())) return null;

        return new HitInfo(intersectionSide, pos, blockPos);
    }
}
