package frenchbot;

import battlecode.common.*;

public class TypeSoldier extends Globals {
    public static void step() throws GameActionException {
        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        findEnemyAndAttack();
        // The sin is not in being outmatched, but in failing to recognize it.
        if (!PathFinding.tryRetreat(13,-1))
            supportFrontier();
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
        for (RobotInfo robot : self.senseNearbyRobots(-1, them)) {
            if (robot.getType().equals(RobotType.ARCHON)) {
                PathFinding.moveTo(robot.getLocation());
                return;
            }
        }
        MapLocation frontier = Messaging.getMostImportantFrontier();
        if (frontier != null) {
            self.setIndicatorLine(self.getLocation(), frontier, 0, 255, 255);
            if (frontier.distanceSquaredTo(self.getLocation()) < 3600)
                PathFinding.moveTo(frontier);
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
}
