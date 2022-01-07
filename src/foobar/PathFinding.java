package foobar;

import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;

import java.util.*;

/**
 * Very naive path finding.
 */
public class PathFinding extends Globals {
    /**
     * A path.
     */
    public static class Path implements Comparable {
        int cost;
        MapLocation loc;
        Path precursor;

        public Path(int cost, MapLocation loc, Path prev) {
            this.cost = cost;
            this.loc = loc;
            this.precursor = prev;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Path path = (Path) o;
            return loc.equals(path.loc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loc);
        }

        @Override
        public int compareTo(Object o) {
            return ((Path) o).cost - cost;
        }
    }

    static int costOf(MapLocation loc) throws GameActionException {
        return 1 + self.senseRubble(loc) / 10;
    }

    public static void findPathTo(MapLocation dest) throws GameActionException {
        if (!self.canSenseLocation(dest)) {
            return;
        }
        PriorityQueue<Path> paths = new PriorityQueue<>();



/*
        HashMap<MapLocation, Path> visited = new HashMap<>();
        HashMap<MapLocation, Path> unvisited = new HashMap<>();
        for (Direction dir : directions) {
            MapLocation next = self.getLocation().add(dir);
            if (self.canSenseLocation(next)) {
                Path newPath = new Path(costOf(next), next, null);
                unvisited.put(next, newPath);
            }
        }
        while (!unvisited.isEmpty()) {
            Path minCostPath = null;
            MapLocation minCostLoc = null;
            for (HashMap.Entry<MapLocation, Path> entry: unvisited.entrySet()) {
                if (minCostPath == null || entry.getValue().cost < minCostPath.cost) {
                    minCostPath = entry.getValue();
                    minCostLoc = entry.getKey();
                }
            }
            if (minCostLoc.equals(dest)) {
                System.out.println("cost of path: " + minCostPath.cost);
                while (minCostPath != null) {
                    System.out.println(minCostPath.loc);
                    minCostPath = minCostPath.precursor;
                }
                break;
            }
            unvisited.remove(minCostLoc);
            visited.put(minCostLoc, minCostPath);
            // GREAT EFFICIENCY PROBLEMS
            for (Direction dir : directions) {
                MapLocation next = minCostPath.loc.add(dir);
                if (!self.canSenseLocation(next) || visited.containsKey(next))
                    continue;
                int newCost = minCostPath.cost + costOf(next);
                if (!unvisited.containsKey(next) || newCost < unvisited.get(next).cost)
                    unvisited.put(next, new Path(newCost, next, minCostPath));
            }
        }
*/
    }
}
