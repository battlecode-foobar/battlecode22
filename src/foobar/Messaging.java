package foobar;

import battlecode.common.*;

/**
 * Messaging-related utility functions.
 */
public class Messaging extends Globals {
    /**
     * Width of coordinate encoding.
     * <p>
     * The height and width are both at most 60 in BattleCode, so they each need 6 bits. Together we need 12bits to
     * fully encode a coordinate.
     */
    public static final int COORDINATE_WIDTH = 12;
    /**
     * Bit mask for location.
     */
    public static final int LOCATION_MASK = (1 << COORDINATE_WIDTH) - 1;
    /**
     * An integer that corresponds to no locations.
     */
    public static final int IMPOSSIBLE_LOCATION = LOCATION_MASK;
    /**
     * Index of global turn count in the shared array.
     */
    public static final int GLOBAL_TURN = 0;
    /**
     * Start index of the archon region in the shared array.
     */
    public static final int ARCHON_REGION_START = GLOBAL_TURN + 1;
    /**
     * Length of archon region allocated for each archon in the shared array.
     */
    public static final int PER_ARCHON_REGION_LENGTH = 4;
    /**
     * Index of soldier count in archon region.
     */
    public static final int SOLDIER_COUNT = 1;
    /**
     * Index of miner count in archon region.
     */
    public static final int MINER_COUNT = 2;
    /**
     * Availability of an archon.
     */
    public static final int AVAILABILITY = 3;
    /**
     * End index of the archon region in the shared array.
     */
    public static final int ARCHON_REGION_END = ARCHON_REGION_START + 4 * PER_ARCHON_REGION_LENGTH;
    /**
     * Start index of the enemy dead archon region in the shared array.
     */
    public static final int DEAD_ARCHON_START = ARCHON_REGION_END;
    /**
     * End index of the enemy dead archon region in the shared array.
     */
    public static final int DEAD_ARCHON_END = DEAD_ARCHON_START + 3;
    /**
     * Start index of the frontier region in the shared array.
     */
    public static final int FRONTIER_START = DEAD_ARCHON_END;
    /**
     * End index of the frontier region in the shared array.
     */
    public static final int FRONTIER_END = FRONTIER_START + 12;
    /**
     * Start index of the enemy archon region in the shared array.
     */
    public static final int ENEMY_ARCHON_START = FRONTIER_END;
    /**
     * End index of the enemy archon region in the shared array.
     */
    public static final int ENEMY_ARCHON_END = ENEMY_ARCHON_START + 4;
    /**
     * Start index of miner broadcast region in the shared array.
     */
    public static final int MINER_START = ENEMY_ARCHON_END;
    /**
     * End index of miner broadcast region in the shared array.
     */
    public static final int MINER_END = MINER_START + 16;
    /**
     * Builder logic: (watchtower location, isclaimed)
     */
    public static final int WATCHTOWER_START = MINER_END;
    public static final int WATCHTOWER_END = WATCHTOWER_START + 4;

    public static final int MINE_PROXIMITY = 2;

    /**
     * Encode build watchtower information: (targetLoc of watchtower, has a builder claimed it, has the builder arrived)
     */
    public static int encodeWatchtowerLocationAndClaimArrivalStatus(MapLocation loc, boolean claimed, boolean arrived) {
        // Add 1 to denote that this has been intentionally written
        return encodeLocation(loc) * 8 + (claimed ? 4 : 0) + (arrived ? 2 : 0) + 1;
    }

    /**
     * Returns whether the watchtower information has been successfully
     * If true, then the location either exists in Sharedarray or has been written to shared array
     */
    public static boolean tryBroadcastTargetWatchtowerLoc(MapLocation loc) throws GameActionException {
        for (int index = Messaging.WATCHTOWER_START; index < WATCHTOWER_END; index++) {
            int raw = self.readSharedArray(index);
            // If this block is empty (!=0 means has been intentionally written) and we can write
            if (raw % 2 == 0) {
                self.writeSharedArray(index,
                        encodeWatchtowerLocationAndClaimArrivalStatus(loc, false, false));
                return true;
            }
            // If there already exists a written entry with same location
            if (decodeLocation(raw / 8).equals(loc))
                return true;
        }
        return false;
    }

    /**
     * Encodes a location as an integer.
     *
     * @param loc The location to be encoded.
     * @return The encoded integer.
     */
    public static int encodeLocation(MapLocation loc) {
        return loc.x << 6 | loc.y;
    }

    public static MapLocation decodeLocation(int raw) {
        return new MapLocation((raw >> 6) & 0x3F, raw & 0x3F);
    }

    /**
     * Writes a location to the shared array.
     *
     * @param index The index in the shared array.
     * @param loc   The location to be written.
     * @throws GameActionException If index is invalid or the location is out of bounds.
     */
    public static void writeSharedLocation(int index, MapLocation loc) throws GameActionException {
        self.writeSharedArray(index, encodeLocation(loc));
    }

    /**
     * Reads a location from the shared array.
     *
     * @param index The index in the shared array.
     * @return The location..
     * @throws GameActionException if index is invalid.
     */
    public static MapLocation readSharedLocation(int index) throws GameActionException {
        return decodeLocation(self.readSharedArray(index));
    }

    /**
     * Gets the offset to the archon-specific region of a given archon.
     *
     * @param index The archon index of that archon.
     * @return The offset to the archon-specific region in the shared array.
     */
    public static int getArchonOffset(int index) {
        return index * PER_ARCHON_REGION_LENGTH + ARCHON_REGION_START;
    }

    /**
     * Gets the location of the archon with the given index.
     *
     * @param index The archon index.
     * @return The archon location.
     * @throws GameActionException If the index is invalid.
     */
    public static MapLocation getArchonLocation(int index) throws GameActionException {
        return readSharedLocation(getArchonOffset(index));
    }

    /**
     * Gets the number of soldiers made by the archon with the given index.
     *
     * @param index The archon index.
     * @return The number of soldiers.
     * @throws GameActionException If the index is invalid.
     */
    public static int getArchonSoldierCount(int index) throws GameActionException {
        return self.readSharedArray(getArchonOffset(index) + SOLDIER_COUNT);
    }

    /**
     * Checks whether an archon is not available.
     *
     * @param index The archon index.
     * @return Whether the archon specified by the index is not available.
     * @throws GameActionException Actually doesn't throw.
     */
    public static boolean isArchonUnavailable(int index) throws GameActionException {
        // When this function is called, there are two possibilities:
        // 1. None of the archons have updated the global turn count. Because update of availability counter happens
        //    after updating global turn count so availability has not been updated as well. In this case global turn
        //    count = availability.
        // 2. Global turn count has been updated but the availability has not. In this case availability = global turn
        //    count - 1.
        return self.readSharedArray(getArchonOffset(index) + AVAILABILITY) <= getGlobalTurnCount() - 2;
    }


    /**
     * Gets the total number of soldiers made by all archons.
     *
     * @return The number of soldiers.
     * @throws GameActionException If the index is invalid.
     */
    public static int getTotalSoldierCount() throws GameActionException {
        int total = 0;
        for (int i = 0; i < initialArchonCount; i++)
            total += getArchonSoldierCount(i);
        return total;
    }

    /**
     * Gets the number of miners made by the archon with the given index.
     *
     * @param index The archon index.
     * @return The number of miners.
     * @throws GameActionException If the index is invalid.
     */
    public static int getArchonMinerCount(int index) throws GameActionException {
        return self.readSharedArray(getArchonOffset(index) + MINER_COUNT);
    }

    /**
     * Gets the total number of miners made by all archons.
     *
     * @return The number of miners.
     * @throws GameActionException If the index is invalid.
     */
    public static int getTotalMinerCount() throws GameActionException {
        int total = 0;
        for (int i = 0; i < initialArchonCount; i++)
            total += getArchonMinerCount(i);
        return total;
    }

    /**
     * Gets the global turn count.
     *
     * @return The global turn count.
     * @throws GameActionException Actually doesn't throw.
     */
    public static int getGlobalTurnCount() throws GameActionException {
        return self.readSharedArray(GLOBAL_TURN);
    }

    /**
     * Tries to write a value in a range in the shared array if it doesn't exist already.
     *
     * @param start The start of the range (inclusive).
     * @param end   The end of the range (exclusive).
     * @param value The value to be written.
     * @param empty The empty value.
     * @throws GameActionException If any index is invalid or the value is out of bounds.
     */
    public static void tryAddInRange(int start, int end, int value, int empty) throws GameActionException {
        for (int i = start; i < end; i++) {
            if (self.readSharedArray(i) == empty) {
                self.writeSharedArray(i, value);
                return;
            }
            if (self.readSharedArray(i) == value)
                return;
        }
        int offset = rng.nextInt(end - start);
        self.writeSharedArray(start + offset, value);
    }

    /**
     * Tries to write a location in a range in the shared array if there isn't already a location in the vicinity of
     * the given location.
     *
     * @param start     The start of the range (inclusive).
     * @param end       The end of the range (exclusive).
     * @param loc       The location to be written.
     * @param proximity The proximity threshold (in distance squared).
     * @throws GameActionException If any index is invalid or the location is invalid.
     */
    public static void tryAddLocationInRange(int start, int end, MapLocation loc, int proximity, boolean replace)
            throws GameActionException {
        int encoded = encodeLocation(loc);
        for (int i = start; i < end; i++) {
            int raw = self.readSharedArray(i);
            if (raw == IMPOSSIBLE_LOCATION) {
                self.writeSharedArray(i, encoded);
                return;
            }
            MapLocation there = decodeLocation(raw);
            if (there.distanceSquaredTo(loc) <= proximity) {
                if (replace)
                    self.writeSharedArray(i, encoded);
                return;
            }
        }
        int offset = rng.nextInt(end - start);
        self.writeSharedArray(start + offset, encoded);
    }

    /**
     * Reports a dead archon.
     *
     * @param loc The location of the dead archon.
     * @throws GameActionException If the location is invalid.
     */
    public static void reportDeadArchon(MapLocation loc) throws GameActionException {
        tryAddInRange(DEAD_ARCHON_START, DEAD_ARCHON_END, encodeLocation(loc), IMPOSSIBLE_LOCATION);
    }

    /**
     * Reports an enemy unit.
     *
     * @param loc The location of the enemy unit.
     * @throws GameActionException If the location is invalid.
     */
    public static void reportEnemyUnit(MapLocation loc) throws GameActionException {
        tryAddLocationInRange(FRONTIER_START, FRONTIER_END, loc, 6, true);
    }

    /**
     * Performs a random sample (without replacement) up to a given number of elements from an array of objects.
     * @param candidates A list of map locations to be partially shuffled. This array will be mutated in-place.
     * @return The sample size.
     */
    @SuppressWarnings("SameParameterValue")
    static int sample(Object[] candidates, int maxCount) {
        if (candidates.length > maxCount) {
            for (int i = 0; i < maxCount; i++) {
                int j = i + rng.nextInt(candidates.length - i);
                Object temp = candidates[i];
                candidates[i] = candidates[j];
                candidates[j] = temp;
            }
            return maxCount;
        }
        return candidates.length;
    }

    /**
     * Reports all enemies around the robot.
     *
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportAllEnemiesAround() throws GameActionException {
        final int MAX_COUNT = 6;
        RobotInfo[] candidates = self.senseNearbyRobots(self.getType().visionRadiusSquared, them);
        if (candidates.length == 0)
            return;
        int len = sample(candidates, MAX_COUNT);
        for (int i = 0; i < len; i++)
            reportEnemyUnit(candidates[i].getLocation());
    }

    /**
     * Reports all lead locations around the robot.
     *
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportAllMinesAround() throws GameActionException {
        final int MAX_COUNT = 6;
        MapLocation[] candidates = self.senseNearbyLocationsWithLead(-1, TypeMiner.SUSTAINABLE_LEAD_THRESHOLD + 1);
        if (candidates.length == 0)
            return;
        int len = sample(candidates, MAX_COUNT);
        for (int i = 0; i < len; i++) {
            MapLocation loc = candidates[i];
            boolean nearMiner = false;
            for (RobotInfo bot : self.senseNearbyRobots(loc, 2, us)) {
                if (bot.getType().equals(RobotType.MINER)) {
                    nearMiner = true;
                    break;
                }
            }
            if (nearMiner)
                continue;
            int encoded = encodeLocation(loc);
            for (int j = MINER_START; j < MINER_END; j++) {
                int raw = self.readSharedArray(j);
                if (raw == IMPOSSIBLE_LOCATION || decodeLocation(raw).distanceSquaredTo(loc) > MINE_PROXIMITY) {
                    self.writeSharedArray(j, encoded/* | MINE_CLAIM_INITIAL_TTL << COORDINATE_WIDTH */);
                    break;
                }
            }
        }
    }


    /**
     * Claim the ownership of a mine.
     *
     * @param loc The location of the mine.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void claimMine(MapLocation loc) throws GameActionException {
        for (int i = MINER_START; i < MINER_END; i++) {
            int raw = self.readSharedArray(i);
            if (raw == IMPOSSIBLE_LOCATION)
                continue;
            MapLocation there = decodeLocation(raw);
            if (loc.distanceSquaredTo(there) <= MINE_PROXIMITY) {
                self.writeSharedArray(i, IMPOSSIBLE_LOCATION);
                break;
            }
        }
    }

    /**
     * Checks if the archon at the given location is dead.
     *
     * @param loc The location of the archon to be checked.
     * @return If the archon has been reported dead.
     * @throws GameActionException Actually doesn't throw.
     */
    public static boolean isEnemyArchonDead(MapLocation loc) throws GameActionException {
        int raw = encodeLocation(loc);
        for (int i = DEAD_ARCHON_START; i < DEAD_ARCHON_END; i++)
            if (self.readSharedArray(i) == raw)
                return true;
        return false;
    }

    public static boolean hasCoordinateIn(int start, int end) throws GameActionException {
        for (int i = start; i < end; i++) {
            int raw = self.readSharedArray(i);
            if (raw != IMPOSSIBLE_LOCATION)
                return true;
        }
        return false;
    }

    public static int getClosestArchonTo(MapLocation loc) throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        int minDisArchon = 0;
        for (int j = 0; j < initialArchonCount; j++) {
            if (isArchonUnavailable(j))
                continue;
            int dis = getArchonLocation(j).distanceSquaredTo(loc);
            if (dis < minDis) {
                minDis = dis;
                minDisArchon = j;
            }
        }
        return minDisArchon;
    }

    public static int getClosestArchonTo(int start, int end, int defaultValue) throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        int minDisArchon = defaultValue;
        for (int i = start; i < end; i++) {
            int raw = self.readSharedArray(i);
            if (raw == Messaging.IMPOSSIBLE_LOCATION || raw >> COORDINATE_WIDTH != 0)
                continue;
            for (int j = 0; j < initialArchonCount; j++) {
                if (isArchonUnavailable(j))
                    continue;
                int dis = getArchonLocation(j).distanceSquaredTo(Messaging.decodeLocation(raw));
                if (dis < minDis) {
                    minDis = dis;
                    minDisArchon = j;
                }
            }
        }
        return minDisArchon;
    }

    /**
     * Returns the most important frontier relative to Location "here"
     *
     * @param here The location.
     * @return The most important frontier relative to here.
     * @throws GameActionException Actually doesn't throw.
     */
    public static MapLocation getMostImportantFrontier(MapLocation here) throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        MapLocation minDisLoc = null;
        for (int i = FRONTIER_START; i < FRONTIER_END; i++) {
            int raw = self.readSharedArray(i);
            if (raw == IMPOSSIBLE_LOCATION)
                continue;
            MapLocation loc = decodeLocation(raw);
            if (loc.distanceSquaredTo(here) < minDis) {
                minDis = loc.distanceSquaredTo(here);
                minDisLoc = loc;
            }
        }
        if (minDis < 10)
            return minDisLoc;

        for (int i = FRONTIER_START; i < FRONTIER_END; i++) {
            int raw = self.readSharedArray(i);
            if (raw == IMPOSSIBLE_LOCATION)
                continue;
            MapLocation loc = decodeLocation(raw);
            for (int j = 0; j < initialArchonCount; j++) {
                if (loc.distanceSquaredTo(getArchonLocation(j)) < minDis) {
                    minDis = loc.distanceSquaredTo(here);
                    minDisLoc = loc;
                }
            }
        }
        return minDisLoc;
    }

    public static MapLocation getMostImportantFrontier() throws GameActionException {
        return getMostImportantFrontier(self.getLocation());
    }
}
