package foobar;

import battlecode.common.*;

/**
 * Functions relevant to fire control.
 */
public class FireControl extends Globals {
    /**
     * Evaluates the strength when it comes to fighting.
     *
     * @param bot the RobotInfo to be evaluated.
     */
    public static double evaluatePower(RobotInfo bot) {
        double ret = 0;
        switch (bot.getType()) {
            case SOLDIER:
            case SAGE:
                ret = 2;
                break;
            case WATCHTOWER:
                ret = 4;
                break;
        }
        double rubble = 0;
        try {
            rubble = 1 + self.senseRubble(bot.getLocation()) / 10.0;
        } catch (GameActionException e) {
            rubble = 99999999.0;
        }
        ret /= rubble;
//        if(self.getRoundNum() <= 150 && self.getRoundNum() >= 100)
//        {
//            log(bot.getLocation() + "");
//            log(ret + " " + (bot.getLocation()));
//        }
        return ret;
    }

    /**
     * Evaluate the priority of the given enemy bot.
     * @param bot The robot to be evaluated.
     * @return The priority. Should not exceed 4 bits.
     */
    public static int evaluatePriority(RobotInfo bot) {
        switch (bot.getType()) {
            case SOLDIER:
                return 4;
            case WATCHTOWER:
                return 3;
            case MINER:
                return 2;
            case ARCHON:
                return 1;
        }
        return 0;
    }
}
