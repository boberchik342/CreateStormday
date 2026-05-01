package org.boberchik342.CreateStormday.wind;

import com.mojang.logging.LogUtils;
import dev.ryanhcode.sable.api.SubLevelHelper;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.mixin.BlockSubLevelLiftProviderMixin;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class RaycastOctree {
    private int sizePower = 0;
    private BlockPos origin = new BlockPos(0, 0, 0);
    private final IntArrayList data = new IntArrayList();
    private final Stack<Integer> freeIndices = new Stack<>();

    private static class NodeInfo {
        public NodeInfo(int id, Bounds bounds) {
            this.id = id;
            this.bounds = bounds;
        }

        int id;
        Bounds bounds;
    }

    public RaycastOctree() {
        data.add(-1); // make the root node leaf with false as the value
    }

    public void set(BlockPos pos, boolean value) {
        LogUtils.getLogger().info("Set {} to {}", pos, value);
        if (!contains(pos)) {
            if (!value) return;
            expandToContain(pos);
        }

        NodeInfo nodeInfo = new NodeInfo(0, getBounds());
        Stack<Integer> trace = new Stack<>(); // used to collapse the nodes

        while (true) {
            trace.add(nodeInfo.id);
            if (nodeInfo.bounds.isSingleBlock()) {
                data.set(nodeInfo.id, value ? -2 : -1); // set the value
                trace.pop();
                while (!trace.empty() && tryCollapse(trace.pop())) {}
                return;
            } else {
                if (data.getInt(nodeInfo.id) < 0) {
                    createChildren(nodeInfo.id);
                }

                BlockPos origin = nodeInfo.bounds.getOrigin();

                boolean up = pos.getY() >= origin.getY();
                boolean east = pos.getX() >= origin.getX();
                boolean south = pos.getZ() >= origin.getZ();

                int childIndex = getChildIndex(up, east, south);
                Bounds childBounds = getChildBounds(nodeInfo.bounds, childIndex);
                nodeInfo = new NodeInfo(data.getInt(nodeInfo.id) + childIndex, childBounds);
            }
        }
    }

    /**
     * fills an area with a specified value
     * @param a first corner of an area
     * @param b second corner of an area
     * @param value the value to fill the area with
     */
    public void fill(BlockPos a, BlockPos b, boolean value) {
        // TODO: implement ts
    }

    /**
     * gets the value stored at a certain position
     * @param pos the block position to retrieve data for
     * @return the value stored at that position, if the position isn't contained by the octree returns false
     */
    public boolean get(BlockPos pos) {
        LogUtils.getLogger().info("Get {}", pos);
        if (!contains(pos)) return false;

        NodeInfo nodeInfo = new NodeInfo(0, getBounds());

        while (true) {
            if (nodeInfo.bounds.isSingleBlock()) {
                return data.getInt(nodeInfo.id) == -2;
            } else {
                if (data.getInt(nodeInfo.id) < 0) {
                    return data.getInt(nodeInfo.id) == -2;
                }

                BlockPos origin = nodeInfo.bounds.getOrigin();

                boolean up = pos.getY() >= origin.getY();
                boolean east = pos.getX() >= origin.getX();
                boolean south = pos.getZ() >= origin.getZ();

                int childIndex = getChildIndex(up, east, south);
                Bounds childBounds = getChildBounds(nodeInfo.bounds, childIndex);
                nodeInfo = new NodeInfo(data.getInt(nodeInfo.id) + childIndex, childBounds);
            }
        }
    }

    public boolean raycast(Vec3 pos, Vec3 direction) {
        // TODO: implement ts
        return false;
    }

    /**
     * expands the octree until it contains a specified block position
     * @param pos the block position for octree to contain
     */
    private void expandToContain(BlockPos pos) {
        Bounds bounds = getBounds();
        while (!bounds.contains(pos)) {
//            LogUtils.getLogger().info("expanding");
            boolean up = pos.getY() > bounds.upper;
            boolean east = pos.getX() > bounds.east;
            boolean south = pos.getZ() > bounds.south;
            if (data.getInt(0) != -1) {
                int childId = getChildIndex(!up, !east, !south);
                int id = createChildren(false);
                data.set(id + childId, data.getInt(0)); // move the root node
                data.set(0, id); // set the root to point at new children
            }

            sizePower++;
            if (up) {
                bounds.upper = bounds.lower + (1 << sizePower) - 1;
            } else {
                bounds.lower = bounds.upper - (1 << sizePower) + 1;
            }
            if (east) {
                bounds.east = bounds.west + (1 << sizePower) - 1;
            } else {
                bounds.west = bounds.east - (1 << sizePower) + 1;
            }
            if (south) {
                bounds.south = bounds.north + (1 << sizePower) - 1;
            } else {
                bounds.north = bounds.south - (1 << sizePower) + 1;
            }
            origin = bounds.getOrigin();
//            printBounds(bounds);
        }
    }

    /**
     * checks if all children can be represented as a single node and removes them if yes
     * @param id the id of the node to collapse
     * @return true if the node collapsed and false if otherwise
     */
    private boolean tryCollapse(int id) {
        boolean equal = true;
        int firstChild = data.getInt(id);
        int value = data.getInt(firstChild);
        for (int i = 1; i < 8; i++) {
            if (data.getInt(firstChild + i) != value) {
                equal = false;
                break;
            }
        }
        if (!equal) return false;
        removeChildren(firstChild);
        data.set(id, value);
        return true;
    }

    /**
     * creates children for a specified node
     * @param id an id of the node to create children for; the node must be a leaf node
     * @return the id of the first created child
     */
    private int createChildren(int id) {
        boolean value = data.getInt(id) == -2;
        int firstChild = createChildren(value);
        data.set(id, firstChild);
        return firstChild;
    }

    /**
     * creates 8 nodes in a row
     * @param value the value to give to the created nodes
     * @return the id of the first created node
     */
    private int createChildren(boolean value) {
        int id;
        if (freeIndices.empty()) {
            id = data.size();
            for (int i = 0; i < 8; i++) {
                data.add(value ? -2 : -1);
            }
            return id;
        }
        id = freeIndices.pop();
        for (int i = 0; i < 8; i++) {
            data.set(id + i, value ? -2 : -1);
        }
        return id;
    }

    /**
     * deletes children
     * @param id the id of the first of the 8 children to delete
     */
    private void removeChildren(int id) {
        freeIndices.push(id);
    }

    // TODO: represent bounds as origin + size
    private static class Bounds {
        public int upper;
        public int lower;
        public int east;
        public int west;
        public int south;
        public int north;

        public boolean contains(BlockPos pos) {
            LogUtils.getLogger().info("does contain {}", pos);
            return pos.getY() <= upper && pos.getY() >= lower &&
                    pos.getX() <= east && pos.getX() >= west &&
                    pos.getZ() <= south && pos.getZ() >= north;
        }

        public BlockPos getOrigin() {
            return new BlockPos(
                    (east + west + 1) / 2,
                    (upper + lower + 1) / 2,
                    (south + north + 1) / 2
            );
        }

        public Bounds copy() {
            Bounds bounds = new Bounds();
            bounds.south = south;
            bounds.north = north;
            bounds.upper = upper;
            bounds.lower = lower;
            bounds.east = east;
            bounds.west = west;
            return bounds;
        }

        public boolean isSingleBlock() {
            return south == north && east == west && upper == lower;
        }
    }

    private boolean contains(BlockPos pos) {
        return getBounds().contains(pos);
    }

    private Bounds getBounds() {
        Bounds bounds = new Bounds();
        int size = 1 << sizePower;
        LogUtils.getLogger().info("Size: {}", size);
        int half = size >> 1;
        bounds.upper = half + origin.getY() - 1 + (size & 1);
        bounds.lower = -half + origin.getY();
        bounds.east = half + origin.getX() - 1 + (size & 1);
        bounds.west = -half + origin.getX();
        bounds.south = half + origin.getZ() - 1 + (size & 1);
        bounds.north = -half + origin.getZ();
        LogUtils.getLogger().info("half: {}", half);
        return bounds;
    }

    private static int getChildIndex(boolean up, boolean east, boolean south) {
        return ((up ? 1 : 0) << 2) + ((east ? 1 : 0) << 1) + (south ? 1 : 0);
    }

    private static Bounds getChildBounds(Bounds bounds, int childIndex) {
        boolean up = (childIndex >> 2 & 1) != 0;
        boolean east = (childIndex >> 1 & 1) != 0;
        boolean south = (childIndex & 1) != 0;
        BlockPos origin = bounds.getOrigin();
        LogUtils.getLogger().info("Computing child bounds");
        LogUtils.getLogger().info("origin: {}", origin);
        Bounds childBounds = bounds.copy();
        if (east) childBounds.west = origin.getX(); else childBounds.east = origin.getX() - 1;
        if (up) childBounds.lower = origin.getY(); else childBounds.upper = origin.getY() - 1;
        if (south) childBounds.north = origin.getZ(); else childBounds.south = origin.getZ() - 1;
        return childBounds;
    }

    private static void printBounds(Bounds bounds) {
        LogUtils.getLogger().info("bounds:");
        LogUtils.getLogger().info("up - {}", bounds.upper);
        LogUtils.getLogger().info("down - {}", bounds.lower);
        LogUtils.getLogger().info("east - {}", bounds.east);
        LogUtils.getLogger().info("west - {}", bounds.west);
        LogUtils.getLogger().info("south - {}", bounds.south);
        LogUtils.getLogger().info("north - {}", bounds.north);
    }

    /**
     * checks the structure of the octree
     * @return true if the structure is correct and false if otherwise
     */
    public boolean structureCheck() {
        Stack<Integer> stack = new Stack<>();
        Set<Integer> visited = new HashSet<>();
        stack.add(0);

        while (!stack.empty()) {
            int id = stack.pop();
            if (visited.contains(id)) {
                return false;
            }
            visited.add(id);
            if (data.getInt(id) < 0) {
                continue;
            }

            int firstChild = data.getInt(id);
            int value = data.getInt(firstChild);
            boolean equal = true;
            for (int i = 0; i < 8; i++) {
                stack.add(firstChild + i);
                if (data.getInt(firstChild + i) != value) {
                    equal = false;
                }
            }
            if (equal) return false;
        }

        return true;
    }

    public static boolean boundsLogicCheck() {
        Bounds bounds = new Bounds();
        bounds.north = 0;
        bounds.south = 0;
        bounds.west = 0;
        bounds.east = 0;
        bounds.upper = 0;
        bounds.lower = 0;
        if (!bounds.isSingleBlock()) return false;
        bounds = new Bounds();
        bounds.north = -2;
        bounds.south = 1;
        bounds.west = -2;
        bounds.east = 1;
        bounds.upper = 1;
        bounds.lower = -2;
        if (bounds.isSingleBlock()) return false;
        Bounds childBounds = getChildBounds(bounds, getChildIndex(true, true, true));
        if (
                childBounds.lower != 0 || childBounds.upper != 1 ||
                childBounds.west != 0 || childBounds.east != 1 ||
                childBounds.north != 0 || childBounds.south != 1
        ) return false;
        if (childBounds.isSingleBlock()) return false;
        childBounds = getChildBounds(bounds, getChildIndex(false, false, false));
        if (
                childBounds.lower != -2 || childBounds.upper != -1 ||
                childBounds.west != -2 || childBounds.east != -1 ||
                childBounds.north != -2 || childBounds.south != -1
        ) return false;
        if (childBounds.isSingleBlock()) return false;

        return true;
    }
}
