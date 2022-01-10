package foobar;

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
     * The cached vision radius.
     */
    static int visionRadiusSq = 0;
    /**
     * The cached action radius.
     */
    static int actionRadiusSq = 0;

    /**
     * A miner will not mine lead beneath this threshold
     */
    static final int sustainableLeadThreshold = 1;

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
        if (firstRun()) {
            visionRadiusSq = self.getType().visionRadiusSquared;
            actionRadiusSq = self.getType().actionRadiusSquared;
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        tryMineResources();

        if (targetLoc == null || !isTargetStillValid())
            searchForTarget();

        if (targetLoc != null) {
            Messaging.claimMine(targetLoc);
            PathFinding.moveToBug0(targetLoc);
        } else {
            PathFinding.spreadOut();
            // wander();
        }
    }

    static void searchForTarget() throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        MapLocation minDisLoc = null;
        for (int i = Messaging.MINER_START; i < Messaging.MINER_END; i++) {
            int raw = self.readSharedArray(i);
/*
            if (raw == Messaging.IMPOSSIBLE_LOCATION || (raw & Messaging.MINE_CLAIM_MASK) != 0)
                continue;
*/
            if (raw == Messaging.IMPOSSIBLE_LOCATION || (raw & Messaging.MINE_CLAIM_MASK) != 0)
                continue;
            MapLocation there = Messaging.decodeLocation(raw);
            if (self.getLocation().distanceSquaredTo(there) < minDis) {
                minDis = self.getLocation().distanceSquaredTo(there);
                minDisLoc = there;
            }
        }
        targetLoc = minDisLoc;
    }

    static boolean hasNeighborMiners(MapLocation loc) throws GameActionException {
        if (!self.canSenseLocation(loc))
            return false;
        for (Direction dir : directionsWithMe) {
            MapLocation neighborLoc = loc.add(dir);
            if (self.canSenseRobotAtLocation(neighborLoc)) {
                RobotInfo botAtLoc = self.senseRobotAtLocation(neighborLoc);
                if (botAtLoc.getType().equals(RobotType.MINER) && botAtLoc.getID() != self.getID()
                        && botAtLoc.getTeam().equals(us))
                    return true;
            }
        }
        return false;
    }

    static boolean isTargetStillValid() throws GameActionException {
        // If not at target, then:
        // 1: >5 lead amount (else someone's probably mining it sustainably
        // 2: No robots near it (including on top of it)
        // If at target, then:
        // 1: Our mine is not depleted
        // 2: If there are neighboring robots, nullify target with small probability

        if (!self.canSenseLocation(targetLoc))
            return true;

        if (self.senseLead(targetLoc) == 0)
            return false;
        if (!self.getLocation().equals(targetLoc)) {
            if (self.senseLead(targetLoc) == 0)
                return false;
            return !hasNeighborMiners(targetLoc);
        } else {
            if (self.senseLead(targetLoc) == 0)
                return false;
            RobotInfo[] neighborBots = self.senseNearbyRobots(actionRadiusSq, us);
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
            while (self.canMineLead(mineLocation) && self.senseLead(mineLocation) > sustainableLeadThreshold) {
                Messaging.claimMine(mineLocation);
                self.mineLead(mineLocation);
            }
        }
    }
}