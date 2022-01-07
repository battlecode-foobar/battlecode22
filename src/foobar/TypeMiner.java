package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

/**
 * Main controller logic for a Miner unit
 */
public strictfp class TypeMiner extends Globals {
    // the default (-1, -1) is (equivalent to saying that there's no target yet)
    static MapLocation target_location = new MapLocation(-1, -1);

    // returns whether the robot has a target
    public static boolean is_wandering() {return target_location.x == -1 && target_location.y == -1;}

    public static void step() throws GameActionException {
        if (firstRun()){
            scan_for_target();
        }
        // scan for a target within sight
        // TODO: make sure there are no other
        if (is_wandering()){
            // TODO: wander in the general direction where there are no other robots
            wander();}
        else {
            //go towards the current target
        }
        try_mining();
    }

    // Scan for whether there is a target in sight
    static void scan_for_lead() throws GameActionException{
        // scan for a list of locations with lead; greater than vision radius so we will
        // MapLocation[] targets = self.senseNearbyLocationsWithLead(64)
    }

    static void scan_for_target()throws GameActionException{

    }

    static void try_mining() throws GameActionException{
        // Try to mine on squares around us.
        MapLocation me = self.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (self.canMineGold(mineLocation)) {
                    self.mineGold(mineLocation);
                }
                while (self.canMineLead(mineLocation)) {
                    self.mineLead(mineLocation);
                }
            }
        }
    }

    static void wander() throws GameActionException {
        // OPTIMIZE: a better direction distribution.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir))
            self.move(dir);
    }
}
