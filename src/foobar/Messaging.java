package foobar;

import battlecode.common.MapLocation;

public class Messaging {
    public static int encodeLocation(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }

    // What is the "&255 supposed to mean??" also isn't 6 (2^6=64) enough
    public static MapLocation decodeLocation(int raw) {
        return new MapLocation(raw >> 8, raw & 255);
    }

    /**
     * Encode locations [0, 60)x[0, 60] into a single integer (to write into shared memory)
     */
    // public static int encodeLocation(MapLocation loc) {return loc.x * 60 + loc.y;}

    /**
     * Decodes MapLocation from integer
     */
     // public static MapLocation decodeLocation(int encoded_location) {return
     //       new MapLocation((encoded_location-encoded_location%60) / 60, encoded_location%60);}
}
