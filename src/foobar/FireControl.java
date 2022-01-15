package foobar;

import battlecode.common.RobotInfo;

/**
 * Functions relevant to fire control.
 */
public class FireControl extends Globals {
    /**
     * Evaluates the strength when it comes to fighting.
     *
     * @param bot the RobotInfo to be evaluated.
     */
    public static int evaluatePower(RobotInfo bot) {
        switch (bot.getType()) {
            case SOLDIER:
            case SAGE:
                return 2;
            case WATCHTOWER:
                return 4;
        }
        return 0;
    }

    /**
     * Evaluate the priority of the given enemy bot.
     * @param bot The robot to be evaluated.
     * @return The priority. Should not exceed 4 bits.
     */
    public static int evaluatePriority(RobotInfo bot) {
        switch (bot.getType()) {
            case SOLDIER:
                return 3;
            case WATCHTOWER:
                return 2;
            case MINER:
                return 1;
        }
        return 0;
    }
}
