package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class TypeSoldier extends Globals {
    public static void step() throws GameActionException {
        int radius = self.getType().actionRadiusSquared;
        RobotInfo[] enemies = self.senseNearbyRobots(radius, them);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (self.canAttack(toAttack)) {
                self.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir)) {
            self.move(dir);
        }
    }
}
