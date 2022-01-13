package foobar;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

/**
 * Messaging-related utility functions.
 */
public class Messaging extends Globals {
    /**
     * An integer that corresponds to no locations.
     */
    public static final int IMPOSSIBLE_LOCATION = 1 << 12;
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
    public static final int BUILDWATCHTOWER_START = MINER_END;
    public static final int BUILDWATCHTOWER_END = BUILDWATCHTOWER_START+4;
    /**
     * Bit masking for claimed mine.
     */
    public static final int MINE_CLAIM_MASK = 1 << 13;
    /**
     * Bit masking for unclaimed mine.
     */
    public static final int MINE_UNCLAIM_MASK = MINE_CLAIM_MASK - 1;
    /**
     * Mine reporting proximity threshold.
     */
    public static final int MINE_PROXIMITY = 2;

    /**
     * Encode build watchtower information: (targetLoc of watchtower, has a builder claimed it, has the builder arrived)
     */
    public static int encodeWatchtowerLocationAndClaimArrivalStatus(MapLocation loc, boolean claimed, boolean arrived){
        // Add 1 to denote that this has been intentionally written
        return encodeLocation(loc)*8 + (claimed? 4:0)+(arrived?2:0)+1;
    }

    /**
     * Returns whether the watchtower information has been successfully
     * If true, then the location either exists in Sharedarray or has been written to shared array
     */
    public static boolean tryBroadcastTargetWatchtowerLoc(MapLocation loc) throws GameActionException{
        for (int index=Messaging.BUILDWATCHTOWER_START; index<BUILDWATCHTOWER_END; index++){
            int raw = self.readSharedArray(index);
            // If this block is empty (!=0 means has been intentionally written) and we can write
            if (raw % 2 ==0){
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
        return (loc.x << 6) | loc.y;
    }

    // What is the "&255 supposed to mean??" also isn't 6 (2^6=64) enough
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

    public static boolean isArchonAvailable(int index) throws GameActionException {
        return Math.abs(getGlobalTurnCount() - self.readSharedArray(getArchonOffset(index) + AVAILABILITY)) < 2;
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
     * Reports all enemies around the robot.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportAllEnemiesAround() throws GameActionException {
        RobotInfo[] candidates = self.senseNearbyRobots(self.getType().visionRadiusSquared, them);
        if (candidates.length == 0)
            return;
        for (RobotInfo candidate : candidates) {
            if (candidate.getType().equals(RobotType.ARCHON)) {
                tryAddLocationInRange(ENEMY_ARCHON_START, ENEMY_ARCHON_END, candidate.getLocation(), 5, true);
            }
        }
        for (int i = 0; i < 5; i++) {
            RobotInfo loc = candidates[rng.nextInt(candidates.length)];
            reportEnemyUnit(loc.getLocation());
        }
    }

    /**
     * Reports all lead locations around the robot.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportAllMinesAround() throws GameActionException {
        MapLocation[] candidates = self.senseNearbyLocationsWithLead(self.getType().visionRadiusSquared);
        if (candidates.length == 0)
            return;
        int iter = 0;
        while (iter < 5) {
            MapLocation loc = candidates[rng.nextInt(candidates.length)];
            if (self.senseLead(loc) > TypeMiner.SUSTAINABLE_LEAD_THRESHOLD) {
                tryAddLocationInRange(MINER_START, MINER_END, loc, MINE_PROXIMITY, false);
                break;
            }
            iter++;
        }
    }


    public static void claimMine(MapLocation loc) throws GameActionException {
        for (int i = MINER_START; i < MINER_END; i++) {
            int raw = self.readSharedArray(i);
/*
            if (raw == IMPOSSIBLE_LOCATION || (raw & MINE_CLAIM_MASK) != 0)
                continue;
*/
            if (raw == IMPOSSIBLE_LOCATION)
                continue;
            MapLocation there = decodeLocation(raw);
            if (loc.distanceSquaredTo(there) <= MINE_PROXIMITY) {
                // self.writeSharedArray(i, raw | MINE_CLAIM_MASK);
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
/*
            if (raw != IMPOSSIBLE_LOCATION && (raw & MINE_CLAIM_MASK) == 0)
                return true;
*/
            if (raw != IMPOSSIBLE_LOCATION)
                return true;
        }
        return false;
    }

    public static int getClosestArchonTo(MapLocation loc) throws GameActionException {
        int minDis = Integer.MAX_VALUE;
        int minDisArchon = 0;
        for (int j = 0; j < initialArchonCount; j++) {
            if (!isArchonAvailable(j))
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
/*
            if (raw == Messaging.IMPOSSIBLE_LOCATION || (raw & MINE_CLAIM_MASK) != 0)
                continue;
*/
            if (raw == IMPOSSIBLE_LOCATION)
                continue;
            for (int j = 0; j < initialArchonCount; j++) {
                if (!isArchonAvailable(j))
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
     * @param location
     * @return
     * @throws GameActionException
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

    public static MapLocation getMostImportantFrontier() throws GameActionException{
        return getMostImportantFrontier(self.getLocation());
    }
}
