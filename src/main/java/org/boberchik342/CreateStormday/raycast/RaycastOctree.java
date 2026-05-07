package org.boberchik342.CreateStormday.raycast;

import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import org.boberchik342.CreateStormday.wind.WindSystem;

import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

public class RaycastOctree {
    public static boolean frozen = false;
    private int sizePower = 0;
    private BlockPos origin = new BlockPos(0, 0, 0);
    private final IntArrayList data = new IntArrayList();
    private final Stack<Integer> freeIndices = new Stack<>();

    private static class NodeInfo {
        public NodeInfo(int id, Bounds bounds, int depth) {
            this.id = id;
            this.bounds = bounds;
            this.depth = depth;
        }

        int depth;
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

        NodeInfo nodeInfo = new NodeInfo(0, getBounds(), 0);
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
                nodeInfo = new NodeInfo(data.getInt(nodeInfo.id) + childIndex, childBounds, nodeInfo.depth + 1);
            }
        }
    }

    private static class TraceElement {
        int id;
        boolean childrenModified = false;

        public TraceElement(int id) {
            this.id = id;
        }
    }

    private boolean validateTrace(Stack<TraceElement> trace) {
        int firstChild = 0;
        for (int i = 0; i < trace.size(); i++) {
            TraceElement element = trace.get(i);
            if (element.id > firstChild + 7 || element.id < firstChild) {
                LogUtils.getLogger().info("next element in trace is not a child of previous element");
                if (i == 0) {
                    LogUtils.getLogger().info("This is the first element in the trace, so most likely this is wrong");
                } else {
                    LogUtils.getLogger().info("Previous element: {}", trace.get(i - 1).id);
                    LogUtils.getLogger().info("Current element: {}", trace.get(i).id);
                }
                return false;
            }
            int val = data.getInt(element.id);
            if (val < 0) {
                if (i == trace.size() - 1) {
                    return true;
                } else {
                    LogUtils.getLogger().info("Not last element is leaf");
                    return false;
                }
            } else {
                firstChild = val;
            }
        }
        return true;
    }

    /**
     * fills an area with a specified value
     * @param a first corner of an area
     * @param b second corner of an area
     * @param value the value to fill the area with
     */
    public void fill(BlockPos a, BlockPos b, boolean value) {
        Bounds fillBounds = new Bounds();

        if (value) {
            expandToContain(a);
            expandToContain(b);
        }

        fillBounds.east = Math.max(a.getX(), b.getX());
        fillBounds.south = Math.max(a.getZ(), b.getZ());
        fillBounds.upper = Math.max(a.getY(), b.getY());
        fillBounds.west = Math.min(a.getX(), b.getX());
        fillBounds.north = Math.min(a.getZ(), b.getZ());
        fillBounds.lower = Math.min(a.getY(), b.getY());

        Stack<NodeInfo> stack = new Stack<>();
        Stack<TraceElement> trace = new Stack<>();
        stack.add(new NodeInfo(0, getBounds(), 0));

        while (!stack.empty()) {
            NodeInfo nodeInfo = stack.pop();
            while (trace.size() > nodeInfo.depth) {
                TraceElement element = trace.pop();
                if (trace.empty()) continue;
                if (element.childrenModified && tryCollapse(element.id) && !trace.empty()) {
                    trace.peek().childrenModified = true;
                }
            }

            if (fillBounds.doesContain(nodeInfo.bounds)) {
                deleteDescendants(nodeInfo.id);
                data.set(nodeInfo.id, value ? -2 : -1);
                // TODO: only set modified if actually modified
                if (!trace.isEmpty()) trace.peek().childrenModified = true;
            } else if (fillBounds.intersects(nodeInfo.bounds)) {
                if (nodeInfo.bounds.isSingleBlock()) {
                    LogUtils.getLogger().info("fill bounds");
                    printBounds(fillBounds);
                    LogUtils.getLogger().info("block bounds");
                    printBounds(nodeInfo.bounds);
                    throw new RuntimeException("shouldn't happen");
                }
                int val = data.getInt(nodeInfo.id);
                if (val == (value ? -2 : -1)) { // making sure at least one block is going to be modified or else created children will not get collapsed
                    continue;
                }
                trace.push(new TraceElement(nodeInfo.id));
                if (val < 0) {
                    LogUtils.getLogger().info("Creating children");
                    createChildren(nodeInfo.id);
                    trace.peek().childrenModified = true; // this shouldn't fix it and it didn't
                    val = data.getInt(nodeInfo.id);
                }
                for (int i = 0; i < 8; i++) {
                    stack.add(new NodeInfo(val + i, getChildBounds(nodeInfo.bounds, i), nodeInfo.depth + 1));
                }
            }
        }
        while (!trace.isEmpty()) {
            TraceElement element = trace.pop();
            if (trace.empty()) continue;
            if (element.childrenModified && tryCollapse(element.id) && !trace.empty()) {
                trace.peek().childrenModified = true;
            }
        }
    }

    /**
     * removes all descendants of a specified node<br>
     * the node must not be a leaf node<br>
     * <b>this does not make it stop pointing to a non-existent node</b>
     * @param id an id of the node whose descendants to delete
     */
    private void deleteDescendants(int id) {
        Stack<Integer> stack = new Stack<>();
        int val = data.getInt(id);
        if (val < 0) return;
        data.set(id, -3);
        for (int i = 0; i < 8; i++) {
            stack.add(val + i);
        }
        freeIndices.add(val);
        while (!stack.empty()) {
            int nodeId = stack.pop();
            int value = data.getInt(nodeId);
            if (value >= 0) {
                for (int i = 0; i < 8; i++) {
                    stack.add(value + i);
                }
                freeIndices.add(value);
            }
        }
    }

    /**
     * gets the value stored at a certain position
     * @param pos the block position to retrieve data for
     * @return the value stored at that position, if the position isn't contained by the octree returns false
     */
    public boolean get(BlockPos pos) {
        if (!contains(pos)) return false;

        NodeInfo nodeInfo = new NodeInfo(0, getBounds(), 0);

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
                nodeInfo = new NodeInfo(data.getInt(nodeInfo.id) + childIndex, childBounds, nodeInfo.depth + 1);
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
     * checks if all children can be represented as a single node and removes them if yes<br>
     * <b>node must not be a leaf node</b>
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

        public boolean doesContain(Bounds bounds) {
            return bounds.lower >= lower && bounds.upper <= upper &&
                    bounds.west >= west && bounds.east <= east &&
                    bounds.north >= north && bounds.south <= south;
        }

        public boolean intersects(Bounds bounds) {
            return !(bounds.west > east || bounds.lower > upper || bounds.north > south ||
                    bounds.east < west || bounds.upper < lower || bounds.south < north);
        }
    }

    private boolean contains(BlockPos pos) {
        return getBounds().contains(pos);
    }

    private Bounds getBounds() {
        Bounds bounds = new Bounds();
        int size = 1 << sizePower;
        int half = size >> 1;
        bounds.upper = half + origin.getY() - 1 + (size & 1);
        bounds.lower = -half + origin.getY();
        bounds.east = half + origin.getX() - 1 + (size & 1);
        bounds.west = -half + origin.getX();
        bounds.south = half + origin.getZ() - 1 + (size & 1);
        bounds.north = -half + origin.getZ();
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
                LogUtils.getLogger().info("A node was visited twice");
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
        return !childBounds.isSingleBlock();
    }

    public static void test() {
        if (!RaycastOctree.boundsLogicCheck()) {
            throw new RuntimeException("Bounds logic check did not pass");
        }
        RaycastOctree octree = new RaycastOctree();
        Set<BlockPos> enabled = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            BlockPos pos = new BlockPos(
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50)
            );
            enabled.add(pos);
            octree.set(pos, true);
        }

        for (int i = 0; i < 100; i++) {
            BlockPos a = new BlockPos(
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50)
            );
            BlockPos b = a.offset(new Vec3i(
                    (int)(Math.random()*10),
                    (int)(Math.random()*10),
                    (int)(Math.random()*10)
            ));
            boolean value = Math.random() > 0.5;
            octree.fill(a, b, value);
            for (int x = a.getX(); x <= b.getX(); x++) {
                for (int y = a.getY(); y <= b.getY(); y++) {
                    for (int z = a.getZ(); z <= b.getZ(); z++) {
                        if (value) {
                            enabled.add(new BlockPos(x, y, z));
                        } else {
                            enabled.remove(new BlockPos(x, y, z));
                        }

                    }
                }
            }
            LogUtils.getLogger().info("Fill: {}", i);
        }

        for (int i = 0; i < 100000; i++) {
            BlockPos pos = new BlockPos(
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50),
                    (int)(Math.random()*100-50)
            );
            if (enabled.contains(pos) != octree.get(pos)) {
                throw new RuntimeException("Octree didn't pass the test");
            }
        }
        if (!octree.structureCheck()) {
            throw new RuntimeException("Octree didn't pass structure test");
        }
    }

    public void loadChunk(LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();

        BlockPos a = new BlockPos(cp.getMinBlockX(), chunk.getMinBuildHeight(), cp.getMinBlockZ());
        BlockPos b = new BlockPos(cp.getMaxBlockX(), chunk.getMaxBuildHeight(), cp.getMaxBlockZ());

        expandToContain(a);
        expandToContain(b);

        Bounds chunkBounds = new Bounds();
        chunkBounds.east = Math.max(a.getX(), b.getX());
        chunkBounds.south = Math.max(a.getZ(), b.getZ());
        chunkBounds.upper = Math.max(a.getY(), b.getY());
        chunkBounds.west = Math.min(a.getX(), b.getX());
        chunkBounds.north = Math.min(a.getZ(), b.getZ());
        chunkBounds.lower = Math.min(a.getY(), b.getY());

        Stack<NodeInfo> stack = new Stack<>();
        Stack<TraceElement> trace = new Stack<>();
        stack.add(new NodeInfo(0, getBounds(), 0));

        while (!stack.empty()) {
            NodeInfo nodeInfo = stack.pop();
            while (trace.size() > nodeInfo.depth) {
                TraceElement element = trace.pop();
                if (trace.empty()) continue;
                if (element.childrenModified && tryCollapse(element.id) && !trace.empty()) {
                    trace.peek().childrenModified = true;
                }
            }

            if (chunkBounds.intersects(nodeInfo.bounds)) {
                if (nodeInfo.bounds.isSingleBlock()) {
                    BlockPos pos = new BlockPos(nodeInfo.bounds.east, nodeInfo.bounds.upper, nodeInfo.bounds.south);
                    data.set(nodeInfo.id, !WindSystem.isBlockWindPassable(chunk.getBlockState(pos)) ? -2 : -1);
                    if (!trace.empty()) trace.peek().childrenModified = true;
                    continue;
                }
                int val = data.getInt(nodeInfo.id);
                trace.push(new TraceElement(nodeInfo.id));
                if (val < 0) {
                    createChildren(nodeInfo.id);
                    trace.peek().childrenModified = true;
                    val = data.getInt(nodeInfo.id);
                }
                for (int i = 0; i < 8; i++) {
                    stack.add(new NodeInfo(val + i, getChildBounds(nodeInfo.bounds, i), nodeInfo.depth + 1));
                }
            }
        }
        while (!trace.isEmpty()) {
            TraceElement element = trace.pop();
            if (trace.empty()) continue;
            if (element.childrenModified && tryCollapse(element.id) && !trace.empty()) {
                trace.peek().childrenModified = true;
            }
        }
    }
}
