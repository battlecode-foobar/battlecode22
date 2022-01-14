package frenchbot;

import battlecode.common.*;


/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    /**
     * The target location the miner is trying to approach.
     */
    static MapLocation targetLoc;
    /**
     * A miner will not mine lead beneath this threshold
     */
    static final int SUSTAINABLE_LEAD_THRESHOLD = 1;

    static double wanderTheta = 0;


    public static void step() throws GameActionException {
        if (firstRun()) {
            wanderTheta = (2 * rng.nextDouble() - 1) * Math.PI;
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        Messaging.claimMine(self.getLocation());
        tryMineResources();
        //This is no place for the weak, or foolhardy
        PathFinding.tryRetreat(20,-3);

        if (targetLoc == null || !isTargetStillValid()) {
            targetLoc = searchForTarget();
            if (targetLoc == null)
                targetLoc = searchForWanderingTarget();
        } else {
            MapLocation newLoc = searchForTarget();
            MapLocation here = self.getLocation();
            if (newLoc != null && newLoc.distanceSquaredTo(here) < targetLoc.distanceSquaredTo(here))
                targetLoc = newLoc;
        }

        if (targetLoc != null) {
            microAdjustTarget();
            // self.setIndicatorString("moving to target " + targetLoc);
            Messaging.claimMine(targetLoc);
            PathFinding.moveTo(targetLoc);
        } else {
            // PathFinding.spreadOut();
            // self.setIndicatorString("wandering");
            wander();
        }
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
        final int RADIUS = 9;
        MapLocation here = self.getLocation();
        while (true) {
            double theta = (2 * rng.nextDouble() - 1) * Math.PI;
            MapLocation newLoc = new MapLocation(
                    here.x + (int)Math.round(RADIUS * Math.cos(theta)),
                    here.y + (int)Math.round(RADIUS * Math.sin(theta))
            );
            if (newLoc.x < 0 || newLoc.y < 0)
                continue;
            if (newLoc.x >= self.getMapWidth() || newLoc.y >= self.getMapHeight())
                continue;
            return newLoc;
        }
    }

    static int getNeighboringMinerCount(MapLocation loc) throws GameActionException {
        if (!self.canSenseLocation(loc))
            return 0;
        int count = 0;
        for (Direction dir : directionsWithMe) {
            MapLocation neighborLoc = loc.add(dir);
            if (self.canSenseRobotAtLocation(neighborLoc)) {
                RobotInfo botAtLoc = self.senseRobotAtLocation(neighborLoc);
                if (botAtLoc == null)
                    continue;
                if (botAtLoc.getType().equals(RobotType.MINER) && botAtLoc.getID() != self.getID()
                        && botAtLoc.getTeam().equals(us))
                    count++;
            }
        }
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
                return;
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
        if (self.senseLead(targetLoc) == 0)
            return false;
        if (self.getLocation().equals(targetLoc))
            return true;
        if (!self.getLocation().equals(targetLoc)) {
            return getNeighboringMinerCount(targetLoc) == 0;
        } else {
            RobotInfo[] neighborBots = self.senseNearbyRobots(self.getType().visionRadiusSquared, us);
            int numMiners = 0;
            for (RobotInfo bot : neighborBots)
                if (bot.getType().equals(RobotType.MINER) && bot.getTeam().equals(us))
                    numMiners++;
            // For each additional bot have 1/10 probability to move away
            return rng.nextInt(80) >= numMiners;
        }
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