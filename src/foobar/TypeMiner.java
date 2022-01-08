package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    // the default (-1, -1) is (equivalent to saying that there's no target yet)
    static MapLocation targetLocation = new MapLocation(-1, -1);
    static MapLocation selfLocation = new MapLocation(-1, -1);
    static int visionRadiusSq = 0;
    static Boolean atTarget = false;

    public static void step() throws GameActionException {
        // Always ascertain own position at the start of turn
        selfLocation = self.getLocation();

        // Always determine whether oneself is at target
        // We are atTarget if: we are physically there AND the target is still there (e.g. during Vortex)
        atTarget = (selfLocation.x == targetLocation.x && selfLocation.y == targetLocation.y &&
                self.senseLead(selfLocation) > 0);

        // Initialization: Scan for target and update radius
        if (firstRun()) {
            searchForTarget();
            visionRadiusSq = self.getType().actionRadiusSquared;
        }

        // Regardless of what it is doing, a miner should always try to mine lead mines around itself sustainably
        MapLocation[] surroundLocs = {
                new MapLocation(selfLocation.x - 1, selfLocation.y - 1),
                new MapLocation(selfLocation.x - 1, selfLocation.y),
                new MapLocation(selfLocation.x - 1, selfLocation.y + 1),
                new MapLocation(selfLocation.x, selfLocation.y - 1),
                new MapLocation(selfLocation.x, selfLocation.y),
                new MapLocation(selfLocation.x, selfLocation.y + 1),
                new MapLocation(selfLocation.x + 1, selfLocation.y - 1),
                new MapLocation(selfLocation.x + 1, selfLocation.y),
                new MapLocation(selfLocation.x + 1, selfLocation.y + 1)
        };
        /*
        while (self.isActionReady()){
            for (MapLocation targetLoc: surroundLocs){
                if (self.canSenseLocation(targetLoc) && self.senseLead(targetLoc) >= 10 && self.canMineLead(targetLoc))
                   self.mineLead(targetLoc);
            }
        }*/

        // Main body of each turn
        if (hasTarget()) {
            // If we have a target, we have either reached it or not
            // We don't need to do anything if we are at the target (we'll just continue mining by default)
            if (selfLocation.x != targetLocation.x || selfLocation.y != targetLocation.y) {
                log("Going for target!");
                // go towards the current target
                goToTarget();
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
        return targetLocation.x != -1 && targetLocation.y != -1;
    }

    static void searchForTarget() throws GameActionException {
        MapLocation[] locations = self.getAllLocationsWithinRadiusSquared(selfLocation, visionRadiusSq);
        log("" + locations.length);
        int mostLead = 0;
        int leadAtLoc = 0;
        for (MapLocation loc : locations) {
            if (self.canSenseLocation(loc)) {
                leadAtLoc = self.senseLead(loc);
                if (leadAtLoc > mostLead) {
                    mostLead = leadAtLoc;
                    locations[0] = loc;
                }
            }
        }
        if (mostLead > 0) {
            targetLocation = locations[0];
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLocation.x + "," + targetLocation.y + ")");
        }
    }

    static void goToTarget() throws GameActionException {
        wander();
    }

    static void wander() throws GameActionException {
        // OPTIMIZE: a better direction distribution.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir)) {
            self.move(dir);
        }
    }
}
