package frenchbot45;

import battlecode.common.*;

import java.util.Random;

import static frenchbot45.PathFinding.getMultiplier;

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
    public static int initialArchonCount;
    /**
     * A 0-based turn count counter.
     */
    public static int turnCount;
    /**
     * A deterministic random number generator.
     */
    public static Random rng = new Random(19260817);
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
    public static final Direction[] diagonalDirections = {
            Direction.NORTHEAST,
            Direction.SOUTHEAST,
            Direction.SOUTHWEST,
            Direction.NORTHWEST,
    };

    /**
     * An array of directions with center included.
     */
    public static final Direction[] directionsWithMe = {
            Direction.CENTER,
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
        rng = new Random(self.getID());
        // This will quickly become 0 as stepGlobals() will be called.
        turnCount = -1;
        initialArchonCount = self.getArchonCount();
    }

    /**
     * Steps and handles everything game-specific.
     */
    public static void stepGlobals() {
        turnCount++;
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

    /**
     * A moment of reflection, a chance to know one-self against the coming horrors.
     */
    public static double selfPower() {
        try {
            return evaluatePower(self.senseRobot(self.getID()));
        } catch (GameActionException e) {
            // Wherefore heroism?
            return 0;
        }
    }

    public static double selfHealth() {
        try {
            return evaluateHealth(self.senseRobot(self.getID()));
        } catch (GameActionException e) {
            // Wherefore heroism?
            return 0;
        }
    }


    /**
     * Evaluates the strength when it comes to fighting.
     * Miner: 0
     * Soldier: 2
     * Watchtower: 4
     * Sage: 3
     *
     * @param bot the RobotInfo to be evaluated.
     */
    public static double evaluatePower(RobotInfo bot) {
        double ret;
        switch (bot.getType()) {
            case SOLDIER:
                ret = 3;
                break;
            case SAGE:
                ret = 2.25;
                break;
            case WATCHTOWER:
                ret = 4;
                break;
            default:
                return 0;
        }
        ret *= getMultiplier(bot.getLocation());
        return ret;
    }

    public static double evaluateHealth(RobotInfo bot) {
        double ret;
        switch (bot.getType()) {
            case SOLDIER:
            case SAGE:
            case WATCHTOWER:
                ret = bot.getHealth();
                break;
            default:
                return 0;
        }
        return ret;
    }

    public static boolean isValidMapLoc(MapLocation loc) {
        return (loc.x > -1 && loc.y > -1 && loc.x < self.getMapWidth() && loc.x < self.getMapHeight());
    }
}
