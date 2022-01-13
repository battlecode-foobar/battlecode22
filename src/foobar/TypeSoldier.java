package foobar;

import battlecode.common.*;

public class TypeSoldier extends Globals {
    // Even the aged oak will fall to the tempest’s winds.
    static final int RUSH_PATIENCE = 30;

    static MapLocation assemblyTarget;
    public static MapLocation[] enemyArchons;
    static boolean rusher;
    static boolean chasingEnemy;

    public static void step() throws GameActionException {
        if (firstRun()) {
            // RUSH_PATIENCE = us.equals(Team.A) ? 400 : 500;
            rusher = Messaging.getTotalSoldierCount() < RUSH_PATIENCE;
            chasingEnemy = false;
            calculateEnemyArchons();
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();

        findEnemyAndAttack();

        // The sin is not in being outmatched, but in failing to recognize it.
        PathFinding.tryRetreat(13,-1);

        if (!rusher) {
            supportFrontier();
        } else {
            updateEnemyArchons();
            if (Messaging.getTotalSoldierCount() < RUSH_PATIENCE) {
                self.setIndicatorString("assemble at " + assemblyTarget);
/*
                int closest = Messaging.getClosestArchonTo(self.getLocation());
                MapLocation target = Messaging.getArchonLocation(closest);
                if (closest > 16)
                    PathFinding.moveToBug0(target);
                else
                    PathFinding.spreadOut();
*/
                PathFinding.moveToBug0(assemblyTarget);
            } else {
                rush();
            }
        }
    }

    /**
     * Finds an enemy and attacks.
     */
    public static void findEnemyAndAttack() throws GameActionException {
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
    }

    static void supportFrontier() throws GameActionException {
        MapLocation frontier = Messaging.getMostImportantFrontier();
        if (frontier != null) {
            self.setIndicatorLine(self.getLocation(), frontier, 0, 255, 255);
            if (frontier.distanceSquaredTo(self.getLocation()) < 3600)
                PathFinding.moveToBug0(frontier);
            else
                PathFinding.wanderAvoidingObstacle(PathFinding.defaultObstacleThreshold);
        } else {
            PathFinding.spreadOut();
/*
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
                PathFinding.wanderAvoidingObstacle(PathFinding.defaultObstacleThreshold);
            }
*/
        }
    }

    public static void calculateEnemyArchons() throws GameActionException {
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

        enemyArchons = new MapLocation[4];
        if (initialArchonCount == 0)
            return;
        for (int i = 0; i < initialArchonCount; i++) {
            enemyArchons[i] = new MapLocation(oneLessWidth - ourArchons[i].x,
                    partialSymmetry ? ourArchons[i].y : oneLessHeight - ourArchons[i].y);
        }
        optimizeEnemyArchonOrder();
        MapLocation producerLoc = Messaging.getArchonLocation(Messaging.getClosestArchonTo(enemyArchons[0]));
        assemblyTarget = new MapLocation(
                (2 * producerLoc.x + enemyArchons[0].x) / 3,
                (2 * producerLoc.y + enemyArchons[0].y) / 3
        );
    }

    static void updateEnemyArchons() throws GameActionException {
        int idx = 0;
        for (int i = Messaging.ENEMY_ARCHON_START; i < Messaging.ENEMY_ARCHON_END; i++) {
            int raw = self.readSharedArray(i);
            if (raw == Messaging.IMPOSSIBLE_LOCATION)
                continue;
            enemyArchons[idx++] = Messaging.decodeLocation(raw);
        }
        optimizeEnemyArchonOrder();
    }

    static void optimizeEnemyArchonOrder() {
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
    }

    static void rush() throws GameActionException {
        // int theUnluckyGuy = (Messaging.getGlobalTurnCount() - RUSH_PATIENCE) * initialArchonCount / (2000 - RUSH_PATIENCE);
        int idx = 0;
        while (idx < initialArchonCount - 1 && Messaging.isEnemyArchonDead(enemyArchons[idx]))
            idx++;

        MapLocation theEnemy = enemyArchons[idx];
        // self.setIndicatorString("rushing " + idx + " " + theEnemy);

        if (self.canSenseRobotAtLocation(theEnemy)) {
            RobotInfo info = self.senseRobotAtLocation(theEnemy);
            if (!info.getType().equals(RobotType.ARCHON)) {
                rusher = false;
                findEnemyAndAttack();
            }
        }

        PathFinding.moveToBug0(theEnemy);
    }
}
