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
    /**
     * Record the miner ID at target (if there is any)
     */
    static int minerIDatTarget = -1;
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

        // Always determine whether oneself is at target
        if (atTarget && self.senseLead(self.getLocation()) <= sustainableLeadThreshold)
            // if we were previously at target then target suddenly disappeared
            // Send this one to journey elsewhere, for we have need of sterner stock
            targetLoc = null;

        if (targetLoc == null) {
            atTarget = false;
            self.setIndicatorString("Wandering");
        } else {
            atTarget = self.getLocation().equals(targetLoc);
            if (atTarget)
                self.setIndicatorString("At target " + targetLoc);
            else
                self.setIndicatorString("Not at target " + targetLoc + " yet");
        }

        tryMineResources();

        if (targetLoc != null) {
            // In the case that we have a target
            if (!self.getLocation().equals(targetLoc)) {
                // We have a target and we have not reached it
                // First, if we can sense the target verify it is still a good target (there's no miner occupying it)
                if (self.canSenseRobotAtLocation(targetLoc)) {
                    RobotInfo robotAtTarget = self.senseRobotAtLocation(targetLoc);
                    if (robotAtTarget.getType() == RobotType.MINER && robotAtTarget.getTeam() == us) {
                        if (minerIDatTarget == robotAtTarget.getID()){
                            // If there the same miner has occupied our target for >1 round: nullify our current target
                            minerIDatTarget = -1;
                            targetLoc = null;
                            wander();
                            // Another miner, its dream battered and broken, in search of a new life
                            searchForTarget();
                            return;
                        }
                        else
                            minerIDatTarget = robotAtTarget.getID();
                    }
                }
                // In all other cases move towards our current target
                if (self.canSenseLocation(targetLoc) && self.senseRubble(targetLoc) > defaultObstacleThreshold)
                    PathFinding.moveToBug0(targetLoc, self.senseRubble(targetLoc));
                PathFinding.moveToBug0(targetLoc);
            }
            else{
                // We are only writing for the extreme case when our mine is depleted, in that case nullify
                //      current target and search for a new one
                if (self.senseLead(targetLoc) == 0){
                    targetLoc = null;
                    wander();
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

    static void searchForTarget() throws GameActionException {
        MapLocation[] candidates = self.getAllLocationsWithinRadiusSquared(self.getLocation(), visionRadiusSq);
        MapLocation bestLocation = null;
        int mostLead = 0;

        for (MapLocation loc : candidates) {
            int leadAtLoc = self.senseLead(loc);
            if (leadAtLoc > mostLead) {
                // when we search for a target, there should not be our miner on it
                if (self.canSenseRobotAtLocation(loc)){
                    RobotInfo targetRobotInfo = self.senseRobotAtLocation(loc);
                    if (targetRobotInfo.getType() == RobotType.MINER && targetRobotInfo.getTeam() == us)
                        continue;
                }
                mostLead = leadAtLoc;
                bestLocation = loc;
            }
        }

        if (mostLead > 0) {
            targetLoc = bestLocation;
            log("Droid " + self.getID() + " found lead amount " + mostLead + " @(" + targetLoc.x + "," + targetLoc.y + ")");
        }
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