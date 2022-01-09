package foobar;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

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
     * Encodes a location as an integer.
     *
     * @param loc The location to be encoded.
     * @return The encoded integer.
     */
    public static int encodeLocation(MapLocation loc) {
        return (loc.x << 6) | loc.y;
    }

    /**
     * Decodes a location from an integer.
     *
     * @param raw The integer to be decoded;
     * @return The encoded location.
     */
    public static MapLocation decodeLocation(int raw) {
        return new MapLocation(raw >> 6, raw & 0x3F);
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
    public static void tryAddLocationInRange(int start, int end, MapLocation loc, int proximity)
            throws GameActionException {
        int encoded = encodeLocation(loc);
        for (int i = start; i < end; i++) {
            if (self.readSharedArray(i) == IMPOSSIBLE_LOCATION) {
                self.writeSharedArray(i, encoded);
                return;
            }
            if (readSharedLocation(i).distanceSquaredTo(loc) < proximity)
                return;
        }
        int offset = rng.nextInt(end - start);
        self.writeSharedArray(start + offset, encoded);
    }

    /**
     * Reports a dead archon.
     *
     * @param loc The location of the dead archon.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportDeadArchon(MapLocation loc) throws GameActionException {
        tryAddInRange(DEAD_ARCHON_START, DEAD_ARCHON_END, encodeLocation(loc), IMPOSSIBLE_LOCATION);
    }

    /**
     * Checks if the archon at the given location is dead.
     *
     * @param loc The location of the archon to be checked.
     * @return If the archon has been reported dead.
     * @throws GameActionException Actually doesn't throw.
     */
    public static boolean isArchonDead(MapLocation loc) throws GameActionException {
        int raw = encodeLocation(loc);
        for (int i = DEAD_ARCHON_START; i < DEAD_ARCHON_END; i++)
            if (self.readSharedArray(i) == raw)
                return true;
        return false;
    }
}
