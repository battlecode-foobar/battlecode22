package foobar;

import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    /**
     * The target location the miner is trying to approach.
     */
    static MapLocation targetLocation;
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
    /**
     * All directions relative to our current position where we can try look for metals and try mine.
     */
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
        // Initialization: Scan for target and update radius
        if (firstRun()) {
            searchForTarget();
            visionRadiusSq = self.getType().visionRadiusSquared;
            actionRadiusSq = self.getType().actionRadiusSquared;
            targetLocation = null;
        }


        MapLocation here = self.getLocation();
        // Always determine whether oneself is at target
        if (atTarget && self.senseLead(here) == 0)
            // if we were previously at target then target suddenly disappeared
            targetLocation = null;

        if (targetLocation == null) {
            atTarget = false;
            self.setIndicatorString("Wandering");
        } else {
            atTarget = here.distanceSquaredTo(targetLocation) <= actionRadiusSq;
            if (atTarget)
                self.setIndicatorString("At target " + targetLocation);
            else
                self.setIndicatorString("Not at target " + targetLocation + " yet");
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
            if (!here.equals(targetLocation)) {
                if (!validTarget(targetLocation)) {
                    targetLocation = null;
                    wander();
                    searchForTarget();
                } else {
                    // go towards the current target
                    PathFinding.moveToBug0(targetLocation);
                    // If we found a better target along the way, go to it
                    searchForTarget();
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
        return targetLocation != null;
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
            targetLocation = bestLocation;
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLocation.x + "," + targetLocation.y + ")");
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
}
