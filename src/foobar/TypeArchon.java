package foobar;

import battlecode.common.*;

/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    static State state;
    static int archonIndex = 0;

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
        System.out.println(before - Clock.getBytecodesLeft());
        before = Clock.getBytecodesLeft();
        PathFinding.findPathTo(target);
        System.out.println("PathFinding took " + (before - Clock.getBytecodesLeft()));
*/
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        switch (state) {
            case NEGOTIATING:
                negotiate();
                break;
            case BUILDING_MINER:
                tryBuildMiner();
                break;
        }
    }

    /**
     * Negotiate with other archons to determine its index.
     * @throws GameActionException Should not throw any exception actually.
     */
    public static void negotiate() throws GameActionException {
        if (turnCount > 0) {
            if (self.readSharedArray(turnCount - 1) == self.getID()) {
                archonIndex = turnCount - 1;
                state = State.BUILDING_MINER; // Exit negotiation.
                return;
            }
        }
        self.writeSharedArray(turnCount, self.getID());
    }

    /**
     * Try build a miner around self.
     */
    public static void tryBuildMiner() throws GameActionException {
        for (Direction dir : directions) {
            if (self.canBuildRobot(RobotType.MINER, dir)) {
                self.buildRobot(RobotType.MINER, dir);
                break;
            }
        }

    }
}
