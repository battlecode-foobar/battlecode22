package foobar;

import battlecode.common.Direction;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Team;

import java.util.Random;

/**
 * Global variables.
 */
public class Globals {
    /**
     * The RobotController singleton.
     */
    public static RobotController self;
    public static Team us;
    public static Team them;
    public static MapLocation here;
    /**
     * A 0-based turn count counter.
     */
    public static int turnCount;
    /**
     * A deterministic random number generator.
     */
    public static final Random rng = new Random(19260817);
    /**
     * An array of all direction.
     */
    public static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    /**
     * Initialize everything.
     *
     * @param rc The RobotController instance given in RobotPlayer.class
     */
    public static void initGlobals(RobotController rc) {
        self = rc;
        us = rc.getTeam();
        them = us.opponent();
        // This will quickly become 0 as stepGlobals() will be called.
        turnCount = -1;
        here = self.getLocation();
    }

    /**
     * Steps and handles everything game-specific.
     */
    public static void stepGlobals() {
        turnCount++;
        here = self.getLocation();
    }

    /**
     * Checks if we are at the first turn (which indicates need for initialization)
     *
     * @return If we are at the first turn.
     */
    public static boolean firstRun() {
        return turnCount == 0;
    }

    /**
     * One-side logging.
     *
     * @param message The message to be logged.
     */
    public static void log(String message) {
        // Change this to keep only the logging of one side.
        if (us == Team.A)
            System.out.println(message);
    }
}
