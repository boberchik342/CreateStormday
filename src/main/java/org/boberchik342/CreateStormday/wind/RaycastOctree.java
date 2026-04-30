package org.boberchik342.CreateStormday.wind;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.Stack;

public class RaycastOctree {
    private int sizePower = 0;
    private BlockPos origin;
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
        if (!contains(pos)) {
            if (!value) return;
            expandToContain(pos);
        }

        NodeInfo nodeInfo = new NodeInfo(0, getBounds());
        Stack<Integer> trace = new Stack<>(); // used to collapse the nodes

        while (nodeInfo != null) {
            trace.add(nodeInfo.id);
            if (nodeInfo.bounds.isSingleBlock()) {
                data.set(nodeInfo.id, value ? -2 : -1); // set the value
                trace.pop();
                while (!trace.empty() && tryCollapse(trace.pop())) {}
            } else {
                if (data.getInt(nodeInfo.id) < 0) {
                    createChildren(nodeInfo.id);
                }
                // TODO: compute child index instead of iterating
                NodeInfo nextNodeInfo = null;
                for (int i = 0; i < 8; i++) {
                    Bounds childBounds = getChildBounds(nodeInfo.bounds, i);
                    if (childBounds.contains(pos)) {
                        nextNodeInfo = new NodeInfo(data.getInt(nodeInfo.id + i), childBounds);
                        break;
                    }
                }
                nodeInfo = nextNodeInfo;
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
        if (!contains(pos)) return false;

        NodeInfo nodeInfo = new NodeInfo(0, getBounds());

        while (nodeInfo != null) {
            if (nodeInfo.bounds.isSingleBlock()) {
                return data.getInt(nodeInfo.id) == -2;
            } else {
                if (data.getInt(nodeInfo.id) < 0) {
                    return data.getInt(nodeInfo.id) == -2;
                }
                // TODO: compute child index instead of iterating
                NodeInfo nextNodeInfo = null;
                for (int i = 0; i < 8; i++) {
                    Bounds childBounds = getChildBounds(nodeInfo.bounds, i);
                    if (childBounds.contains(pos)) {
                        nextNodeInfo = new NodeInfo(data.getInt(nodeInfo.id + i), childBounds);
                        break;
                    }
                }
                nodeInfo = nextNodeInfo;
            }
        }
        throw new RuntimeException("This really shouldn't happen");
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
            boolean up = pos.getY() > bounds.upper;
            boolean east = pos.getX() > bounds.east;
            boolean south = pos.getZ() > bounds.south;
            int childId = getChildIndex(up, east, south);
            int id = createChildren(false);
            data.set(id + childId, data.getInt(0)); // move the root node
            data.set(0, id); // set the root to point at new children
            sizePower <<= 1;
            origin = new BlockPos(
                    up ? bounds.upper + 1 : bounds.lower,
                    east ? bounds.east + 1 : bounds.west,
                    south ? bounds.south + 1 : bounds.north
            );
            bounds = getBounds();
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
            return pos.getY() <= upper && pos.getY() >= lower &&
                    pos.getX() <= east && pos.getX() >= west &&
                    pos.getZ() <= south && pos.getZ() >= north;
        }

        public BlockPos getOrigin() {
            return new BlockPos(
                    west - (east - west) / 2,
                    lower - (upper - lower) / 2,
                    north - (south - north) / 2
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
        bounds.upper = size >> 1 - size & 1 + origin.getY();
        bounds.lower = -size >> 1 + origin.getY();
        bounds.east = size >> 1 - size & 1 + origin.getX();
        bounds.west = -size >> 1 + origin.getX();
        bounds.south = size >> 1 - size & 1 + origin.getZ();
        bounds.north = -size >> 1 + origin.getZ();
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
        Bounds childBounds = bounds.copy();
        if (east) childBounds.west = origin.getY(); else childBounds.east = origin.getY() - 1;
        if (up) childBounds.lower = origin.getY(); else childBounds.upper = origin.getY() - 1;
        if (south) childBounds.north = origin.getY(); else childBounds.south = origin.getY() - 1;
        return childBounds;
    }
}
