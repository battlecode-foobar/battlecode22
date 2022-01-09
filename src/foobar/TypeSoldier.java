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
        int minHealth = Integer.MAX_VALUE;
        RobotInfo enemy = null;
        for (RobotInfo candidate : enemies) {
            if (candidate.getType().equals(RobotType.ARCHON)) {
                enemy = candidate;
                break;
            }
            if (candidate.getHealth() < minHealth) {
                minHealth = candidate.getHealth();
                enemy = candidate;
            }
        }

        if (enemy != null) {
            MapLocation toAttack = enemy.location;
            if (self.canAttack(toAttack)) {
                self.attack(toAttack);
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    if (!self.canSenseRobotAtLocation(toAttack)) {
                        Messaging.reportDeadArchon(toAttack);
                    }
                }
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
        for (int i = 1; i < initialArchonCount - 1; i++) {
            int closest = i;
            MapLocation prev = enemyArchons[i - 1];
            for (int j = i + 1; j < initialArchonCount; j++)
                if (prev.distanceSquaredTo(enemyArchons[j]) < prev.distanceSquaredTo(enemyArchons[closest]))
                    closest = j;
            MapLocation temp = enemyArchons[i];
            enemyArchons[i] = enemyArchons[closest];
            enemyArchons[closest] = temp;
        }

        // int theUnluckyGuy = (Messaging.getGlobalTurnCount() - RUSH_PATIENCE) * initialArchonCount / (2000 - RUSH_PATIENCE);
        int idx = 0;
        while (idx < initialArchonCount - 1 && Messaging.isArchonDead(enemyArchons[idx]))
            idx++;
        MapLocation theEnemy = enemyArchons[idx];
        self.setIndicatorString("rushing " + idx + " " + theEnemy);
        PathFinding.moveToBug0(theEnemy, 80);
    }
}
