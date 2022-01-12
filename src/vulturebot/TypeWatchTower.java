package vulturebot;

import battlecode.common.GameActionException;
import foobar.Globals;
import foobar.Messaging;
import foobar.TypeSoldier;

public class TypeWatchTower extends Globals {
    public static void step() throws GameActionException {
        foobar.Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();
        TypeSoldier.findEnemyAndAttack();
    }
}
