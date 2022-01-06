package foobar;

import battlecode.common.GameActionException;

/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeMiner extends Globals {

    public static void init() {
        // TODO: initialize stuffs.
    }

    public static void step() throws GameActionException {
        if (turnCount == 0)
            init();

    }
}
