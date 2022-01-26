package frenchbot45;

import battlecode.common.*;

public class TypeSoldier extends Globals {
    public static void step() throws GameActionException {
        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        findEnemyAndAttack();
        // The sin is not in being outmatched, but in failing to recognize it.
        if (!PathFinding.tryRetreat(20))
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
            PathFinding.moveTo(frontier);
        } else {
            PathFinding.spreadOut();
        }
    }
}
