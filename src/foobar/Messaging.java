package foobar;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

public class Messaging extends Globals {
    public static void writeSharedLocation(int index, MapLocation loc) throws GameActionException {
        self.writeSharedArray(index, (loc.x << 6) | loc.y);
    }

    // What is the "&255 supposed to mean??" also isn't 6 (2^6=64) enough
    public static MapLocation readSharedLocation(int index) throws GameActionException {
        int raw = self.readSharedArray(index);
        return new MapLocation(raw >> 6, raw & 0x3F);
    }

    public static int getArchonOffset(int index) {
        return index * 4 + 1;
    }

    public static int getGlobalTurnCount() throws GameActionException {
        return self.readSharedArray(0);
    }
}
