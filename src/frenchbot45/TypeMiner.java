package frenchbot45;

import battlecode.common.*;

import java.util.Random;


/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    /**
     * The target location the miner is trying to approach.
     */
    static MapLocation targetLoc;
    /**
     * Am I wandering?
     */
    static boolean isWandering;
    /**
     * A miner will not mine lead beneath this threshold
     */
    static final int SUSTAINABLE_LEAD_THRESHOLD = 1;
    static double wanderTheta = 0;
    static int currentTargetCommitment = 0;

    public static void step() throws GameActionException {
        if (firstRun()) {
            Random trueRandom = new Random(self.getID());
            wanderTheta = (2 * trueRandom.nextDouble() - 1) * Math.PI;
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        Messaging.claimMine(self.getLocation());
        tryMineResources();
        // This is no place for the weak, or foolhardy
        PathFinding.tryRetreat(20);

        if (targetLoc == null || !isTargetStillValid()) {
            targetLoc = searchForTarget();
            isWandering = false;
            currentTargetCommitment = 0;
            if (targetLoc == null && false) {
                targetLoc = searchForWanderingTarget();
                isWandering = true;
            }
        } else if (currentTargetCommitment >= 1) {
            MapLocation newLoc = searchForTarget();
            MapLocation here = self.getLocation();
            boolean shouldUpdateLocation = false;
            if (newLoc != null) {
                if (isWandering) {
                    shouldUpdateLocation = true;
                    // shouldUpdateLocation = (targetLoc.x - here.x) * (newLoc.x - here.x) + (targetLoc.y - here.y) * (newLoc.y - here.y) >= 0;
                } else {
                    double oldDis = Math.sqrt(targetLoc.distanceSquaredTo(here));
                    double newDis = Math.sqrt(newLoc.distanceSquaredTo(here));
                    shouldUpdateLocation = newDis <= oldDis - 2;
                }
            }
            if (shouldUpdateLocation) {
                isWandering = false;
                targetLoc = newLoc;
                currentTargetCommitment = 0;
            }
        }

        if (targetLoc != null) {
            microAdjustTarget();
            Messaging.claimMine(targetLoc);
            boolean moved = PathFinding.moveTo(targetLoc);
            if (moved && isWandering)
                currentTargetCommitment++;
        } else {
            PathFinding.spreadOut();
        }
        self.setIndicatorString("target " + targetLoc + " wandering? " + isWandering);
    }

    static MapLocation searchForTarget() throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        MapLocation minDisLoc = null;
        for (int i = Messaging.MINER_START; i < Messaging.MINER_END; i++) {
            int raw = self.readSharedArray(i);
            if (raw == Messaging.IMPOSSIBLE_LOCATION)
                continue;
            MapLocation there = Messaging.decodeLocation(raw);
            if (self.getLocation().distanceSquaredTo(there) < minDis) {
                minDis = self.getLocation().distanceSquaredTo(there);
                minDisLoc = there;
            }
        }
        return minDisLoc;
    }

    static MapLocation searchForWanderingTarget() throws GameActionException {
        MapLocation here = self.getLocation();
        int width = self.getMapWidth(), height = self.getMapHeight();
        // double theta = (2 * rng.nextDouble() - 1) * Math.PI;
        double dx = width * Math.cos(wanderTheta);
        double dy = height * Math.sin(wanderTheta);
        double norm = Math.hypot(dx, dy);
        dx /= norm;
        dy /= norm;
        double x = here.x, y = here.y;
        while (0 <= x && x < width && 0 <= y && y < height) {
            x += dx;
            y += dy;
        }
        return new MapLocation((int)(x - dx), (int)(y - dy));
    }

    static int getNeighboringMinerCount(MapLocation loc) {
        if (!self.canSenseLocation(loc))
            return 0;
        int count = 0;
        for (RobotInfo bot : self.senseNearbyRobots(loc, 2, us))
            if (bot.getType().equals(RobotType.MINER))
                count++;
        return count;
    }

    static void microAdjustTarget() throws GameActionException {
        if (targetLoc == null || self.getLocation().distanceSquaredTo(targetLoc) > 9)
            return;
        int maxMinesAround = self.senseNearbyLocationsWithLead(targetLoc, 2).length;
        MapLocation newLoc = targetLoc;
        if (maxMinesAround == 8)
            return;
        for (Direction dir : directions) {
            MapLocation there = targetLoc.add(dir);
            if (!self.canSenseLocation(there))
                continue;
            int minesAround = self.senseNearbyLocationsWithLead(there, 2).length;
            if (maxMinesAround < minesAround) {
                maxMinesAround = minesAround;
                newLoc = there;
            }
        }
        targetLoc = newLoc;
    }

    static boolean isTargetStillValid() throws GameActionException {
        if (!self.canSenseLocation(targetLoc))
            return true;
        if (isWandering)
            return true;
        //    return self.senseNearbyLocationsWithLead(targetLoc, 2, SUSTAINABLE_LEAD_THRESHOLD + 1).length != 0;
        if (self.getLocation().equals(targetLoc))
            return true;
/*
        if (!self.getLocation().equals(targetLoc)) {
*/
        return getNeighboringMinerCount(targetLoc) == 0;
/*
        RobotInfo botThere = self.senseRobotAtLocation(targetLoc);
        return botThere == null || !botThere.getTeam().equals(us) || !botThere.getType().equals(RobotType.MINER);
*/
/*
        } else {
            RobotInfo[] neighborBots = self.senseNearbyRobots(self.getType().visionRadiusSquared, us);
            int numMiners = 0;
            for (RobotInfo bot : neighborBots)
                if (bot.getType().equals(RobotType.MINER) && bot.getTeam().equals(us))
                    numMiners++;
            // For each additional bot have 1/10 probability to move away
            return rng.nextInt(80) >= numMiners;
        }
*/
    }

    static void wander() throws GameActionException {
        int numValidDirections = 0;
        Direction[] validDirections = new Direction[9];
        for (Direction dir : directions) {
            MapLocation tentativeLoc = self.getLocation().add(dir);
            if (self.canMove(dir) && getNeighboringMinerCount(tentativeLoc) == 0)
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
        for (Direction dir : directionsWithMe) {
            MapLocation mineLocation = self.getLocation().add(dir);
            // Notice that the Miner's action cool down is very low.
            // You can mine multiple times per turn!
            while (self.canMineGold(mineLocation))
                self.mineGold(mineLocation);
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) > SUSTAINABLE_LEAD_THRESHOLD) {
                self.mineLead(mineLocation);
            }
        }
    }
}