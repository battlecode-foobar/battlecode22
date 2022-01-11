package vulturebot;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

import java.util.*;

/**
 * Very naive path finding.
 */
public class PathFinding extends Globals {
    static int defaultObstacleThreshold = 42;


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

    /**
     * A min-heap of Paths by their total cost.
     */
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
    static int getCostAt(MapLocation loc) throws GameActionException {
        if (self.canSenseRobotAtLocation(loc) || !self.canSenseLocation(loc))
            return 0xFFFF; // A very high value to deter the planner.
        return 1 + self.senseRubble(loc) / 10;
    }

    /**
     * Heuristics used for the A* algorithm.
     *
     * @param here The current location.
     * @param dest The destination location.
     * @return The heuristic estimate of the cost from here to dest.
     */
    static int estimateCost(MapLocation here, MapLocation dest) {
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
        MapLocation here = self.getLocation();
        if (here.distanceSquaredTo(dest) > 9 || !self.canSenseLocation(dest))
            return null;
        LocationMap memory = new LocationMap(9);
        PathHeap open = new PathHeap(30);
        Path nothing = new Path(0, estimateCost(here, dest), self.getLocation(), null);
        open.add(nothing);
        memory.put(here, nothing);
        while (!open.isEmpty()) {
            Path cur = open.poll();
            if (cur.loc.equals(dest))
                return cur;
            if (cur != memory.get(cur.loc))
                continue;
            for (Direction dir : directions) {
                MapLocation next = cur.loc.add(dir);
                if (here.distanceSquaredTo(next) > 9 || !self.canSenseLocation(dest))
                    continue;
                int newCostSoFar = cur.costSoFar + getCostAt(next);
                Path stored = (Path) memory.get(next);
                if (stored != null && stored.costSoFar <= newCostSoFar)
                    continue;
                Path newPath = new Path(newCostSoFar, estimateCost(next, dest), next, cur);
                open.add(newPath);
                memory.put(next, newPath);
            }
        }
        return null;
    }

    static final Direction[] directionsCycle = new Direction[]{
            Direction.NORTH,
            Direction.NORTHWEST,
            Direction.WEST,
            Direction.SOUTHWEST,
            Direction.SOUTH,
            Direction.SOUTHEAST,
            Direction.EAST,
            Direction.NORTHEAST,
            Direction.NORTH,
            Direction.NORTHWEST,
            Direction.WEST,
            Direction.SOUTHWEST,
            Direction.SOUTH
    };

    /**
     * Returns the three directions closest to the given angle.
     *
     * @param theta The angle relative to positive x-axis specifying an accurate direction.
     * @return An array of three directions closest to theta.
     */
    static Direction[] getDiscreteDirection3(double theta) {
        // 0.785398 is pi / 4
        int index = (int) Math.round(theta / 0.785398) + 4;
        return new Direction[]{
                directionsCycle[index + 2],
                directionsCycle[index + 1],
                directionsCycle[index + 3],
                // directionsCycle[index],
                // directionsCycle[index + 4],
        };
    }

    /**
     * Returns the five directions closest to the given angle.
     *
     * @param theta The angle relative to positive x-axis specifying an accurate direction.
     * @return An array of five directions closest to theta.
     */
    static Direction[] getDiscreteDirection5(double theta) {
        int index = (int) Math.round(theta / 0.785398) + 4;
        return new Direction[]{
                directionsCycle[index + 2],
                directionsCycle[index + 1],
                directionsCycle[index + 3],
                directionsCycle[index],
                directionsCycle[index + 4],
        };
    }

    /**
     * Returns the local-best direction to approach the dest from where the robot is in.
     *
     * @param dest The destination.
     * @return A direction locally the best for the robot to move.
     * @throws GameActionException Actually doesn't throw.
     */
    public static Direction findDirectionTo(MapLocation dest) throws GameActionException {
        MapLocation here = self.getLocation();
        double theta = Math.atan2(dest.y - here.y, dest.x - here.x);
        int minCost = Integer.MAX_VALUE;
        Direction minCostDir = null;
        for (Direction dir : getDiscreteDirection3(theta)) {
            MapLocation there = here.add(dir);
            if (!self.canSenseLocation(there))
                continue;
            int costThere = getCostAt(there);
            if (costThere < minCost) {
                minCost = costThere;
                minCostDir = dir;
            }
        }
        if (minCost == Integer.MAX_VALUE)
            return null;
        return minCostDir;
    }

    static Direction bugDirection = null;

    static void updateObstacleThreshold() throws GameActionException {
        int[] around = new int[directions.length];
        for (int i = 0; i < directions.length; i++) {
            MapLocation there = self.getLocation().add(directions[i]);
            around[i] = self.canSenseLocation(there) ? self.senseRubble(there) : Integer.MAX_VALUE;
        }
        Arrays.sort(around);
        defaultObstacleThreshold = around[1] + 16;
    }

    /**
     * Returns if we have an obstacle in the given direction.
     *
     * @param dir The direction.
     * @return If cell in the direction can be considered an obstacle.
     */
    static boolean notObstacle(Direction dir, int obstacleThreshold) throws GameActionException {
        // TODO: obstacle detection: perhaps rubble over a certain threshold?
        MapLocation there = self.getLocation().add(dir);
        for (MapLocation past : history)
            if (there.equals(past))
                return true;
        return self.senseRubble(there) <= obstacleThreshold;
    }

    static MapLocation[] history = new MapLocation[3];
    static int historyPtr = 0;

    static void addToHistory(MapLocation loc) {
        history[historyPtr++] = loc;
        historyPtr %= history.length;
    }

    /**
     * Use Bug 0 algorithm to move to the target.
     *
     * @param dest The target.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void moveToBug0(MapLocation dest, int obstacleThreshold) throws GameActionException {
        if (!self.isMovementReady())
            return;
        MapLocation here = self.getLocation();
        if (here.equals(dest))
            return;
        Direction dir = here.directionTo(dest);
        if (self.canMove(dir) && notObstacle(dir, obstacleThreshold)) {
            self.move(dir);
            bugDirection = null;
        } else {
            if (bugDirection == null)
                bugDirection = dir;
            for (int i = 0; i < 8; i++) {
                if (self.canMove(bugDirection) && notObstacle(bugDirection, obstacleThreshold)) {
                    self.move(bugDirection);
                    addToHistory(here);
                    bugDirection = bugDirection.rotateLeft();
                    break;
                } else
                    bugDirection = bugDirection.rotateRight();
            }
        }
    }

    /**
     * Use Bug 0 algorithm to move to the target.
     *
     * @param dest The target.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void moveToBug0(MapLocation dest) throws GameActionException {
        updateObstacleThreshold();
        if (!dest.equals(self.getLocation())) {
            Direction dir = findDirectionTo(dest);
            if (dir != null && self.canMove(dir)) {
                self.move(dir);
                addToHistory(self.getLocation());
                return;
            }
        }
        moveToBug0(dest, defaultObstacleThreshold);
    }

    /**
     * Randomly wander.
     *
     * @throws GameActionException Actually doesn't throw.
     */
    public static void wander() throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir))
            self.move(dir);
    }

    /**
     * Randomly wander, but avoids obstacles.
     *
     * @param threshold The robot will avoid wandering to locations with rubbles exceeding this threshold.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void wanderAvoidingObstacle(int threshold) throws GameActionException {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir) && notObstacle(dir, threshold))
            self.move(dir);
    }

    public static void spreadOut() throws GameActionException {
        updateObstacleThreshold();
        MapLocation here = self.getLocation();
        RobotInfo[] botsAround = self.senseNearbyRobots();
        double x = 0, y = 0;
        for (RobotInfo bot : botsAround) {
            MapLocation loc = bot.getLocation();
            double denom = Math.sqrt(loc.distanceSquaredTo(here));
            denom *= loc.distanceSquaredTo(here);
            x -= (loc.x - here.x) / denom;
            y -= (loc.y - here.y) / denom;
        }
        double theta = Math.atan2(y, x);
        Direction[] candidates = getDiscreteDirection5(theta);
        Direction dir = candidates[rng.nextInt(candidates.length)];
        if (self.canMove(dir) && notObstacle(dir, defaultObstacleThreshold))
            self.move(dir);
    }
}
