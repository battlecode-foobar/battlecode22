package foobar;

import battlecode.common.*;

public class TypeSoldier extends Globals {
    static final int RUSH_PATIENCE = 900;

    static int initialArchonCount;

    public static void step() throws GameActionException {
        if (firstRun()) {
            initialArchonCount = self.getArchonCount();
        }

        int radius = self.getType().actionRadiusSquared;
        RobotInfo[] enemies = self.senseNearbyRobots(radius, them);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (self.canAttack(toAttack)) {
                self.attack(toAttack);
            }
        }

        if (Messaging.getGlobalTurnCount() < RUSH_PATIENCE) {
            // Also try to move randomly.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (self.canMove(dir)) {
                self.move(dir);
            }
        } else {
            rush();
        }
    }

    static void rush() throws GameActionException {
        MapLocation[] enemyArchons = new MapLocation[initialArchonCount];
        for (int i = 0; i < initialArchonCount; i++) {
            MapLocation ourArchon = Messaging.readSharedLocation(Messaging.getArchonOffset(i));
            enemyArchons[i] = new MapLocation(self.getMapWidth() - ourArchon.x - 1, self.getMapHeight() - ourArchon.y - 1);
        }
        int theUnluckyGuy = (Messaging.getGlobalTurnCount() - RUSH_PATIENCE) * initialArchonCount / (2000 - RUSH_PATIENCE);
        MapLocation theEnemy = enemyArchons[theUnluckyGuy];
        self.setIndicatorString("rushing " + theUnluckyGuy + " " + theEnemy);
        PathFinding.moveToBug0(theEnemy, 80);
    }
}
