package foobar;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Messaging extends Globals {
    /**
     * An integer that corresponds to no locations.
     */
    public static final int IMPOSSIBLE_LOCATION = 1 << 12;

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
        return index * 4 + 1;
    }

    /**
     * Gets the global turn count.
     *
     * @return The global turn count.
     * @throws GameActionException Actually doesn't throw.
     */
    public static int getGlobalTurnCount() throws GameActionException {
        return self.readSharedArray(0);
    }

    /**
     * Tries to write a value in a range in the shared array.
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
     * Reports a dead archon.
     * @param loc The location of the dead archon.
     * @throws GameActionException Actually doesn't throw.
     */
    public static void reportDeadArchon(MapLocation loc) throws GameActionException {
        System.out.println("Archon dead at " + loc);
        tryAddInRange(17, 21, encodeLocation(loc), IMPOSSIBLE_LOCATION);
    }

    /**
     * Checks if the archon at the given location is dead.
     * @param loc The location of the archon to be checked.
     * @return If the archon has been reported dead.
     * @throws GameActionException Actually doesn't throw.
     */
    public static boolean isArchonDead(MapLocation loc) throws GameActionException {
        int raw = encodeLocation(loc);
        for (int i = 17; i < 21; i++)
            if (self.readSharedArray(i) == raw)
                return true;
        return false;
    }
}
