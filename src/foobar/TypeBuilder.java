package foobar;

import battlecode.common.*;

public class TypeBuilder extends Globals {
    static int targetID = 0;
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

    public static void searchForTarget() throws GameActionException{
        // Try find a position that is:
            // Not occupied by a builder
            // In the diagonal of an archon or watchtower
        // Try find a watchtower or archon
        RobotInfo[] nearbyBots = self.senseNearbyRobots(RobotType.BUILDER.visionRadiusSquared);
        if (nearbyBots.length == 0)
            return;
        for (RobotInfo bot:nearbyBots){
            if (bot.getType() != RobotType.WATCHTOWER && bot.getType() != RobotType.ARCHON)
                continue;
            MapLocation botLoc = bot.getLocation();
            for (Direction diagDirection: diagonalDirections){
                MapLocation tentativeLoc = botLoc.add(diagDirection);
                if (validTarget(tentativeLoc) && self.canSenseLocation(tentativeLoc) && self.onTheMap(tentativeLoc))
                {
                    targetLoc = tentativeLoc;
                    return;
                }
            }
        }
    }

    public static void reserveWatchtowerBudget() throws GameActionException{
        if (self.readSharedArray(Messaging.BUILDWATCHTOWER_START) != 1)
            self.writeSharedArray(Messaging.BUILDWATCHTOWER_START, 1);
    }

    public static void cancelWatchtowerBudget() throws GameActionException{
        if (self.readSharedArray(Messaging.BUILDWATCHTOWER_START) != 0)
            self.writeSharedArray(Messaging.BUILDWATCHTOWER_START, 0);
    }

    public static void step() throws GameActionException {
        MapLocation here = self.getLocation();
        tryRepairAround();
        if (targetLoc == null){
            cancelWatchtowerBudget();
            PathFinding.wander();
            searchForTarget();
        }
        else{
            // if we are at target
            if (here.distanceSquaredTo(targetLoc)<= 2) {
                reserveWatchtowerBudget();
                self.setIndicatorString("I'm near target"+targetLoc+self.getActionCooldownTurns());
                if (self.canBuildRobot(RobotType.WATCHTOWER, here.directionTo(targetLoc))) {
                    self.setIndicatorString("Can build towards"+here.directionTo(targetLoc)+targetLoc);
                    self.buildRobot(RobotType.WATCHTOWER, here.directionTo(targetLoc));
                }
                if (!validTarget(targetLoc))
                    if (self.canSenseRobotAtLocation(targetLoc) &&
                            self.senseRobotAtLocation(targetLoc).getType() == RobotType.WATCHTOWER &&
                            self.senseRobotAtLocation(targetLoc).getMode() == RobotMode.PROTOTYPE){
                        while (self.canRepair(targetLoc))
                            self.repair(targetLoc);
                    }
                    else {
                        cancelWatchtowerBudget();
                        PathFinding.wander();
                        searchForTarget();
                        return;
                    }
            }
            else{
                cancelWatchtowerBudget();
                self.setIndicatorString("Moving for target"+targetLoc);
                PathFinding.moveToBug0(targetLoc);
            }
        }

        /**
        // If we have a target robot to track, and it is in our sight...
        if (targetLoc != null && self.canSenseRobot(targetID)) {
            // then update its location.
            targetLoc = self.senseRobot(targetID).getLocation();
        } else if (targetLoc != null && self.canSenseLocation(targetLoc)) {
            // Otherwise, if we cannot see the robot, but we can sense the location, we lost the track of target robot.
            targetLoc = null;
            targetID = 0;
        }
        // However, if even targetLoc is out of sight, then the target robot may still be there, just that we can't see
        // it yet. In this case we do not change the target.

        if (targetLoc == null) {
            // Find a new target if there isn't one already.
            RobotInfo[] robots = self.senseNearbyRobots();
            int distance = Integer.MAX_VALUE;
            for (RobotInfo robot : robots) {
                if (isInNeedOfHelp(robot)) {
                    if (here.distanceSquaredTo(robot.location) < distance) {
                        targetLoc = robot.getLocation();
                        targetID = robot.getID();
                        distance = here.distanceSquaredTo(robot.location);
                    }
                }
            }
        }

        if (targetLoc != null) {
            PathFinding.moveToBug0(targetLoc);
        } else {
            PathFinding.wander();
        }
         */
    }
}
