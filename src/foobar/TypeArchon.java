package foobar;

import battlecode.common.*;

/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    /**
     * How many bytes an archon can use in the shared array.
     */
    public static final int ARCHON_SPACE = 4;
    /**
     * The maximum number of miners this archon is going to make.
     */
    public static final int MAX_MINER = 8;

    static State state;
    static int archonIndex;
    static int minerCount = 0;
    /**
     * Offset into shared array.
     */
    static int sharedOffset;

    enum State {
        NEGOTIATING,
        BUILDING_MINER,
    }

    public static void init() throws GameActionException {
        if (self.getArchonCount() > 1) // Enter a negotiation to determine the order of itself.
            state = State.NEGOTIATING;
        else
            state = State.BUILDING_MINER;
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
        switch (state) {
            case NEGOTIATING:
                negotiate();
                tryBuildMiner();
                // what if we simply don't break?
                break;
            case BUILDING_MINER:
                tryBuildMiner();
                break;
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
                state = State.BUILDING_MINER; // Exit negotiation.
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
     * Try build a miner around self.
     */
    public static void tryBuildMiner() throws GameActionException {
        for (Direction dir : directions) {
            if (self.canBuildRobot(RobotType.MINER, dir) && minerCount <= MAX_MINER) {
                self.buildRobot(RobotType.MINER, dir);
                minerCount++;
                break;
            }
        }
    }
}
