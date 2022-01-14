package foobar;

import battlecode.common.*;

import java.util.*;

/**
 * Very naive path finding.
 */
public class PathFinding extends Globals {
    static int defaultObstacleThreshold = 3;

    final static int INFINITY = 0xFFFF;

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
     */
    static int getCostAt(MapLocation loc) {
        if (!self.canSenseLocation(loc) || self.canSenseRobotAtLocation(loc))
            return INFINITY; // A very high value to deter the planner.
        try {
            return 1 + self.senseRubble(loc) / 10;
        } catch (GameActionException e) {
            return INFINITY;
        }
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
     */
    public static Path findPathWithAStar(MapLocation dest) {
        MapLocation here = self.getLocation();
        if (here.distanceSquaredTo(dest) > 9 || !self.canSenseLocation(dest))
            return null;
        LocationMap memory = new LocationMap(self.getLocation(), 9);
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
        int index = (int) Math.round(theta / 0.785398) + 4;
        return new Direction[]{
                directionsCycle[index + 2],
                directionsCycle[index + 1],
                directionsCycle[index + 3],
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
     * Gets the accurate direction to from the current location to the given destination in radians.
     *
     * @param dest The destination.
     * @return The direction from the current location to dest, in radians, relative to the positive x-axis.
     */
    public static double getTheta(MapLocation dest) {
        MapLocation here = self.getLocation();
        return Math.atan2(dest.y - here.y, dest.x - here.x);
    }

    /**
     * Returns the local-best direction to approach the dest from where the robot is in.
     *
     * @param theta The destination angle.
     * @return A direction locally the best for the robot to move.
     */
    public static Direction findDirectionTo(double theta) {
        int minCost = INFINITY;
        Direction minCostDir = null;
        MapLocation here = self.getLocation();
        for (Direction dir : getDiscreteDirection5(theta)) {
            MapLocation there = here.add(dir);
            if (!self.canSenseLocation(there))
                continue;
            // A wise bot should not repeat its mistake twice.
            if (isInHistory(there) > 0)
                continue;
            int multiplier = 2;
            if (here.distanceSquaredTo(there) == 2
                    && Math.cos(theta) * dir.getDeltaX() + Math.sin(theta) * dir.getDeltaY() > 1.01) {
                multiplier = 1;
            }
            int costThere = multiplier * getCostAt(there);
            if (costThere < minCost) {
                minCost = costThere;
                minCostDir = dir;
            }
        }
        if (minCost == INFINITY)
            return null;
        return minCostDir;
    }

    static Direction bugDirection = null;
    static boolean rotatingLeft = false;

    /**
     * Adaptively update the obstacle threshold for path finding algorithms.
     */
    static void updateObstacleThreshold() {
        int[] around = new int[directions.length];
        for (int i = 0; i < directions.length; i++)
            around[i] = getCostAt(self.getLocation().add(directions[i]));
        Arrays.sort(around);
        defaultObstacleThreshold = around[1] + 1;
        self.setIndicatorString("threshold: " + defaultObstacleThreshold);
    }

    /**
     * Adaptively update the obstacle threshold for path finding algorithms.
     *
     * @param theta A reference direction.
     */
    static void updateObstacleThreshold(double theta) {
        int minCost = INFINITY;
        int maxCost = 0;
        for (Direction dir : getDiscreteDirection5(theta)) {
            int costThere = getCostAt(self.getLocation().add(dir));
            if (costThere < INFINITY) {
                minCost = Math.min(minCost, costThere);
                maxCost = Math.max(maxCost, costThere);
            }
        }
        defaultObstacleThreshold = Math.max(minCost + 1, maxCost - 3);
    }

    /**
     * Checks if the given location was visited recently.
     *
     * @param loc The location to be checked.
     * @return The number of occurrence of the given location in the recently visited history.
     */
    static int isInHistory(MapLocation loc) {
        int ret = 0;
        for (MapLocation past : history)
            if (loc.equals(past))
                ret++;
        return ret;
    }

    /**
     * Returns if we have an obstacle in the given direction.
     *
     * @param dir The direction.
     * @return If cell in the direction can be considered an obstacle.
     */
    static boolean notObstacle(Direction dir, int obstacleThreshold) {
        MapLocation there = self.getLocation().add(dir);
        return getCostAt(there) <= obstacleThreshold;
    }

    static MapLocation[] history = new MapLocation[3];
    static int historyPtr = 0;

    /**
     * Adds a location to movement history.
     *
     * @param loc The location to be added.
     */
    static void addToHistory(MapLocation loc) {
        history[historyPtr++] = loc;
        historyPtr %= history.length;
    }

    /**
     * Tries to move in a direction. This is a safe wrapper that doesn't throw any exceptions that would possibly
     * disrupt the flow of the caller.
     *
     * @param dir The direction.
     * @return If movement succeeds.
     */
    public static boolean tryMove(Direction dir) {
        try {
            if (self.canMove(dir)) {
                self.move(dir);
                return true;
            }
            return false;
        } catch (GameActionException e) {
            return false;
        }
    }

    /**
     * Use Bug 0 algorithm to move to the target.
     *
     * @param dest The target.
     */
    static void moveToBug0(MapLocation dest, int obstacleThreshold) {
        if (!self.isMovementReady())
            return;
        MapLocation here = self.getLocation();
        if (here.equals(dest))
            return;
        Direction dir = here.directionTo(dest);
        if (notObstacle(dir, obstacleThreshold) && tryMove(dir)) {
            bugDirection = null;
        } else {
            if (bugDirection == null) {
                bugDirection = dir;
                // rotatingLeft = rng.nextBoolean();
                rotatingLeft = !rotatingLeft;
            }
            for (int i = 0; i < 8; i++) {
                if (notObstacle(bugDirection, obstacleThreshold) && tryMove(bugDirection)) {
                    addToHistory(here);
                    bugDirection = rotatingLeft ? bugDirection.rotateLeft() : bugDirection.rotateRight();
                    break;
                } else
                    bugDirection = rotatingLeft ? bugDirection.rotateRight() : bugDirection.rotateLeft();
            }
        }
    }

    /**
     * Use Bug 0 algorithm to move to the target.
     *
     * @param dest The target.
     */
    public static void moveTo(MapLocation dest) {
        MapLocation here = self.getLocation();
        if (!dest.equals(here)) {
            updateObstacleThreshold(getTheta(dest));
            // Path finding is a lie!
            Direction dir = dest.distanceSquaredTo(here) <= 2 ? here.directionTo(dest)
                    : findDirectionTo(getTheta(dest));
            if (dir != null) {
                if (tryMove(dir))
                    addToHistory(here);
            }
        }
        // :w
    }

    /**
     * Randomly wander.
     */
    public static void wander() {
        Direction dir = directions[rng.nextInt(directions.length)];
        tryMove(dir);
    }

    /**
     * Randomly wander, but avoids obstacles.
     *
     * @param threshold The robot will avoid wandering to locations with rubbles exceeding this threshold.
     */
    public static void wanderAvoidingObstacle(int threshold) {
        Direction dir = directions[rng.nextInt(directions.length)];
        if (notObstacle(dir, threshold))
            tryMove(dir);
    }

    /**
     * Spread out.
     */
    public static void spreadOut() {
        // updateObstacleThreshold();
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
        if (notObstacle(dir, defaultObstacleThreshold))
            tryMove(dir);
    }

    /**
     * A wise general cuts losses, and regroup.
     *
     * @param radius The radius at which you want to start to retreat.
     */
    public static boolean tryRetreat(int radius, int confidence) {
        MapLocation here = self.getLocation();
        RobotInfo[] botsAround = self.senseNearbyRobots(13, us);
        for (RobotInfo bot : botsAround)
            confidence += evaluatePower(bot);
        double x = 0, y = 0;
        boolean impendingDoom = false;
        botsAround = self.senseNearbyRobots(radius, them);
        for (RobotInfo bot : botsAround) {
            if (evaluatePower(bot) != 0) {
                impendingDoom = true;
                confidence -= evaluatePower(bot);
                MapLocation loc = bot.getLocation();
                double denom = Math.sqrt(loc.distanceSquaredTo(here));
                denom *= loc.distanceSquaredTo(here);
                x -= (loc.x - here.x) / denom;
                y -= (loc.y - here.y) / denom;
            }
        }
        // self.setIndicatorString("retreating confidence " + confidence + " doom? " + impendingDoom);
        if (impendingDoom && confidence < 0) {
            // The sin is not in being outmatched, but in failing to recognize it.
            double theta = Math.atan2(y, x);
            Direction dir = findDirectionTo(theta);
            if (dir != null)
                tryMove(dir);
            return true;
        }
        return false;
    }
}
