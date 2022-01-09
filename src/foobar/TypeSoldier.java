package foobar;

import battlecode.common.*;

public class TypeSoldier extends Globals {
    static final int RUSH_PATIENCE = 800;

    enum Role {
        RUSHER,
        REINFORCEMENT,
        WANDERER,
    }

    static MapLocation assemblyTarget;
    static MapLocation[] enemyArchons;

    public static void step() throws GameActionException {
        if (firstRun()) {
            // RUSH_PATIENCE = us.equals(Team.A) ? 400 : 500;
            calculateEnemyArchons();
        }

        Messaging.reportAllEnemiesAround();

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
            MapLocation toAttack = enemy.getLocation();
            if (self.canAttack(toAttack)) {
                self.attack(toAttack);
                if (enemy.getType().equals(RobotType.ARCHON)) {
                    if (!self.canSenseRobotAtLocation(toAttack)) {
                        Messaging.reportDeadArchon(toAttack);
                    }
                }
            }
        }

        if (us.equals(Team.A)) {
            MapLocation frontier = Messaging.getMostImportantFrontier();
            if (frontier != null) {
                self.setIndicatorLine(self.getLocation(), frontier, 0, 255, 255);
                PathFinding.moveToBug0(frontier, 80);
            } else {
                int minDis = Integer.MAX_VALUE;
                MapLocation minDisLoc = null;
                for (int i = 0; i < initialArchonCount; i++) {
                    MapLocation ourArchon = Messaging.getArchonLocation(i);
                    if (self.getLocation().distanceSquaredTo(ourArchon) < minDis) {
                        minDis = self.getLocation().distanceSquaredTo(ourArchon);
                        minDisLoc = ourArchon;
                    }
                }
                if (minDisLoc != null && minDis > 16) {
                    PathFinding.moveToBug0(minDisLoc);
                } else {
                    PathFinding.wanderAvoidingObstacle(PathFinding.DEFAULT_OBSTACLE_THRESHOLD);
                }
            }
        } else {
            if (Messaging.getGlobalTurnCount() < RUSH_PATIENCE) {
                PathFinding.moveToBug0(assemblyTarget, 80);
            } else {
                rush();
            }
        }
    }

    static void calculateEnemyArchons() throws GameActionException {
        boolean partialSymmetry = false;
        MapLocation[] ourArchons = new MapLocation[initialArchonCount];
        for (int i = 0; i < initialArchonCount; i++)
            ourArchons[i] = Messaging.getArchonLocation(i);
        int oneLessWidth = self.getMapWidth() - 1;
        int oneLessHeight = self.getMapHeight() - 1;
        for (int i = 0; i < initialArchonCount; i++) {
            for (int j = i + 1; j < initialArchonCount; j++) {
                if (ourArchons[i].x + ourArchons[j].x == oneLessWidth
                        && ourArchons[i].y + ourArchons[j].y == oneLessHeight) {
                    partialSymmetry = true;
                    break;
                }
            }
            if (partialSymmetry)
                break;
        }

        enemyArchons = new MapLocation[initialArchonCount];
        int sumX = 0, sumY = 0;
        for (int i = 0; i < initialArchonCount; i++) {
            sumX += ourArchons[i].x;
            sumY += ourArchons[i].y;
            enemyArchons[i] = new MapLocation(oneLessWidth - ourArchons[i].x,
                    partialSymmetry ? ourArchons[i].y : oneLessHeight - ourArchons[i].y);
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
        int weight = (initialArchonCount + 1) / 2;
        assemblyTarget = new MapLocation(
                (sumX + enemyArchons[0].x * weight) / (initialArchonCount + weight),
                (sumY + enemyArchons[0].y * weight) / (initialArchonCount + weight)
        );
    }

    static void rush() throws GameActionException {
        // int theUnluckyGuy = (Messaging.getGlobalTurnCount() - RUSH_PATIENCE) * initialArchonCount / (2000 - RUSH_PATIENCE);
        int idx = 0;
        while (idx < initialArchonCount - 1 && Messaging.isArchonDead(enemyArchons[idx]))
            idx++;
        MapLocation theEnemy = enemyArchons[idx];
        self.setIndicatorString("rushing " + idx + " " + theEnemy);
        PathFinding.moveToBug0(theEnemy, 80);
    }
}
