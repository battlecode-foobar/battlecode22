package vulturebot;

import battlecode.common.GameActionException;

public class TypeWatchTower extends Globals {
    public static void step() throws GameActionException {
        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        TypeSoldier.findEnemyAndAttack();
    }
}
