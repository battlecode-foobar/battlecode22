package foobar;

import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    /**
     * The target location the miner is trying to approach.
     */
    static MapLocation targetLoc;
    /**
     * The cached vision radius.
     */
    static int visionRadiusSq = 0;
    /**
     * The cached action radius.
     */
    static int actionRadiusSq = 0;
    /**
     * If we are at our target.
     */
    static Boolean atTarget = false;

    static int minerIDatTarget = null;
    /**
     * All directions relative to our current position where we can try look for metals and try mine.
     */
    /**
     * A miner will not mine lead beneath this threshold
     */
    static final int sustainableLeadThreshold = 12;
    static Direction[] canTryMine = {
            Direction.CENTER,
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    public static void step() throws GameActionException {

        /** How the miner works
         *  Mining: try to mine gold, then lead, around itself
         *  Movement:
         *      If it has a target (a lead mine without another miner on top) then go for it using bug0
         *      Without a target, a miner wanders
         */

        // Initialization: Scan for target and update radius
        if (firstRun()) {
            searchForTarget();
            visionRadiusSq = self.getType().visionRadiusSquared;
            actionRadiusSq = self.getType().actionRadiusSquared;
            targetLoc = null;
        }


        MapLocation here = self.getLocation();
        // Always determine whether oneself is at target
        if (atTarget && self.senseLead(here) == 0)
            // if we were previously at target then target suddenly disappeared
            targetLoc = null;

        if (targetLoc == null) {
            atTarget = false;
            self.setIndicatorString("Wandering");
        } else {
            atTarget = here.equals(targetLoc);
            if (atTarget)
                self.setIndicatorString("At target " + targetLoc);
            else
                self.setIndicatorString("Not at target " + targetLoc + " yet");
        }

        // Try to mine on squares around us.
        for (Direction dir : canTryMine) {
            MapLocation mineLocation = here.add(dir);
            // Notice that the Miner's action cool down is very low.
            // You can mine multiple times per turn!
            while (self.canMineGold(mineLocation))
                self.mineGold(mineLocation);
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) >= 16)
                self.mineLead(mineLocation);
        }

        // Main body of each turn
        if (hasTarget()) {
            // If we have a target, we have either reached it or not
            // We don't need to do anything if we are at the target (we'll just continue mining by default)
            if (!here.equals(targetLoc)) {
                if (!validTarget(targetLoc)) {
                    targetLoc = null;
                    wander();
                    searchForTarget();
                } else {
                    // go towards the current target
                    PathFinding.moveToBug0(targetLoc);
                    // If we found a better target along the way, go to it
                    searchForTarget();
                    return;
                }
            }
        } else {
            // If we don't even have a target, then wander around
            // TODO: wander in the general direction where there are no other robots
            wander();
            searchForTarget();
        }
    }

    // returns whether the robot has a target
    public static boolean hasTarget() {
        return targetLoc != null;
    }

    static void searchForTarget() throws GameActionException {
        MapLocation[] candidates = self.getAllLocationsWithinRadiusSquared(self.getLocation(), visionRadiusSq);
        MapLocation bestLocation = null;
        int mostLead = 0;

        for (MapLocation loc : candidates) {
            int leadAtLoc = self.senseLead(loc);
            if (leadAtLoc > mostLead) {
                // Heuristic: The periphery of our target shouldn't contain any bots
                if (validTarget(loc)) {
                    mostLead = leadAtLoc;
                    bestLocation = loc;
                }
            }
        }

        if (mostLead > 0) {
            targetLoc = bestLocation;
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLoc.x + "," + targetLoc.y + ")");
        }
    }

    static boolean validTarget(MapLocation loc) {
        if (loc == null)
            return false;
        int neighborRobotsCount = 0;
        MapLocation here = self.getLocation();
        for (Direction dir : canTryMine) {
            MapLocation targetNeighbor = here.add(dir);
            if (self.canSenseRobotAtLocation(targetNeighbor)) {
                // A target is an immediate no-no if there's another robot squatting on it
                if (loc.equals(targetNeighbor))
                    return false;
                neighborRobotsCount++;
            }
        }
        return neighborRobotsCount < 4;
    }

    static void wander() throws GameActionException {
        // OPTIMIZE: a better direction distribution.
        RobotInfo[] neighborBots = self.senseNearbyRobots(self.getLocation(), visionRadiusSq, us);

        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir)) {
            self.move(dir);
        }
    }

    static void tryMineResources() throws GameActionException {
        // Try to mine on squares around us.
        for (Direction dir : canTryMine) {
            MapLocation mineLocation = self.getLocation().add(dir);
            // Notice that the Miner's action cool down is very low.
            // You can mine multiple times per turn!
            while (self.canMineGold(mineLocation))
                self.mineGold(mineLocation);
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) >= sustainableLeadThreshold)
                self.mineLead(mineLocation);
        }
    }
}
