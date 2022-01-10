package foobar;

import battlecode.common.Direction;
import battlecode.common.RobotInfo;
import battlecode.common.RobotMode;
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

        tryMineResources();

        if (targetLoc != null) {
            // Nullify our target if
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

    static boolean hasNeighborMiners(MapLocation loc) throws GameActionException{
        if (!self.canSenseLocation(loc))
            return false;
        for (Direction dir : directionsWithMe) {
            MapLocation neighborLoc = loc.add(dir);
            // If there is a miner adjacent to our dear target
            if (self.canSenseRobotAtLocation(neighborLoc)) {
                RobotInfo botAtLoc = self.senseRobotAtLocation(neighborLoc);
                if (botAtLoc.getType() == RobotType.MINER && botAtLoc.getID() != self.getID()
                        && botAtLoc.getTeam() == us)
                    // If there is another miner adjacent to our dear target
                    return true;
            }
        }
        return false;
    }

    static boolean validTarget(MapLocation loc) throws GameActionException{
        // If not at target, then:
            // 1: >5 lead amount (else someone's probably mining it sustainably
            // 2: No robots near it (including on top of it)
        // If at target, then:
            // 1: Our mine is not depleted
            // 2: If there are neighboring robots, nullify target with small probability
        if (!self.canSenseLocation(loc))
            return true;

        if (!self.getLocation().equals(loc)) {
            if (self.senseLead(loc) < 4)
                return false;

            if (hasNeighborMiners(loc))
                return false;
        } else{
            if (self.senseLead(loc) == 0) {
                self.setIndicatorString("Aborting current loc target due to no lead");
                return false;
            }
            RobotInfo[] neighborBots = self.senseNearbyRobots(actionRadiusSq, us);
            int numMiners = 0;
            for (RobotInfo bot:neighborBots)
                if (bot.getType() == RobotType.MINER && bot.getTeam() == us && bot.getID() != self.getID())
                    numMiners++;
            numMiners = 0;
            // For each additional bot have 1/10 probability to move away
            if (rng.nextInt(80) < numMiners) {
                self.setIndicatorString("Aborting current loc target due neighboring miners");
                return false;
            }
        }
        return true;
    }

    static void wander() throws GameActionException {
        // do not wander to a place with miners nearby
        int numValidDirections = 0;
        Direction[] validDirections = new Direction[9];
        for (Direction dir:directions){
            MapLocation tentativeLoc = self.getLocation().add(dir);
            if (self.canMove(dir) && !hasNeighborMiners(tentativeLoc))
                validDirections[numValidDirections++] = dir;
        }
        Direction moveDir;
        if (numValidDirections == 0)
            moveDir = directions[rng.nextInt(directions.length)];
        else
            moveDir = validDirections[rng.nextInt(numValidDirections)];
        if (self.canMove(moveDir))
            self.move(moveDir);
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