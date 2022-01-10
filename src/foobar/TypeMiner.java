package foobar;

import battlecode.common.*;

import java.awt.*;
import java.util.Map;

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
    /**
     * Record the miner ID at target (if there is any)
     */
    static int minerIDatTarget = -1;

    static RobotInfo nullRobotInfo = new RobotInfo(-1, us, RobotType.MINER,
            RobotMode.DROID, 0, 0, new MapLocation(-1, -1));

    static RobotInfo[] previousRobotsNearTarget = new RobotInfo[9];
    static int[] previousRobotsNearTargetStayTurnCount = new int[9];

    static int turnCount = 0;

    /**
     * As name
     */
    static int defaultObstacleThreshold = 25;
    /**
     * All directions relative to our current position where we can try look for metals and try mine.
     */
    /**
     * A miner will not mine lead beneath this threshold
     */
    static final int sustainableLeadThreshold = 1;
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
        if ((turnCount++) == 0){
            // Initialize our bot
            for (int i=0; i<9; i++){
                previousRobotsNearTarget[i] = nullRobotInfo;
                previousRobotsNearTargetStayTurnCount[i] = 0;
            }
        }

        /** How the miner works
         *  Mining: try to mine gold, then lead, around itself; this always happens
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

        // Always determine whether oneself is at target
        if (atTarget && self.senseLead(self.getLocation()) == 0) // <= sustainableleadthreshold
            // if we were previously at target then target suddenly disappeared
            targetLoc = null;

        tryMineResources();

        if (targetLoc != null) {
            if (!validTarget(targetLoc)) {
                    targetLoc = null;
                    wander();
                    searchForTarget();
                    return;
                }

            // In the case that we have a target but have not reached it
            if (!self.getLocation().equals(targetLoc)) {
                // We have a target and we have not reached it

                // In all other cases move towards our current target
                if (self.canSenseLocation(targetLoc) && self.senseRubble(targetLoc) > defaultObstacleThreshold)
                    PathFinding.moveToBug0(targetLoc, self.senseRubble(targetLoc));
                PathFinding.moveToBug0(targetLoc);
                self.setIndicatorString("Moving to target " + targetLoc);
            }
            else{
                // When our mine is depleted, in that case nullify current target and search for a new one
                if (self.senseLead(targetLoc) == 0){
                    targetLoc = null;
                    wander();
                    searchForTarget();
                    return;
                }

                self.setIndicatorString("At target");
            }
        } else {
            // If we don't even have a target, then wander around
            // TODO: Check the broadcasted queue and go for one
            wander();
            searchForTarget();
            self.setIndicatorString("Wandering");
        }

        Messaging.reportAllEnemiesAround();
    }

    static void searchForTarget() throws GameActionException {
        MapLocation[] candidates = self.senseNearbyLocationsWithLead(visionRadiusSq);
        MapLocation bestLocation = null;
        int mostLead = 0;
        for (MapLocation loc : candidates){
            int leadAtLoc = self.senseLead(loc);
            if (leadAtLoc > mostLead && validTarget(loc)) {
                mostLead = leadAtLoc;
                bestLocation = loc;
            }
        }

        if (mostLead > 0) {
            targetLoc = bestLocation;
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLoc.x + "," + targetLoc.y + ")");
        }
    }

    static boolean validTarget(MapLocation loc) throws GameActionException{
        for (Direction dir: directionsWithMe){
            MapLocation neighborLoc = loc.add(dir);
            // If there is a miner adjacent to our dear target
            if (self.canSenseRobotAtLocation(neighborLoc)){
                RobotInfo botAtLoc = self.senseRobotAtLocation(neighborLoc);
                if (botAtLoc.getType() == RobotType.MINER && botAtLoc.getID() != self.getID()
                        && botAtLoc.getTeam() == us){
                    // If there is another miner adjacent to our dear target
                    atTarget = (self.getLocation() == loc);
                    if (atTarget && rng.nextInt(100) == 0)
                        return false;
                    else if (!atTarget && rng.nextInt(2) == 0)
                        return false;
                }
            }
        }
        return true;
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
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) > sustainableLeadThreshold)
                self.mineLead(mineLocation);
        }
    }
}