package emobot;

import battlecode.common.*;

public class TypeBuilder extends Globals {
    static int targetSharedArrayIndex = -1;
    static MapLocation targetLoc = null;

    static boolean isInNeedOfHelp(RobotInfo robot) {
        return robot.getTeam().equals(us)
                && robot.type.isBuilding()
                && robot.health < robot.type.getMaxHealth(robot.level);
    }

    static void tryRepairAround() throws GameActionException{
        return;
        /*
        MapLocation here = self.getLocation();
        for (Direction dir : directionsWithMe) {
            MapLocation there = here.add(dir);
            while (self.canRepair(there))
                self.repair(there);
        }*/
    }

    public static boolean validTarget(MapLocation loc) throws GameActionException{
        if (!self.canSenseRobotAtLocation(loc))
            return true;
        RobotInfo robotAtLoc = self.senseRobotAtLocation(loc);
        if (robotAtLoc.getType() == RobotType.BUILDER || robotAtLoc.getType() == RobotType.ARCHON ||
            robotAtLoc.getType() == RobotType.WATCHTOWER || robotAtLoc.getType() == RobotType.MINER)
            return false;
        return true;
    }

    public static void readSharedArrayForTarget() throws GameActionException{
        for (int index=Messaging.BUILDWATCHTOWER_START; index<Messaging.BUILDWATCHTOWER_END; index++){
            int raw = self.readSharedArray(index);
            boolean isclaimed = raw % 8 / 4 == 1;
            boolean isIntentionallyWritten = raw % 2 == 1;
            // Do not consider those mines for which another builder has already claimed
            if (isclaimed || !isIntentionallyWritten)
                continue;
            // Read the valid target location and ID
            targetLoc = Messaging.decodeLocation(raw / 8);
            targetSharedArrayIndex = index;
            return;
        }
    }

    public static void step() throws GameActionException {
        MapLocation here = self.getLocation();
        tryRepairAround();
        if (targetLoc == null){
            readSharedArrayForTarget();
        }
        RobotInfo[] bots = self.senseNearbyRobots();
        if(self.canMutate(bots[0].getLocation())) {
            self.mutate(bots[0].getLocation());
            self.disintegrate();
        }

//        // If we have a target
//        if (targetLoc != null){
//            int raw = self.readSharedArray(targetSharedArrayIndex);
//            // Assert that targetLoc and location read from array are equal
//            assert (Messaging.decodeLocation(raw / 8).equals(targetLoc));
//
//            // If we are immediately besides our target
//            if (here.distanceSquaredTo(targetLoc)<= 2) {
//                // The block is not occupied at all: send message for archon to reserve lead
//                if (!self.canSenseRobotAtLocation(targetLoc))
//                    self.writeSharedArray(targetSharedArrayIndex,
//                        Messaging.encodeWatchtowerLocationAndClaimArrivalStatus(targetLoc, true, true));
//                // The block is occupied by some robot
//                if (self.canSenseRobotAtLocation(targetLoc)){
//                    RobotInfo botAtTarget = self.senseRobotAtLocation(targetLoc);
//                    // The block is occupied by tower
//                    if (botAtTarget.getType() == RobotType.WATCHTOWER && botAtTarget.getTeam() == us) {
//                        // The block is occupied by prototype tower: repair it
//                        if (botAtTarget.getMode() == RobotMode.PROTOTYPE)
//                            while (self.canRepair(targetLoc))
//                                self.repair(targetLoc);
//                        // The block is occupied by finished tower: remove entry from shared array and reset target
//                        if (botAtTarget.getMode() == RobotMode.TURRET)
//                        {
//                            self.writeSharedArray(targetSharedArrayIndex, 0);
//                            targetSharedArrayIndex = -1;
//                            targetLoc = null;
//                        }
//                    }
//                    else{
//                        // The block is occupied by other stuff: other stuff
//                        PathFinding.wander();
//                    }
//                }
//            } else{
//                self.setIndicatorString("Moving for target"+targetLoc);
//                PathFinding.moveTo(targetLoc);
//            }
//        }
//
//        /**
//        // If we have a target robot to track, and it is in our sight...
//        if (targetLoc != null && self.canSenseRobot(targetID)) {
//            // then update its location.
//            targetLoc = self.senseRobot(targetID).getLocation();
//        } else if (targetLoc != null && self.canSenseLocation(targetLoc)) {
//            // Otherwise, if we cannot see the robot, but we can sense the location, we lost the track of target robot.
//            targetLoc = null;
//            targetID = 0;
//        }
//        // However, if even targetLoc is out of sight, then the target robot may still be there, just that we can't see
//        // it yet. In this case we do not change the target.
//
//        if (targetLoc == null) {
//            // Find a new target if there isn't one already.
//            RobotInfo[] robots = self.senseNearbyRobots();
//            int distance = Integer.MAX_VALUE;
//            for (RobotInfo robot : robots) {
//                if (isInNeedOfHelp(robot)) {
//                    if (here.distanceSquaredTo(robot.location) < distance) {
//                        targetLoc = robot.getLocation();
//                        targetID = robot.getID();
//                        distance = here.distanceSquaredTo(robot.location);
//                    }
//                }
//            }
//        }
//
//        if (targetLoc != null) {
//            PathFinding.moveToBug0(targetLoc);
//        } else {
//            PathFinding.wander();
//        }
//         */
    }
}
