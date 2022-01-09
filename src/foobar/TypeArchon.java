package foobar;

import battlecode.common.*;

import java.util.Arrays;


/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    /**
     * How many bytes an archon can use in the shared array.
     */
    public static final int ARCHON_SPACE = 4;

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
     * Offset into shared array.
     */
    static int sharedOffset;


    public static void init() throws GameActionException {
        inNegotiation = self.getArchonCount() > 1;
        minerCount = 0;
        soldierCount = 0;
        builderCount = 0;
        /*
        int before = Clock.getBytecodesLeft();
        MapLocation target = null;
        MapLocation[] potentialMines = self.getAllLocationsWithinRadiusSquared(self.getLocation(), Integer.MAX_VALUE);
        for (MapLocation potentialMine : potentialMines) {
            if (!self.canSenseLocation(potentialMine))
                continue;
            if (self.senseLead(potentialMine) > 0) {
                target = potentialMine;
                System.out.println("found lead at " + potentialMine);
                break;
            }
        }
        System.out.println("Lead finding took " + (before - Clock.getBytecodesLeft()));
        before = Clock.getBytecodesLeft();
        PathFinding.findPathWithAStar(target);
        System.out.println("Path finding took " + (before - Clock.getBytecodesLeft()));*/
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        if (inNegotiation)
            negotiate();
        self.setIndicatorString("lead: " + self.getTeamLeadAmount(us));
        // This mostly the same as the lecture player.
        if (minerCount < 8) {
            tryBuildTowardsLowRubble(RobotType.MINER);
        } else if (soldierCount < 10) {
            tryBuildTowardsLowRubble(RobotType.SOLDIER);
        } else if (builderCount < 1) {
            tryBuildTowardsLowRubble(RobotType.BUILDER);
        } else if (minerCount < soldierCount * 9 / 10 && self.getTeamLeadAmount(us) < 5000) {
            tryBuildTowardsLowRubble(RobotType.MINER);
        } else if (builderCount < soldierCount / 30) {
            tryBuildTowardsLowRubble(RobotType.BUILDER);
        } else {
            tryBuildTowardsLowRubble(RobotType.SOLDIER);
        }
    }

    /**
     * Negotiate with other archons to determine its index.
     *
     * @throws GameActionException Should not throw any exception actually.
     */
    public static void negotiate() throws GameActionException {
        if (turnCount > 0) { // If at least one turn elapsed.
            if (self.readSharedArray(turnCount - 1) == self.getID()) {
                archonIndex = turnCount - 1;
                log("Negotiate complete! I get index of " + archonIndex);
                sharedOffset = archonIndex * ARCHON_SPACE;
                self.writeSharedArray(sharedOffset, Messaging.encodeLocation(self.getLocation()));
                inNegotiation = false;
                return;
            }
        }
        self.writeSharedArray(turnCount, self.getID());
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
            MapLocation thisArchon = Messaging.decodeLocation(self.readSharedArray(i * ARCHON_SPACE));
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
        Arrays.sort(dirs, (a, b) -> senseRubbleSafe(a) - senseRubbleSafe(b));
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
