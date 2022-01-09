package foobar;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;


/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
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


    public static void init() throws GameActionException {
        inNegotiation = self.getArchonCount() > 1;
        if (!inNegotiation)
            Messaging.writeSharedLocation(Messaging.getArchonOffset(0), self.getLocation());
        minerCount = 0;
        soldierCount = 0;
        builderCount = 0;
        for (int i = Messaging.DEAD_ARCHON_START; i < Messaging.DEAD_ARCHON_END; i++)
            self.writeSharedArray(i, Messaging.IMPOSSIBLE_LOCATION);
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();

        if (inNegotiation)
            negotiate();
        self.writeSharedArray(0, turnCount);

        if (rng.nextDouble() <= 1.0 / self.getArchonCount()) {
            for (int i = 0; i < 3; i++) {
                int index = Messaging.FRONTIER_START + rng.nextInt(Messaging.FRONTIER_END - Messaging.FRONTIER_START);
                self.writeSharedArray(index, Messaging.IMPOSSIBLE_LOCATION);
            }
        }
        Messaging.reportAllEnemiesAround();

        int minSoldierCount = Integer.MAX_VALUE;
        int minMinerCount = Integer.MAX_VALUE;
        int totalMinerCount = 0;
        for (int i = 0; i < initialArchonCount; i++) {
            minSoldierCount = Math.min(minSoldierCount, Messaging.getArchonSoldierCount(i));
            minMinerCount = Math.min(minMinerCount, Messaging.getArchonMinerCount(i));
            totalMinerCount += Messaging.getArchonMinerCount(i);
        }

        // self.setIndicatorString("lead: " + self.getTeamLeadAmount(us));
        // This mostly the same as the lecture player.
        if (minerCount < 8) {
            tryBuildTowardsLowRubble(RobotType.MINER);
        } else if (soldierCount < 10) {
            tryBuildTowardsLowRubble(RobotType.SOLDIER);
        } else if (builderCount < 1) {
            tryBuildTowardsLowRubble(RobotType.BUILDER);
        } else if (minerCount < soldierCount * 5 / 10 && self.getTeamLeadAmount(us) < 5000) {
            tryBuildTowardsLowRubble(RobotType.MINER);
        } else if (builderCount < soldierCount / 30) {
            tryBuildTowardsLowRubble(RobotType.BUILDER);
        } else {
            self.setIndicatorString("min soldier count: " + minSoldierCount + " my soldier: " + soldierCount);
            if (soldierCount <= minSoldierCount + 100)
                tryBuildTowardsLowRubble(RobotType.SOLDIER);
        }

        if (turnCount > initialArchonCount) {
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.SOLDIER_COUNT, soldierCount);
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.MINER_COUNT, minerCount);
        }
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
                log("Negotiate complete! I get index of " + archonIndex);
                Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
                inNegotiation = false;
                return;
            }
        }
        self.writeSharedArray(turnCount + 1, self.getID());
    }

    /**
     * To be called by other type of droids and queries who built itself.
     *
     * @return The archon index of the archon that built the droid.
     * @throws GameActionException Actually doesn't throw.
     */
    public static int whoBuiltMe() throws GameActionException {
        MapLocation here = self.getLocation();
        for (int i = 0; i < self.getArchonCount(); i++) {
            MapLocation thisArchon = Messaging.readSharedLocation(self.readSharedArray(Messaging.getArchonOffset(i)));
            if (thisArchon.isAdjacentTo(here)) {
                return i;
            }
        }
        // Should be unreachable;
        return 0;
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
            return self.canSenseLocation(loc) ? self.senseRubble(loc) : Integer.MAX_VALUE;
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
}
