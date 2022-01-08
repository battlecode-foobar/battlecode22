package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.*;

/**
 * Very naive path finding.
 */
public class PathFinding extends Globals {
    /**
     * A path.
     */
    public static class Path implements Comparable<Path> {
        int costSoFar;
        int costEstimate;
        MapLocation loc;
        Path precursor;

        /**
         * Creates a new path.
         *
         * @param costSoFar    The determined cost so far.
         * @param costEstimate The future cost estimated using heuristics.
         * @param loc          The last location of this path.
         * @param precursor    The precursor of this path, which is a path containing everything but the end location.
         */
        public Path(int costSoFar, int costEstimate, MapLocation loc, Path precursor) {
            this.costSoFar = costSoFar;
            this.costEstimate = costEstimate;
            this.loc = loc;
            this.precursor = precursor;
        }

        /**
         * Gets the total cost of the path.
         *
         * @return The total cost, i.e., the determined and estimated cost combined.
         */
        int getTotalCost() {
            return costSoFar + costEstimate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Path path = (Path) o;
            return loc.equals(path.loc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loc);
        }

        @Override
        public int compareTo(Path another) {
            // return another.costSoFar + another.costEstimate - costSoFar - costEstimate;
            return costSoFar + costEstimate - another.costSoFar - another.costEstimate;
        }
    }

    static class PathHeap {
        Path[] heap;
        int n;

        /**
         * Constructs a path heap with the given capacity.
         *
         * @param capacity The max capacity of the constructed heap.
         */
        PathHeap(int capacity) {
            heap = new Path[capacity + 1];
            n = 0;
        }

        /**
         * Checks if the heap is empty.
         *
         * @return If the heap contains no elements.
         */
        boolean isEmpty() {
            return n == 0;
        }

        /**
         * Pops the least-cost path off the heap.
         *
         * @return The least-cost path.
         */
        Path poll() {
            Path ret = heap[1];
            heap[1] = heap[n];
            heap[n--] = null;
            int cur = 1;
            while (cur * 2 <= n) {
                int sch = cur * 2; // smaller child.
                if (sch + 1 <= n && heap[sch + 1].getTotalCost() < heap[sch].getTotalCost())
                    sch++;
                if (heap[cur].getTotalCost() < heap[sch].getTotalCost())
                    break;
                Path temp = heap[cur];
                heap[cur] = heap[sch];
                heap[cur = sch] = temp;
            }
            return ret;
        }

        /**
         * Pushes a path into the heap.
         *
         * @param p The path to be pushed into the heap.
         */
        void add(Path p) {
            heap[++n] = p;
            int cur = n;
            while (cur > 1 && heap[cur].getTotalCost() < heap[cur / 2].getTotalCost()) {
                Path temp = heap[cur];
                heap[cur] = heap[cur / 2];
                heap[cur / 2] = temp;
                cur /= 2;
            }
        }
    }

    /**
     * Cost of a single location according to the spec.
     *
     * @param loc The location.
     * @return The cost of that location.
     * @throws GameActionException If the location can't be sensed by the robot.
     */
    static int costOf(MapLocation loc) throws GameActionException {
        return 1 + self.senseRubble(loc) / 10;
    }

    /**
     * Heuristics used for the A* algorithm.
     *
     * @param here The current location.
     * @param dest The destination location.
     * @return The heuristic estimate of the cost from here to dest.
     */
    static int heuristic(MapLocation here, MapLocation dest) {
        // For now manhattan distance looks like a pretty good approximation.
        return Math.max(Math.abs(here.x - dest.x), Math.abs(here.y - dest.y)) - 1;
    }

    /**
     * Finds a path to the given destination using A*.
     * This function is already heavily optimized, but it is still very slow.
     *
     * @param dest The destination, must be within vision range of the robot.
     * @return The least-cost path, or null if the given location is out-of-range.
     * @throws GameActionException Actually it doesn't throw.
     */
    public static Path findPathWithAStar(MapLocation dest) throws GameActionException {
        if (!self.canSenseLocation(dest))
            return null;
        MapLocation src = self.getLocation();
        LocationMap memory = new LocationMap(self.getType().visionRadiusSquared);
        PathHeap open = new PathHeap(3 * self.getType().visionRadiusSquared);
        Path nothing = new Path(0, heuristic(src, dest), self.getLocation(), null);
        open.add(nothing);
        memory.put(src, nothing);
        while (!open.isEmpty()) {
            Path cur = open.poll();
            if (cur.loc.equals(dest))
                return cur;
            if (cur != memory.get(cur.loc))
                continue;
            for (Direction dir : directions) {
                MapLocation next = cur.loc.add(dir);
                if (!self.canSenseLocation(next))
                    continue;
                int newCostSoFar = cur.costSoFar + costOf(next);
                Path stored = (Path) memory.get(next);
                if (stored != null && stored.costSoFar <= newCostSoFar)
                    continue;
                Path newPath = new Path(newCostSoFar, heuristic(next, dest), next, cur);
                open.add(newPath);
                memory.put(next, newPath);
            }
        }
        return null;
    }
}
