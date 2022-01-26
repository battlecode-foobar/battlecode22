package frenchbot45;

import battlecode.common.*;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */
public strictfp class RobotPlayer extends Globals {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */
    static int turnCount = 0;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * It is like the main function for your robot. If this method returns, the robot dies!
     *
     * @param rc The RobotController object. You use it to perform actions from this robot, and to get
     *           information on its current status. Essentially your portal to interacting with the world.
     **/
    @SuppressWarnings({"unused", "InfiniteLoopStatement"})
    public static void run(RobotController rc) throws GameActionException {
        initGlobals(rc);

        while (true) {
            try {
                stepGlobals();
                // OPTIMIZE: if we run low on available bytecode counts, probably consider factoring this out of the
                // loop?
                switch (rc.getType()) {
                    case ARCHON:
                        TypeArchon.step();
                        break;
                    case MINER:
                        TypeMiner.step();
                        break;
                    case SOLDIER:
                        TypeSoldier.step();
                        break;
                    case LABORATORY:
                        break;
                    case WATCHTOWER:
                        TypeWatchTower.step();
                    case BUILDER:
                        TypeBuilder.step();
                        break;
                    case SAGE:
                        // The mighty sword arm anchored by holy purpose: a zealous warrior.
                        TypeSoldier.step();
                        break;
                }
            } catch (GameActionException e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            } catch (Exception e) {
                System.out.println("SERIOUS ERROR" + rc.getType() + " Exception");
                e.printStackTrace();
            } finally {
                Clock.yield();
            }
        }
    }
}
