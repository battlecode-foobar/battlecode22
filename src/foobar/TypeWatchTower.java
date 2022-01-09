package foobar;

import battlecode.common.GameActionException;

public class TypeWatchTower extends Globals {
    public static void step() throws GameActionException {
        Messaging.reportAllEnemiesAround();
        TypeSoldier.findEnemyAndAttack();
    }
}
