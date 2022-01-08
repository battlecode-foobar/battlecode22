package foobar;

import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    // the default (-1, -1) is (equivalent to saying that there's no target yet)
    static MapLocation targetLocation = new MapLocation(-1, -1);
    static int[][] delta = {{0, 0}, {0, -1}, {0, 1}, {1, 0}, {-1, 0}, {1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
    static int visionRadiusSq = 0;
    static int actionRadiusSq = 0;
    static Boolean atTarget = false;

    public static void step() throws GameActionException {
        // Always determine whether oneself is at target
        System.out.println(here);
        if (atTarget && self.senseLead(here) == 0)
            // if we were previously at target then target suddenly disappeared
            targetLocation = new MapLocation(-1, -1);
        atTarget = (here.x == targetLocation.x && here.y == targetLocation.y);
        if (atTarget)
            self.setIndicatorString("At target");
        else if (targetLocation.x == -1 && targetLocation.y == -1)
            self.setIndicatorString("Wandering");
        else if (here.x != targetLocation.x || here.y != targetLocation.y)
            self.setIndicatorString("Not at target ("+targetLocation.x+","+targetLocation.y+") yet");
        else
            self.setIndicatorString("I don't know what I'm doing");

        // Initialization: Scan for target and update radius
        if (firstRun()) {
            searchForTarget();
            visionRadiusSq = self.getType().actionRadiusSquared;
            actionRadiusSq = self.getType().actionRadiusSquared;
        }

        // Try to mine on squares around us.
        for (int i=0; i<9; i++)
        {
            MapLocation mineLocation = new MapLocation(here.x + delta[i][0],
                    here.y + delta[i][1]);
            // Notice that the Miner's action cooldown is very low.
            // You can mine multiple times per turn!
            while (self.canMineGold(mineLocation)) {
                self.mineGold(mineLocation);
            }
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) >= 16) {
                self.mineLead(mineLocation);
            }
        }

        // Main body of each turn
        if (hasTarget()) {
            // If we have a target, we have either reached it or not
            // We don't need to do anything if we are at the target (we'll just continue mining by default)
            if (here.x != targetLocation.x || here.y != targetLocation.y) {
                // go towards the current target
                PathFinding.moveToBug0(targetLocation);
                // If we found a better target along the way, go to it
                searchForTarget();
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
        MapLocation[] locations = self.getAllLocationsWithinRadiusSquared(here, visionRadiusSq);
        MapLocation bestLocation = new MapLocation(-1, -1);
        int mostLead = 0;
        int leadAtLoc = 0;

        for (MapLocation loc : locations) {
            if (self.canSenseLocation(loc)) {
                leadAtLoc = self.senseLead(loc);
                if (leadAtLoc > mostLead) {
                    boolean validTarget = true;
                    // Heuristic: The periphery of our target shouldn't contain any bots
                    for (int i=0; i<9; i++){
                        MapLocation targetNeighbor = new MapLocation(loc.x + delta[i][0],
                                loc.y + delta[i][1]);
                        if (self.canSenseRobotAtLocation(targetNeighbor)){
                            validTarget = false;
                            break;
                        }
                    }
                    if (validTarget) {
                        mostLead = leadAtLoc;
                        bestLocation = loc;
                    }
                }
            }
        }

        if (mostLead > 0) {
            targetLocation = bestLocation;
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLocation.x + "," + targetLocation.y + ")");
        }
    }

    static void goToTarget() throws GameActionException {
        wander();
    }

    static void wander() throws GameActionException {
        // OPTIMIZE: a better direction distribution.
        RobotInfo[] neighborBots = self.senseNearbyRobots(here, visionRadiusSq, us);

        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir)) {
            self.move(dir);
        }
    }
}
