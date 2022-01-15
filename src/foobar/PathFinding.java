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

    static MapLocation[] history = new MapLocation[8];
    static MapLocation lastTarget = null;
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
     * Clears the movement history.
     */
    static void clearHistory() {
        Arrays.fill(history, null);
        historyPtr = 0;
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
    public static boolean moveTo(MapLocation dest) {
        MapLocation here = self.getLocation();
        if (!dest.equals(here)) {
            if (lastTarget == null || dest.distanceSquaredTo(lastTarget) > 10) {
                clearHistory();
                lastTarget = dest;
            }
            updateObstacleThreshold(getTheta(dest));
            // Path finding is a lie!
            Direction dir = dest.distanceSquaredTo(here) <= 2 ? here.directionTo(dest)
                    : findDirectionTo(getTheta(dest));
            if (dir != null) {
                if (tryMove(dir)) {
                    addToHistory(here);
                    return true;
                }
            }
        }
        return false;
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
    @SuppressWarnings("DuplicatedCode")
    public static boolean tryRetreat(int radius, int confidence) {
        MapLocation here = self.getLocation();
        RobotInfo[] botsAround = self.senseNearbyRobots(13, us);
        for (RobotInfo bot : botsAround)
            confidence += FireControl.evaluatePower(bot);
        double x = 0, y = 0;
        boolean impendingDoom = false;
        botsAround = self.senseNearbyRobots(radius, them);
        for (RobotInfo bot : botsAround) {
            int power = FireControl.evaluatePower(bot);
            if (power != 0) {
                impendingDoom = true;
                confidence -= power;
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
            // Nobody cares about history when you are running.
            clearHistory();
            Direction dir = findDirectionTo(theta);
            if (dir != null)
                tryMove(dir);
            return true;
        }
        return false;
    }
}
