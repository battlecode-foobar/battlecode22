package foobar;

import battlecode.common.MapLocation;

public class Messaging {
    public static int encodeLocation(MapLocation loc) {
        return (loc.x << 8) | loc.y;
    }

    public static MapLocation decodeLocation(int raw) {
        return new MapLocation(raw >> 8, raw & 255);
    }
}
