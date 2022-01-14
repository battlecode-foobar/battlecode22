package foobar;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;


/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    static final int STARTUP_MINER_THRESHOLD = 10;
    /**
     * If we are in negotiation right now.
     */
    static boolean inNegotiation;
    /**
     * The relative index of the current archon relative to all archons. Guaranteed to be unique and falls in
     * [0, total archon count).
     */
    static int archonIndex;
    /**
     * Number of miners built.
     */
    static int minerCount;
    /**
     * Number of soldiers built.
     */
    static int soldierCount;
    /**
     * Number of builders built.
     */
    static int builderCount;
    /**
     * The index of the central archon.
     */
    static int centralArchonIndex;

    static Direction enemyDirection = Direction.CENTER;


    public static void init() throws GameActionException {
        inNegotiation = self.getArchonCount() > 1;
        if (!inNegotiation)
            Messaging.writeSharedLocation(Messaging.getArchonOffset(0), self.getLocation());
        minerCount = 0;
        soldierCount = 0;
        builderCount = 0;
        for (int i = Messaging.DEAD_ARCHON_START; i < Messaging.MINER_END; i++)
            self.writeSharedArray(i, Messaging.IMPOSSIBLE_LOCATION);
        // Clear the watchtower indices
        for (int i = Messaging.WATCHTOWER_START; i<Messaging.WATCHTOWER_END; i++)
            self.writeSharedArray(i, 0);
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        if (inNegotiation)
            negotiate();
        if (turnCount == initialArchonCount + 1)
            centralArchonIndex = computeCentralArchon();
        // Write global turn count.
        self.writeSharedArray(0, turnCount);

        boolean shouldForget = false;
        if (isAllNegotiationsComplete()) {
            for (int i = 0; i < initialArchonCount; i++) {
                if (Messaging.isArchonUnavailable(i))
                    continue;
                shouldForget = i == archonIndex;
                break;
            }
        }

        if (shouldForget) {
            // Probabilistic forgetting. Average lifetime of frontier message = frontier region length / constant below.
            for (int i = 0; i < 2; i++) {
                int index = Messaging.FRONTIER_START + rng.nextInt(Messaging.FRONTIER_END - Messaging.FRONTIER_START);
                self.writeSharedArray(index, Messaging.IMPOSSIBLE_LOCATION);
            }
/*
            for (int i = Messaging.MINER_START; i < Messaging.MINER_END; i++) {
                int raw = self.readSharedArray(i);
                // A trick: IMPOSSIBLE_LOCATION is defined as 1 less 1 << COORDINATE_WIDTH. So all mine locations with
                // at least 1 ttl must be > IMPOSSIBLE_LOCATION.
                if (raw > Messaging.IMPOSSIBLE_LOCATION)
                    self.writeSharedArray(i, raw - (1 << Messaging.COORDINATE_WIDTH));
            }
*/
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();

        // self.setIndicatorString("lead " + self.getTeamLeadAmount(us));

        if (self.getMode().equals(RobotMode.TURRET)) {
            // enemyDirection is defaultly center; else when frontier occurs update for the archon closest to it.
            updateProximalEnemyDirection();
            self.setIndicatorString(""+enemyDirection);
            if (shouldBuildWatchtower()){
                scheduleWatchtower();
            }
            // OPTIMIZE: if multiple options are available, should have a tie-breaker of some sort.
            if (shouldBuildMiner())
                tryBuildTowardsLowRubble(RobotType.MINER);
            else if (shouldBuildSoldier())
                tryBuildTowardsLowRubble(RobotType.SOLDIER);
            else if (shouldBuildBuilder())
                tryBuildTowardsLowRubble(RobotType.BUILDER);
            if (isAllNegotiationsComplete())
                self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.AVAILABILITY, turnCount);
        } else if (self.getMode().equals(RobotMode.PORTABLE)) {
            // System.out.println("Max soldiers: " + maxSoldierCount + " archon: " + maxSoldierCountArchon);
            MapLocation target = Messaging.getArchonLocation(centralArchonIndex);
            int distanceToTarget = self.getLocation().distanceSquaredTo(target);
            if (distanceToTarget > 25)
                PathFinding.moveTo(target);
            if (distanceToTarget <= 25 && self.canTransform())
                self.transform();
        }
        if (isAllNegotiationsComplete()) {
            Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.SOLDIER_COUNT, soldierCount);
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.MINER_COUNT, minerCount);
        }
    }

    /**
     * Checks if all archons have completed negotiation.
     * @return Whether all archons have completed negotiation.
     */
    public static boolean isAllNegotiationsComplete() {
        return turnCount > initialArchonCount;
    }

    /**
     * Checks if I should build miners.
     * @return Whether the archon should build a miner in this turn.
     */
    @SuppressWarnings("RedundantIfStatement")
    static boolean shouldBuildMiner() throws GameActionException {
        if (turnCount < 4 && minerCount < 3)
            return true;
        if (!Messaging.hasCoordinateIn(Messaging.MINER_START, Messaging.MINER_END))
            return false;
        if (Messaging.getClosestArchonTo(Messaging.MINER_START, Messaging.MINER_END, archonIndex) != archonIndex)
            return false;
        if (Messaging.getTotalMinerCount() > 7 * initialArchonCount && rng.nextDouble() > 0.125)
            return false;
        if (self.getTeamLeadAmount(us) > 300)
            return true;
        if (PathFinding.tryRetreat(RobotType.ARCHON.visionRadiusSquared, -1))
            return false;
        if (watchtowerWaitingFund())
            return false;
        return true;
    }

    /**
     * Checks if I should build soldiers.
     * @return Whether the archon should build a soldier in this turn.
     */
    @SuppressWarnings("RedundantIfStatement")
    static boolean shouldBuildSoldier() throws GameActionException {
        if (Messaging.hasCoordinateIn(Messaging.FRONTIER_START, Messaging.FRONTIER_END)) {
            if (watchtowerWaitingFund())
                return false;
            if (self.getTeamLeadAmount(us) > 300)
                return true;
            if (Messaging.getClosestArchonTo(Messaging.FRONTIER_START, Messaging.FRONTIER_END, archonIndex) != archonIndex)
                return false;
            return true;
        }
        return true;
    }

    /**
     * Checks if I should build builders.
     * @return Whether the archon should build a builder in this turn.
     */
    static boolean shouldBuildBuilder() {
        // Build a builder when we are ready to build a watchtower and there is no builder
        // return false; // Temporarily debugging...
        return builderCount == 0 && shouldBuildWatchtower();
    }

    static boolean shouldBuildWatchtower() {
        return (enemyDirection != Direction.CENTER && self.getTeamLeadAmount(us) > 300);
    }

    static void scheduleWatchtower() throws GameActionException{
        if (enemyDirection == Direction.CENTER)
            return;
        MapLocation tentativeWatchtowerLoc = self.getLocation().add(enemyDirection);
        if (!self.canSenseRobotAtLocation(tentativeWatchtowerLoc))
            Messaging.tryBroadcastTargetWatchtowerLoc(tentativeWatchtowerLoc);
    }

    /**
     * Negotiate with other archons to determine its index.
     *
     * @throws GameActionException Should not throw any exception actually.
     */
    public static void negotiate() throws GameActionException {
        if (turnCount > 0) { // If at least one turn elapsed.
            if (self.readSharedArray(turnCount) == self.getID()) {
                archonIndex = turnCount - 1;
                Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
                inNegotiation = false;
                return;
            }
        }
        self.writeSharedArray(turnCount + 1, self.getID());
    }

    /**
     * Safely sense the amount of rubble in the given direction.
     *
     * @param dir The direction to which to sense rubble.
     * @return The amount of rubble in that direction. An impractically large value is returned if the direction is
     * invalid.
     */
    static int senseRubbleSafe(Direction dir) {
        try {
            MapLocation loc = self.getLocation().add(dir);
            return self.canSenseLocation(loc) && !self.canSenseRobotAtLocation(loc)
                    ? self.senseRubble(loc) : Integer.MAX_VALUE;
        } catch (GameActionException e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Try to build a robot towards an available surrounding location with the least rubble available.
     *
     * @param type The type of robot to be built.
     * @throws GameActionException Actually it doesn't throw.
     */
    static void tryBuildTowardsLowRubble(RobotType type) throws GameActionException {
        Direction[] dirs = Arrays.copyOf(directions, directions.length);
        Arrays.sort(dirs, Comparator.comparingInt(TypeArchon::senseRubbleSafe));
        for (Direction dir : dirs) {
            if (self.canBuildRobot(type, dir)) {
                self.buildRobot(type, dir);
                switch (type) {
                    case MINER:
                        minerCount++;
                        break;
                    case SOLDIER:
                        soldierCount++;
                        break;
                    case BUILDER:
                        builderCount++;
                        break;
                }
                break;
            }
        }
    }

    /**
     * Compute and select a central archon for other archons to flee to.
     * @return The archon index of the described archon.
     * @throws GameActionException Actually doesn't throw.
     */
    static int computeCentralArchon() throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        int minDisArchon = archonIndex;
        for (int i = 0; i < initialArchonCount; i++) {
            int maxDis = 0;
            MapLocation loc = Messaging.getArchonLocation(i);
            for (int j = 0; j < initialArchonCount; j++) {
                if (i == j)
                    continue;
                maxDis = Math.max(maxDis, Messaging.getArchonLocation(j).distanceSquaredTo(loc));
            }
            if (minDis > maxDis) {
                minDis = maxDis;
                minDisArchon = i;
            }
        }
        return minDisArchon;
    }


    /**
     * Utility for archon: scans array for whether there is a watchtower which awaits fund to be built
     * If Archon wants to build watchtower then reserve 180 lead
     */
    static boolean watchtowerWaitingFund() throws GameActionException {
        for (int index = Messaging.WATCHTOWER_START; index < Messaging.WATCHTOWER_END; index++)
            // This means the last three bits encode
            // (builder claimed this watchtower) (builder arrived) (this entry is intentionally written)
            if (self.readSharedArray(index) % 8 == 7)
                return true;
        return false;
    }

    // Keep try broadcasting diagonal positions of ourselves for a watchtower
    static boolean BroadcastDiagonalPositionsForWatchtower() throws GameActionException{
        for (Direction diagDir:diagonalDirections) {
            MapLocation tentativeWatchtowerLoc = self.getLocation().add(diagDir);
            if (!self.canSenseRobotAtLocation(tentativeWatchtowerLoc))
            {
                self.setIndicatorString("Broadcasting for Turret built@"+tentativeWatchtowerLoc);
                return Messaging.tryBroadcastTargetWatchtowerLoc(tentativeWatchtowerLoc);
            }
        }
        return false;
    }

    // Check whether this is the archon closest to frontier
    // Returns CENTER if not closest (or no frontier) else direction
    // This provides a first (often accurate) estimate of where the enemy is relative to this archon
    static Direction updateProximalEnemyDirection() throws GameActionException{
        int minDistanceToFrontier = Integer.MAX_VALUE;
        int minArchonIndex = -1;
        Direction closestArchonDirToFrontier = Direction.CENTER;
        for (int archonIndex=0; archonIndex<self.getArchonCount(); archonIndex++){
            MapLocation archonLoc = Messaging.getArchonLocation(archonIndex);
            MapLocation relativeFrontierLoc = Messaging.getMostImportantFrontier(archonLoc);
            int distToFrontier = Integer.MAX_VALUE;
            if (relativeFrontierLoc != null)
                distToFrontier = archonLoc.distanceSquaredTo(relativeFrontierLoc);
            if (distToFrontier < minDistanceToFrontier){
                minDistanceToFrontier = distToFrontier;
                minArchonIndex = archonIndex;
                closestArchonDirToFrontier = archonLoc.directionTo(relativeFrontierLoc);
            }
        }
        if (minArchonIndex == archonIndex) {
            // Provide a first estimate of where the enemy is;
            // if (enemyDirection == Direction.CENTER)
            if (closestArchonDirToFrontier != Direction.CENTER)
                enemyDirection = closestArchonDirToFrontier;
            return closestArchonDirToFrontier;
        }
        return Direction.CENTER;
    }
}
