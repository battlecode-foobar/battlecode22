package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;

public class TypeBuilder extends Globals {
    static int targetID = 0;
    static MapLocation targetLoc = null;

    static boolean isInNeedOfHelp(RobotInfo robot) {
        return robot.getTeam().equals(us)
                && robot.type.isBuilding()
                && robot.health < robot.type.getMaxHealth(robot.level);
    }

    public static void step() throws GameActionException {
        MapLocation here = self.getLocation();
        for (Direction dir : directionsWithMe) {
            MapLocation there = here.add(dir);
            while (self.canRepair(there))
                self.repair(there);
        }

        // If we have a target robot to track, and it is in our sight...
        if (targetLoc != null && self.canSenseRobot(targetID)) {
            // then update its location.
            targetLoc = self.senseRobot(targetID).getLocation();
        } else if (targetLoc != null & self.canSenseLocation(targetLoc)) {
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
            // Wander around.
            Direction dir = directions[rng.nextInt(directions.length)];
            if (self.canMove(dir))
                self.move(dir);
        }
    }
}
