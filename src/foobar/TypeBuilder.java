package foobar;

import battlecode.common.*;

public class TypeBuilder extends Globals {
    static int targetSharedArrayIndex = -1;
    static MapLocation targetLoc = null;
    static MapLocation birthLoc = null;
    static int turnCount = 0;

    static boolean isInNeedOfHelp(RobotInfo robot) {
        return robot.getTeam().equals(us)
                && robot.type.isBuilding()
                && robot.health < robot.type.getMaxHealth(robot.level);
    }

    static void tryRepairAround() throws GameActionException{
        MapLocation here = self.getLocation();
        for (Direction dir : directionsWithMe) {
            MapLocation there = here.add(dir);
            while (self.canRepair(there))
                self.repair(there);
        }
    }

    public static void scanSharedArrayForTarget() throws GameActionException{
        for (int index=Messaging.BUILDWATCHTOWER_START; index<Messaging.BUILDWATCHTOWER_END; index++){
            int raw = self.readSharedArray(index);
            boolean isclaimed = raw % 8 / 4 == 1;
            boolean isIntentionallyWritten = raw % 2 == 1;
            MapLocation tentativeLoc = Messaging.decodeLocation(raw / 8);
            self.setIndicatorString(""+raw);
            // Do not consider those mines for which another builder has already claimed (or unvalid ones)
            if (isclaimed || !isIntentionallyWritten || !isValidMapLoc(tentativeLoc) ||
                    !tentativeLoc.isWithinDistanceSquared(birthLoc, self.getType().visionRadiusSquared))
                continue;
            // Read the valid target location and ID; claim it
            targetLoc = tentativeLoc;
            targetSharedArrayIndex = index;
            self.writeSharedArray(targetSharedArrayIndex,
                    Messaging.encodeWatchtowerLocationAndClaimArrivalStatus(targetLoc, true, false));
            return;
        }
    }

    public static void step() throws GameActionException {
        turnCount += 1;
        MapLocation here = self.getLocation();

        if (turnCount == 1)
            birthLoc = self.getLocation();

        if (targetLoc == null){
            scanSharedArrayForTarget();
        }

        // If we have a target
        if (targetLoc != null){
            int raw = self.readSharedArray(targetSharedArrayIndex);
            // Assert that targetLoc and location read from array are equal
            assert (Messaging.decodeLocation(raw / 8).equals(targetLoc));

            // If we are immediately besides our target
            if (here.distanceSquaredTo(targetLoc)<= 2) {
                // The block is not occupied at all: send message for archon to reserve lead; if can build, build
                if (!self.canSenseRobotAtLocation(targetLoc)) {
                    self.writeSharedArray(targetSharedArrayIndex,
                            Messaging.encodeWatchtowerLocationAndClaimArrivalStatus(targetLoc, true, true));
                    if (self.canBuildRobot(RobotType.WATCHTOWER, self.getLocation().directionTo(targetLoc)))
                        self.buildRobot(RobotType.WATCHTOWER, self.getLocation().directionTo(targetLoc));
                    self.setIndicatorString("Trying to build@"+targetLoc);
                }
                // The block is occupied by some robot
                if (self.canSenseRobotAtLocation(targetLoc)){
                    RobotInfo botAtTarget = self.senseRobotAtLocation(targetLoc);
                    // The block is occupied by tower
                    if (botAtTarget.getType() == RobotType.WATCHTOWER && botAtTarget.getTeam() == us) {
                        // The block is occupied by prototype tower: repair it
                        if (botAtTarget.getMode() == RobotMode.PROTOTYPE) {
                            while (self.canRepair(targetLoc))
                                self.repair(targetLoc);
                            self.setIndicatorString("Repairing " + targetLoc);
                        }
                        // The block is occupied by finished tower: remove entry from shared array and reset target
                        if (botAtTarget.getMode() == RobotMode.TURRET)
                        {
                            self.setIndicatorString("Turret built, null target");
                            self.writeSharedArray(targetSharedArrayIndex, 0);
                            targetSharedArrayIndex = -1;
                            targetLoc = null;
                        }
                    }
                    else{
                        // The block is occupied by other stuff: other stuff
                        PathFinding.wander();
                    }
                }
            } else{
                self.setIndicatorString("Moving for target"+targetLoc);
                PathFinding.moveTo(targetLoc);
            }
        }
        else{
            // Builder-specific wandering; do not move too away from birthplace
            Direction direction = directions[rng.nextInt(directions.length)];
            MapLocation tentativeLoc = self.getLocation().add(direction);
            if (self.canMove(direction) &&
                    birthLoc.isWithinDistanceSquared(tentativeLoc, self.getType().visionRadiusSquared))
                self.move(direction);
        }

        Messaging.reportAllEnemiesAround();
        Messaging.reportAllMinesAround();

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
