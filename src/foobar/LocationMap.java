package foobar;

import battlecode.common.MapLocation;

/**
 * Adjacent location map. This data structure is much more bytecode-efficient than Java's stock HashMap because it is
 * more specialized. It can hold all MapLocations within a certain radius of a reference location (which is usually
 * where the robot is).
 */
public class LocationMap extends Globals {
    int offset;
    int stride;
    Object[] values;
    MapLocation center;

    /**
     * Creates a new LocationMap.
     *
     * @param center The center location of the map.
     * @param radiusSquared In some sense, the "capacity" of the map.
     */
    public LocationMap(MapLocation center, int radiusSquared) {
        assert radiusSquared >= 2;
        offset = 2 * radiusSquared;
        stride = 2 * (int) Math.sqrt(radiusSquared) + 1;
        values = new Object[4 * radiusSquared + 1];
        this.center = center;
    }

    /**
     * Gets the value associated with a given key.
     *
     * @param key The key.
     * @return The associate value if exists, null otherwise.
     */
    public Object get(MapLocation key) {
        return values[stride * (key.x - center.x) + (key.y - center.y) + offset];
    }

    /**
     * Sets the value associated with a given key.
     *
     * @param key The key.
     * @param value The value.
     */
    public void put(MapLocation key, Object value) {
        values[stride * (key.x - center.x) + (key.y - center.y) + offset] = value;
    }
}
