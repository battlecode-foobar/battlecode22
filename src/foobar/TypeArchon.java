package foobar;

import battlecode.common.*;

import java.util.Arrays;
import java.util.Comparator;


/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeArchon extends Globals {
    static final int STARTUP_MINER_THRESHOLD = 10;
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
    /**
     * The index of the central archon.
     */
    static int centralArchonIndex;

    static Direction enemyDirection = Direction.CENTER;


    public static void init() throws GameActionException {
        inNegotiation = self.getArchonCount() > 1;
        if (!inNegotiation)
            Messaging.writeSharedLocation(Messaging.getArchonOffset(0), self.getLocation());
        minerCount = 0;
        soldierCount = 0;
        builderCount = 0;
        for (int i = Messaging.DEAD_ARCHON_START; i < Messaging.MINER_END; i++)
            self.writeSharedArray(i, Messaging.IMPOSSIBLE_LOCATION);
        // Clear the watchtower indices
        for (int i = Messaging.WATCHTOWER_START; i < Messaging.WATCHTOWER_END; i++)
            self.writeSharedArray(i, 0);
        // Initialize symmetry indices: ***note we initialize to 1 (last bit means whether symmetry is possible)
        for (int i = Messaging.CANDIDATE_SYMMETRY_START; i < Messaging.CANDIDATE_SYMMETRY_END; i++)
            self.writeSharedArray(i, 1);
    }

    // Decides whether the two arrays of candidate locations are equivalent up to permutation
    public static boolean isSymmetryEquivalent(MapLocation[] locsA, MapLocation[] locsB) {
        for (MapLocation b : locsB) {
            boolean hasMatch = false;
            for (MapLocation a : locsA) {
                if (b.equals(a)) {
                    hasMatch = true;
                    break;
                }
            }
            if (!hasMatch)
                return false;
        }
        return true;
    }

    /**
     * Hypothisize different symmetry possibilities; rule out symmetry if one of the following
     * 1. Candidate symmetry overlaps with friendly archon location
     * 2. Candidate symmetry equivalent with another symmetry
     * 3. A location of candidate symmetry observable by friendly archon but no archon found
     * Then compute and write representative symmetry locations to shared array
     */
    public static void hypothesizeWriteSymmetry() throws GameActionException {
        boolean debugOutputs = archonIndex == centralArchonIndex;
        boolean[] symmetryPossible = new boolean[3];
        if (debugOutputs)
            System.out.println("Central archon @" + self.getLocation() + " trying to compute enemy archons");
        MapLocation[] usArchonLocs = new MapLocation[initialArchonCount];
        if (debugOutputs)
            System.out.println("Our Archon Locs:");
        for (int i = 0; i < initialArchonCount; i++) {
            usArchonLocs[i] = Messaging.getArchonLocation(i);
            if (debugOutputs)
                System.out.println("" + i + " " + usArchonLocs[i]);
        }
        MapLocation[][] enemyArchonLocs = new MapLocation[3][initialArchonCount];

        int width = self.getMapWidth();
        int height = self.getMapHeight();
        for (int i = 0; i < initialArchonCount; i++) {
            MapLocation loc = usArchonLocs[i];
            int x = loc.x, cx = width - x - 1;
            int y = loc.y, cy = height - y - 1;
            enemyArchonLocs[0][i] = new MapLocation(cx, y); // Horizontal symmetry.
            enemyArchonLocs[1][i] = new MapLocation(x, cy); // Vertical symmetry.
            enemyArchonLocs[2][i] = new MapLocation(cx, cy); // 180 deg. rotational symmetry.
        }

        symmetryLoop:
        for (int symmetryIndex = 0; symmetryIndex < 3; symmetryIndex++) {
            symmetryPossible[symmetryIndex] = true;
            if (debugOutputs)
                System.out.println("Enemy Archon Locs under symmetry #" + symmetryIndex);
            for (int i = 0; i < initialArchonCount; i++) {
                // If any location coincides with any of our archon's locations, immediate it rule out
                for (int k = 0; k < initialArchonCount; k++) {
                    if (enemyArchonLocs[symmetryIndex][i].equals(usArchonLocs[k])) {
                        if (debugOutputs)
                            System.out.println("Ruled out symmetry #" + symmetryIndex + " due to archon loc conflict");
                        symmetryPossible[symmetryIndex] = false;
                        continue symmetryLoop;
                    }
                }
                // If this archon can sense this location and there's no enemy archon there, rule it out
                // This is the archon-specific rule that compels us to run a copy of this largely-extraneous
                // computation for each archon
                MapLocation enemyArchonLoc = enemyArchonLocs[symmetryIndex][i];
                if (self.canSenseLocation(enemyArchonLoc)) {
                    if (debugOutputs)
                        System.out.println(self.getLocation() + " can sense " + enemyArchonLoc);
                    if (!self.canSenseRobotAtLocation(enemyArchonLoc) ||
                                self.senseRobotAtLocation(enemyArchonLoc).getType() != RobotType.ARCHON) {
                        if (debugOutputs) {
                            System.out.println("Ruled out symmetry #" + symmetryIndex + " because candidate loc " +
                                    enemyArchonLocs[symmetryIndex][i] + " is in vision of " + self.getLocation()
                                    + "but not sensed.");
                        }
                        symmetryPossible[symmetryIndex] = false;
                        continue symmetryLoop;
                    }
                }
                if (debugOutputs)
                    System.out.println(enemyArchonLocs[symmetryIndex][i]);
            }
        }
        // Most simple case: if two symmetries produce the same result then eliminate one
        if (isSymmetryEquivalent(enemyArchonLocs[0], enemyArchonLocs[1])) {
            if (debugOutputs)
                System.out.println("Ruled out vertical symmetry due to equivalence with horizontal");
            symmetryPossible[1] = false;
        }
        if (isSymmetryEquivalent(enemyArchonLocs[0], enemyArchonLocs[2])) {
            if (debugOutputs)
                System.out.println("Ruled out central symmetry due to equivalence with horizontal");
            symmetryPossible[2] = false;
        }
        if (isSymmetryEquivalent(enemyArchonLocs[1], enemyArchonLocs[2])) {
            if (debugOutputs)
                System.out.println("Ruled central symmetry due to equivalence with vertical");
            symmetryPossible[2] = false;
        }
        // For each valid symmetry, compute representative Archon (unique to symmetry mod & least distance from us)
        MapLocation[] repEnemyArchons = new MapLocation[3];
        int[] closestFriendlyArchonIndices = new int[3];
        for (int symmetryIndex = 0; symmetryIndex < 3; symmetryIndex++) {
            if (!symmetryPossible[symmetryIndex])
                continue;
            int minMinDistance = Integer.MAX_VALUE;
            for (int i = 0; i < initialArchonCount; i++) {
                // Compute the shortest distance of enemyArchonLocs[i] to our archons
                int minDistance = Integer.MAX_VALUE;
                int minDistIndex = -1;
                for (int j = 0; j < initialArchonCount; j++) {
                    int distSq = enemyArchonLocs[symmetryIndex][i].distanceSquaredTo(usArchonLocs[j]);
                    if (distSq < minDistance) {
                        minDistance = distSq;
                        minDistIndex = j;
                    }
                }
                if (minDistance < minMinDistance) {
                    minMinDistance = minDistance;
                    repEnemyArchons[symmetryIndex] = enemyArchonLocs[symmetryIndex][i];
                    closestFriendlyArchonIndices[symmetryIndex] = minDistIndex;
                }
            }
        }
        for (int symmetryIndex = 0; symmetryIndex < 3; symmetryIndex++) {
            if (debugOutputs)
                System.out.println("Representative for symmetry " + symmetryIndex + repEnemyArchons[symmetryIndex]);
            // Write (representative location, claimed, is_possible) to shared array
            int arrayIndex = Messaging.CANDIDATE_SYMMETRY_START + symmetryIndex;
            boolean broadcastedSymmetryPossible = self.readSharedArray(arrayIndex) % 2 == 1;
            // Don't bother to write if the symmetry's already declared impossible
            if (!broadcastedSymmetryPossible) {
                if (debugOutputs)
                    System.out.println("Symmetry " + symmetryIndex + " globally broadcasted as impossible");
                continue;
            }
            // Else: if locally known to be impossible simply write 0 (denoting this symmetry's impossible
            if (!symmetryPossible[symmetryIndex]) {
                if (debugOutputs)
                    System.out.println("Symmetry " + symmetryIndex + " locally computed not possible");
                self.writeSharedArray(arrayIndex, 0);
            } else {
                // Else write encoded tuple to array
                int raw = Messaging.encodeSymmetryInfo(repEnemyArchons[symmetryIndex],
                        closestFriendlyArchonIndices[symmetryIndex], false, true);
                if (debugOutputs)
                    System.out.println("Writing symmetry hypothsis " + raw + " for " + symmetryIndex);
                self.writeSharedArray(arrayIndex, raw);
            }
        }
    }

    /**
     * Representative symmetry locations and friendly archon responsible for them have already been written.
     * TODO: What to do?
     * Write now, just print which archon location for which locations
     */
    public static void actToSymmetryHypothesis() throws GameActionException {
        for (int symmetryIndex = 0; symmetryIndex < 3; symmetryIndex++) {
            int arrayIndex = Messaging.CANDIDATE_SYMMETRY_START + symmetryIndex;
            int raw = self.readSharedArray(arrayIndex);
            // Decode the symmetry information (representative loc, responsible archon index, claimed, isPossible)
            boolean possible = (raw & 1) != 0;
            if (!possible)
                continue;
            boolean claimed = (raw & 2) != 0;
            MapLocation repLoc = Messaging.decodeLocation(raw >> 4);
            int responsibleArchonIndex = (raw >> 2) & 3;
        }
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        if (inNegotiation)
            negotiate();
        if (turnCount == initialArchonCount + 1)
            centralArchonIndex = computeCentralArchon();

        if (turnCount == initialArchonCount + 1) // && archonIndex == centralArchonIndex
            hypothesizeWriteSymmetry();

        if (turnCount == initialArchonCount + 2)
            actToSymmetryHypothesis();

        MapLocation debugLoc = new MapLocation(rng.nextInt(self.getMapWidth()), rng.nextInt(self.getMapHeight()));

        if (isValidMapLoc(debugLoc)) {
            boolean[] symmetryCandidates = Messaging.readSymmetryPossibilities();
            self.setIndicatorString(debugLoc + "territory type " + isOurTerritoryUnderSymmetryHypothesis(debugLoc) + " under hypothesis " + symmetryCandidates[0] + " " + symmetryCandidates[1] + " " + symmetryCandidates[2]);
        }

        // Write global turn count.
        self.writeSharedArray(0, turnCount);

        boolean shouldForget = false;
        if (isAllNegotiationsComplete()) {
            for (int i = 0; i < initialArchonCount; i++) {
                if (Messaging.isArchonUnavailable(i))
                    continue;
                shouldForget = i == archonIndex;
                break;
            }
        }

        if (shouldForget) {
            // Probabilistic forgetting. Average lifetime of frontier message = frontier region length / constant below.
            for (int i = 0; i < 2; i++) {
                int index = Messaging.FRONTIER_START + rng.nextInt(Messaging.FRONTIER_END - Messaging.FRONTIER_START);
                self.writeSharedArray(index, Messaging.IMPOSSIBLE_LOCATION);
            }
/*
            for (int i = Messaging.MINER_START; i < Messaging.MINER_END; i++) {
                int raw = self.readSharedArray(i);
                // A trick: IMPOSSIBLE_LOCATION is defined as 1 less 1 << COORDINATE_WIDTH. So all mine locations with
                // at least 1 ttl must be > IMPOSSIBLE_LOCATION.
                if (raw > Messaging.IMPOSSIBLE_LOCATION)
                    self.writeSharedArray(i, raw - (1 << Messaging.COORDINATE_WIDTH));
            }
*/
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();

        // self.setIndicatorString("lead " + self.getTeamLeadAmount(us));

        if (self.getMode().equals(RobotMode.TURRET)) {
            // If we can't build anything, we should try repairing units around us.
            // Builder is the cheapest unit we can build.
            if (self.getTeamLeadAmount(us) < RobotType.BUILDER.buildCostLead) {
                int minHealth = 0;
                int maxPriority = 0;
                MapLocation repairLoc = null;
                for (RobotInfo bot : self.senseNearbyRobots(self.getType().actionRadiusSquared, us)) {
                    int priority = FireControl.evaluatePriority(bot);
                    if (priority < maxPriority)
                        continue;
                    int health = bot.getHealth();
                    if (health == bot.getType().health)
                        continue;
                    if (priority > maxPriority || minHealth > health) {
                        maxPriority = priority;
                        minHealth = health;
                        repairLoc = bot.getLocation();
                    }
                }
                if (repairLoc != null && self.canRepair(repairLoc))
                    self.repair(repairLoc);
            } else {
                // enemyDirection is defaultly center; else when frontier occurs update for the archon closest to it.
                updateProximalEnemyDirection();
                if (shouldBuildWatchtower())
                    scheduleWatchtower();
                    // OPTIMIZE: if multiple options are available, should have a tie-breaker of some sort.
                else if (shouldBuildBuilder())
                    tryBuildTowardsLowRubble(RobotType.BUILDER);
                else if (shouldBuildMiner())
                    tryBuildTowardsLowRubble(RobotType.MINER);
                else if (shouldBuildSoldier())
                    tryBuildTowardsLowRubble(RobotType.SOLDIER);
                if (isAllNegotiationsComplete())
                    self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.AVAILABILITY, turnCount);
            }
        } else if (self.getMode().equals(RobotMode.PORTABLE)) {
            // System.out.println("Max soldiers: " + maxSoldierCount + " archon: " + maxSoldierCountArchon);
            MapLocation target = Messaging.getArchonLocation(centralArchonIndex);
            int distanceToTarget = self.getLocation().distanceSquaredTo(target);
            if (distanceToTarget > 25)
                PathFinding.moveTo(target);
            if (distanceToTarget <= 25 && self.canTransform())
                self.transform();
        }
        if (isAllNegotiationsComplete()) {
            Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.SOLDIER_COUNT, soldierCount);
            self.writeSharedArray(Messaging.getArchonOffset(archonIndex) + Messaging.MINER_COUNT, minerCount);
        }
    }

    /**
     * Checks if all archons have completed negotiation.
     *
     * @return Whether all archons have completed negotiation.
     */
    public static boolean isAllNegotiationsComplete() {
        return turnCount > initialArchonCount;
    }

    /**
     * Checks if I should build miners.
     *
     * @return Whether the archon should build a miner in this turn.
     */
    @SuppressWarnings("RedundantIfStatement")
    static boolean shouldBuildMiner() throws GameActionException {
        if (turnCount < 4 && minerCount < 3)
            return true;
        if (!Messaging.hasCoordinateIn(Messaging.MINER_START, Messaging.MINER_END))
            return false;
        if (Messaging.getClosestArchonTo(Messaging.MINER_START, Messaging.MINER_END, archonIndex) != archonIndex)
            return false;
        if (Messaging.getTotalMinerCount() > 6 * initialArchonCount && rng.nextDouble() > 0.1)
            return false;
        if (self.getTeamLeadAmount(us) > 300)
            return true;
        if (PathFinding.tryRetreat(RobotType.ARCHON.visionRadiusSquared, -1))
            return false;
        if (watchtowerWaitingFund())
            return false;
        return true;
    }

    /**
     * Checks if I should build soldiers.
     *
     * @return Whether the archon should build a soldier in this turn.
     */
    @SuppressWarnings("RedundantIfStatement")
    static boolean shouldBuildSoldier() throws GameActionException {
        if (Messaging.hasCoordinateIn(Messaging.FRONTIER_START, Messaging.FRONTIER_END)) {
            if (watchtowerWaitingFund())
                return false;
            if (self.getTeamLeadAmount(us) > 300)
                return true;
            if (Messaging.getClosestArchonTo(Messaging.FRONTIER_START, Messaging.FRONTIER_END, archonIndex) != archonIndex)
                return false;
            return true;
        }
        return true;
    }

    /**
     * Checks if I should build builders.
     *
     * @return Whether the archon should build a builder in this turn.
     */
    static boolean shouldBuildBuilder() {
        // Build a builder when we are ready to build a watchtower and there is no builder
        // return false; // Temporarily debugging...
        return builderCount == 0 && shouldBuildWatchtower();
    }

    static boolean shouldBuildWatchtower() {
        return (enemyDirection != Direction.CENTER && soldierCount > 5 && false);
    }

    static void scheduleWatchtower() throws GameActionException {
        if (enemyDirection == Direction.CENTER)
            return;
        MapLocation tentativeWatchtowerLoc = self.getLocation().add(enemyDirection);
        // In the 5 directions closest, choose the one with lowest rubble
        Direction[] watchtowerDirs = PathFinding.getDiscreteDirection5(PathFinding.getTheta(tentativeWatchtowerLoc));
        Direction bestWatchtowerDir = null;
        int leastRubble = Integer.MAX_VALUE;
        for (Direction dir : watchtowerDirs) {
            MapLocation candidateLoc = self.getLocation().add(dir);
            if (self.canSenseLocation(candidateLoc)) {
                if (self.canSenseRobotAtLocation(candidateLoc)) {
                    if (self.senseRobotAtLocation(candidateLoc).getType() == RobotType.WATCHTOWER)
                        return;
                    continue;
                }
                int rubbleAtLoc = self.senseRubble(candidateLoc);
                if (rubbleAtLoc < leastRubble) {
                    leastRubble = rubbleAtLoc;
                    bestWatchtowerDir = dir;
                }
            }
        }
        MapLocation scheduledWatchtowerLoc = self.getLocation().add(bestWatchtowerDir);
        if (bestWatchtowerDir != null && !self.canSenseRobotAtLocation(scheduledWatchtowerLoc)) {
            self.setIndicatorString("Broadcasting for watchtower built@" + scheduledWatchtowerLoc + shouldBuildWatchtower());
            Messaging.tryBroadcastTargetWatchtowerLoc(scheduledWatchtowerLoc);
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
                Messaging.writeSharedLocation(Messaging.getArchonOffset(archonIndex), self.getLocation());
                inNegotiation = false;
                return;
            }
        }
        self.writeSharedArray(turnCount + 1, self.getID());
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
            return self.canSenseLocation(loc) && !self.canSenseRobotAtLocation(loc)
                    ? self.senseRubble(loc) : Integer.MAX_VALUE;
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

    /**
     * Compute and select a central archon for other archons to flee to.
     *
     * @return The archon index of the described archon.
     * @throws GameActionException Actually doesn't throw.
     */
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


    /**
     * Utility for archon: scans array for whether there is a watchtower which awaits fund to be built
     * If Archon wants to build watchtower then reserve 180 lead
     */
    static boolean watchtowerWaitingFund() throws GameActionException {
        for (int index = Messaging.WATCHTOWER_START; index < Messaging.WATCHTOWER_END; index++)
            // This means the last three bits encode
            // (builder claimed this watchtower) (builder arrived) (this entry is intentionally written)
            if (self.readSharedArray(index) % 8 == 7)
                return true;
        return false;
    }

    // Keep try broadcasting diagonal positions of ourselves for a watchtower
    static boolean BroadcastDiagonalPositionsForWatchtower() throws GameActionException {
        for (Direction diagDir : diagonalDirections) {
            MapLocation tentativeWatchtowerLoc = self.getLocation().add(diagDir);
            if (!self.canSenseRobotAtLocation(tentativeWatchtowerLoc)) {
                self.setIndicatorString("Broadcasting for Turret built@" + tentativeWatchtowerLoc);
                return Messaging.tryBroadcastTargetWatchtowerLoc(tentativeWatchtowerLoc);
            }
        }
        return false;
    }

    // Check whether this is the archon closest to frontier
    // Returns CENTER if not closest (or no frontier) else direction
    // This provides a first (often accurate) estimate of where the enemy is relative to this archon
    static Direction updateProximalEnemyDirection() throws GameActionException {
        int minDistanceToFrontier = Integer.MAX_VALUE;
        int minArchonIndex = -1;
        Direction closestArchonDirToFrontier = Direction.CENTER;
        for (int archonIndex = 0; archonIndex < self.getArchonCount(); archonIndex++) {
            MapLocation archonLoc = Messaging.getArchonLocation(archonIndex);
            MapLocation relativeFrontierLoc = Messaging.getMostImportantFrontier(archonLoc);
            int distToFrontier = Integer.MAX_VALUE;
            if (relativeFrontierLoc != null)
                distToFrontier = archonLoc.distanceSquaredTo(relativeFrontierLoc);
            if (distToFrontier < minDistanceToFrontier) {
                minDistanceToFrontier = distToFrontier;
                minArchonIndex = archonIndex;
                closestArchonDirToFrontier = archonLoc.directionTo(relativeFrontierLoc);
            }
        }
        if (minArchonIndex == archonIndex) {
            // Provide a first estimate of where the enemy is;
            if (enemyDirection == Direction.CENTER)
                enemyDirection = closestArchonDirToFrontier;
            return closestArchonDirToFrontier;
        }
        return Direction.CENTER;
    }
}
