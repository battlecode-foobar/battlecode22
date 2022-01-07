package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;

/**
 * Main controller logic for an Archon unit.
 */
public strictfp class TypeMiner extends Globals {
    static State state;

    enum State {
        WANDERING,
        TARGETED,
    }

    public static void init() {
        // A miner is born wandering by default.
        state = State.WANDERING;
    }

    public static void step() throws GameActionException {
        if (firstRun())
            init();
        switch (state) {
            case WANDERING:
                wander();
                break;
        }
    }

    static void wander() throws GameActionException {
        // TODO: add logic to actually mine.

        // OPTIMIZE: a better direction distribution.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (self.canMove(dir))
            self.move(dir);
    }
}
