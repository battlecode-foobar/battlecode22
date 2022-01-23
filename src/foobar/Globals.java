package foobar;

import battlecode.common.*;

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
        rng = new Random(rc.getID());
        them = us.opponent();
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

    public static void log(int ID, String message) {
        // Only targeting a specific ID.
        if (self.getID() == ID && us == Team.A)
            System.out.println(message);
    }

    public static class LeadMine {
        public static int leadAmount;
        public static int isTargeted;
        public static MapLocation mineLoc;

        public LeadMine(int amount, int istargeted, MapLocation mineloc) {
            leadAmount = amount;
            isTargeted = istargeted;
            mineLoc = mineloc;
        }

        public static boolean isEmpty() {
            return (leadAmount == 0 && isTargeted == 0 && mineLoc.x == 0 && mineLoc.y == 0);
        }
    }

    public static boolean isValidMapLoc(MapLocation loc) {
        return (loc.x > -1 && loc.y > -1 && loc.x < self.getMapWidth() && loc.x < self.getMapHeight());
    }

    /**
     * Search for mines around us; if there is no friendly miner squatting on it, broadcast it
     */
    public static boolean tryBroadcastLeadMines() throws GameActionException {
        /**boolean broadcastedAnything = false;
         MapLocation[] leadMines = self.senseNearbyLocationsWithLead(self.getType().visionRadiusSquared);
         int[] leadAmount = new int[leadMines.length];
         for (int i=0;i<leadMines.length; i++)
         leadAmount[i] = self.senseLead(leadMines[i]);

         int numBroadcastedTargets = 0;
         int leadMineBroadcastBandwith = (Messaging.Min - Messaging.minerBroadcastBandMin) / 2;
         LeadMine[] broadcastedTargets = new LeadMine[leadMineBroadcastBandwith];
         // First read all the mines in the broadcasting channel
         for (int i=Messaging.minerBroadcastBandMin; i<Messaging.minerBroadcastBandMin; i+=2){
         broadcastedTargets[numBroadcastedTargets] = readLeadMineInfoAt(i);
         if (broadcastedTargets[numBroadcastedTargets++].isEmpty())
         break;
         }

         // How many have we written to the channel already
         int written_count = 0;
         int writing_index_count = 0;
         // for each locally sensed leadmine, if it has not been broadcasted, then broadcast it
         while (written_count < leadMineBroadcastBandwith - numBroadcastedTargets &&
         writing_index_count < leadMines.length){
         boolean hasBeenBroadcasted = false;
         for (int i=0; i<numBroadcastedTargets; i++){
         if (leadMines[writing_index_count].equals(broadcastedTargets[i])){
         hasBeenBroadcasted = true;
         break;
         }
         }

         // TODO: not only sense but only do so for friendly miner, but this shouldn't be a big problem.
         if (!hasBeenBroadcasted && !self.canSenseRobotAtLocation(leadMines[writing_index_count])){
         writeLeadMineInfoTo(new LeadMine(leadAmount[writing_index_count],
         0, leadMines[writing_index_count]),
         Messaging.minerBroadcastBandMin+2*(numBroadcastedTargets+(written_count)));
         self.setIndicatorString("Broadcasting lead amount"+leadAmount[writing_index_count]+"@"+
         leadMines[writing_index_count]);
         writing_index_count++;
         written_count++;
         broadcastedAnything = true;
         }
         }
         return broadcastedAnything;*/
        return false;
    }
}
