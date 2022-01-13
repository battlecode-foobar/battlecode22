package vulturebot;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;


/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    static final int STARTUP_MINER_THRESHOLD = 15;
    /**
     * If we are in negotiation right now.
     */
    static boolean inNegotiation;
    /**
     * The relative index of the current archon relative to all archons. Guaranteed to be unique and falls in
     * [0, total archon count).
     */
    static int archonIndex;
    /**
     * Number of miners built.
     */
    static int minerCount;
    /**
     * Number of soldiers built.
     */
    static int soldierCount;
    /**
     * Number of builders built.
     */
    static int builderCount;


    public static void init() throws GameActionException {
        inNegotiation = self.getArchonCount() > 1;
        if (!inNegotiation)
            Messaging.writeSharedLocation(Messaging.getArchonOffset(0), self.getLocation());
        minerCount = 0;
        soldierCount = 0;
        builderCount = 0;
        for (int i = Messaging.DEAD_ARCHON_START; i < Messaging.MINER_END; i++)
            self.writeSharedArray(i, Messaging.IMPOSSIBLE_LOCATION);
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        if (turnCount > initialArchonCount)
            TypeSoldier.calculateEnemyArchons();

        if (inNegotiation)
            negotiate();
        self.writeSharedArray(0, turnCount);

        if (rng.nextDouble() <= 1.0 / self.getArchonCount()) {
            for (int i = 0; i < 2; i++) {
                int index = Messaging.FRONTIER_START + rng.nextInt(Messaging.FRONTIER_END - Messaging.FRONTIER_START);
                self.writeSharedArray(index, Messaging.IMPOSSIBLE_LOCATION);
            }
/*
            for (int i = 0; i < 6; i++) {
                int index = Messaging.MINER_START + rng.nextInt(Messaging.MINER_END - Messaging.MINER_START);
                self.writeSharedArray(index, self.readSharedArray(index) & Messaging.MINE_UNCLAIM_MASK);
            }
*/
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();

        int minSoldierCount = Integer.MAX_VALUE;
        int minMinerCount = Integer.MAX_VALUE;
        for (int i = 0; i < initialArchonCount; i++) {
            minSoldierCount = Math.min(minSoldierCount, Messaging.getArchonSoldierCount(i));
            minMinerCount = Math.min(minMinerCount, Messaging.getArchonMinerCount(i));
        }


        int maxSoldierCount = 0;
        int maxSoldierCountArchon = 0;
        for (int i = 0; i < initialArchonCount; i++) {
            int thisSoldierCount = Messaging.getArchonSoldierCount(i);
            if (thisSoldierCount > maxSoldierCount) {
                maxSoldierCount = thisSoldierCount;
                maxSoldierCountArchon = i;
            }
        }

        self.setIndicatorString("lead " + self.getTeamLeadAmount(us));

        if (self.getMode().equals(RobotMode.TURRET)) {
            // Build miners to start.
            if (turnCount <= 4 && minerCount < 2) {
                tryBuildTowardsLowRubble(RobotType.MINER);
/*
            } else if (soldierCount < 5) {
                tryBuildTowardsLowRubble(RobotType.SOLDIER);
            } else if (builderCount < 0) {
                tryBuildTowardsLowRubble(RobotType.BUILDER);
            } else if (minerCount < soldierCount * 5 / 10 && self.getTeamLeadAmount(us) < 5000) {
                tryBuildTowardsLowRubble(RobotType.MINER);
            } else if (builderCount < soldierCount / 30) {
                tryBuildTowardsLowRubble(RobotType.BUILDER);
*/
            } else {
                boolean hasUnclaimedMine = Messaging.hasCoordinateIn(Messaging.MINER_START, Messaging.MINER_END);
                boolean hasFrontiers = Messaging.hasCoordinateIn(Messaging.FRONTIER_START, Messaging.FRONTIER_END);

                boolean bestForMeToBuildMiner = true;
                if (hasUnclaimedMine) {
                    // If we have unclaimed mines, build miners to claim it only when we are closest to the mines.
                    bestForMeToBuildMiner = Messaging
                            .getClosestArchonTo(Messaging.MINER_START, Messaging.MINER_END, archonIndex) == archonIndex;
                }
                // But of course if we have surplus we don't mind building more miners.
                bestForMeToBuildMiner |= self.getTeamLeadAmount(us) > 300;

                boolean shouldBuildMiner = hasUnclaimedMine && bestForMeToBuildMiner;
                // We should definitely build more miners, but later we should focus less on building miners.
                shouldBuildMiner &= Messaging.getTotalMinerCount() <= STARTUP_MINER_THRESHOLD
                        || rng.nextDouble() < 0.125;
                shouldBuildMiner &= self.senseNearbyRobots(100, them).length == 0;

                if (shouldBuildMiner) {
                    tryBuildTowardsLowRubble(RobotType.MINER);
                } else {
                    boolean bestForMeToBuildSoldier;
                    if ((!hasFrontiers || rng.nextDouble() < 0.5) && turnCount > initialArchonCount) {
                        // If we have no frontiers, or with one half chance, we can try follow the schedule of a rush
                        // and build soldiers at the closest archon.
                        int idx = 0;
                        while (idx < initialArchonCount - 1 && Messaging.isEnemyArchonDead(TypeSoldier.enemyArchons[idx]))
                            idx++;
                        bestForMeToBuildSoldier = Messaging.getClosestArchonTo(TypeSoldier.enemyArchons[idx]) == archonIndex;
                    } else {
                        // Otherwise, we build soldiers at the archon closest to the frontier.
                        bestForMeToBuildSoldier = Messaging
                                .getClosestArchonTo(Messaging.FRONTIER_START, Messaging.FRONTIER_END, archonIndex) == archonIndex;
                    }
                    // But of course if we have surplus we don't mind building more soldiers.
                    bestForMeToBuildSoldier |= self.getTeamLeadAmount(us) > 300;

                    if (bestForMeToBuildSoldier)
                        tryBuildTowardsLowRubble(RobotType.SOLDIER);
                }
            }
            if (turnCount > initialArchonCount)
                self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.AVAILABILITY, turnCount);

/*
            if (self.senseNearbyRobots(self.getLocation(), self.getType().visionRadiusSquared, them).length > 0) {
                MapLocation target = Messaging.getArchonLocation(maxSoldierCountArchon);
                int distanceToTarget = self.getLocation().distanceSquaredTo(target);
                if (distanceToTarget > 10 && self.canTransform())
                    self.transform();
            }
*/
        } else if (self.getMode().equals(RobotMode.PORTABLE)) {
            // System.out.println("Max soldiers: " + maxSoldierCount + " archon: " + maxSoldierCountArchon);
            MapLocation target = Messaging.getArchonLocation(maxSoldierCountArchon);
            int distanceToTarget = self.getLocation().distanceSquaredTo(target);
            if (distanceToTarget > 10)
                PathFinding.moveToBug0(target);
            if (distanceToTarget <= 10 && self.canTransform())
                self.transform();
        }

/*
        MapLocation here = self.getLocation();
        int left = Clock.getBytecodesLeft();
        PathFindingGenerated.findPath(new MapLocation(here.x + 2, here.y + 2));
        System.out.println("path finding took " + (left - Clock.getBytecodesLeft()));
*/

        if (turnCount > initialArchonCount) {
            Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.SOLDIER_COUNT, soldierCount);
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.MINER_COUNT, minerCount);
        }
    }

    /**
     * Negotiate with other archons to determine its index.
     *
     * @throws GameActionException Should not throw any exception actually.
     */
    public static void negotiate() throws GameActionException {
        if (turnCount > 0) { // If at least one turn elapsed.
            if (self.readSharedArray(turnCount) == self.getID()) {
                archonIndex = turnCount - 1;
                log("Negotiate complete! I get index of " + archonIndex);
                Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
                inNegotiation = false;
                return;
            }
        }
        self.writeSharedArray(turnCount + 1, self.getID());
    }

    /**
     * To be called by other type of droids and queries who built itself.
     *
     * @return The archon index of the archon that built the droid.
     * @throws GameActionException Actually doesn't throw.
     */
    public static int whoBuiltMe() throws GameActionException {
        MapLocation here = self.getLocation();
        for (int i = 0; i < self.getArchonCount(); i++) {
            MapLocation thisArchon = Messaging.readSharedLocation(self.readSharedArray(Messaging.getArchonOffset(i)));
            if (thisArchon.isAdjacentTo(here)) {
                return i;
            }
        }
        // Should be unreachable;
        return 0;
    }

    /**
     * Safely sense the amount of rubble in the given direction.
     *
     * @param dir The direction to which to sense rubble.
     * @return The amount of rubble in that direction. An impractically large value is returned if the direction is
     * invalid.
     */
    static int senseRubbleSafe(Direction dir) {
        try {
            MapLocation loc = self.getLocation().add(dir);
            return self.canSenseLocation(loc) ? self.senseRubble(loc) : Integer.MAX_VALUE;
        } catch (GameActionException e) {
            e.printStackTrace();
            return Integer.MAX_VALUE;
        }
    }

    /**
     * Try to build a robot towards an available surrounding location with the least rubble available.
     *
     * @param type The type of robot to be built.
     * @throws GameActionException Actually it doesn't throw.
     */
    static void tryBuildTowardsLowRubble(RobotType type) throws GameActionException {
        Direction[] dirs = Arrays.copyOf(directions, directions.length);
        Arrays.sort(dirs, Comparator.comparingInt(TypeArchon::senseRubbleSafe));
        for (Direction dir : dirs) {
            if (self.canBuildRobot(type, dir)) {
                self.buildRobot(type, dir);
                switch (type) {
                    case MINER:
                        minerCount++;
                        break;
                    case SOLDIER:
                        soldierCount++;
                        break;
                    case BUILDER:
                        builderCount++;
                        break;
                }
                break;
            }
        }
    }

    static int computeCentralArchon() throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        int minDisArchon = archonIndex;
        for (int i = 0; i < initialArchonCount; i++) {
            int maxDis = 0;
            MapLocation loc = Messaging.getArchonLocation(i);
            for (int j = 0; j < initialArchonCount; j++) {
                if (i == j)
                    continue;
                maxDis = Math.max(maxDis, Messaging.getArchonLocation(j).distanceSquaredTo(loc));
            }
            if (minDis > maxDis) {
                minDis = maxDis;
                minDisArchon = i;
            }
        }
        return minDisArchon;
    }
}
