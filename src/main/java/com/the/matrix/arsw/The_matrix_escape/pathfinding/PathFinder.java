package com.the.matrix.arsw.The_matrix_escape.pathfinding;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;

import java.util.*;

public interface PathFinder {

    List<Position> findPath(Board board, Position start, Position goal);

    static List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
        List<Position> path = new LinkedList<>();
        for (Position node = current; node != null; node = cameFrom.get(node))
            path.add(0, node);
        return path;
    }

    class AStar implements PathFinder {

        private record Node(Position pos, int g, int h) {}

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingInt(n -> n.g + n.h));
            Set<Position> closedSet = new HashSet<>();
            Map<Position, Position> cameFrom = new HashMap<>();
            Map<Position, Integer> gScore = new HashMap<>();

            openSet.add(new Node(start, 0, start.chebyshevDistance(goal)));
            gScore.put(start, 0);

            while (!openSet.isEmpty()) {
                Node current = openSet.poll();
                Position pos = current.pos;
                if (pos.equals(goal)) return reconstructPath(cameFrom, pos);
                closedSet.add(pos);

                board.getNeighbors(pos).stream()
                    .filter(n -> !closedSet.contains(n))
                    .forEach(n -> {
                        int tentativeG = gScore.get(pos) + 1;
                        if (!gScore.containsKey(n) || tentativeG < gScore.get(n)) {
                            cameFrom.put(n, pos);
                            gScore.put(n, tentativeG);
                            openSet.add(new Node(n, tentativeG, tentativeG + n.chebyshevDistance(goal)));
                        }
                    });
            }
            return Collections.emptyList();
        }
    }

    class BFS implements PathFinder {

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            Queue<Position> queue = new LinkedList<>();
            Map<Position, Position> cameFrom = new HashMap<>();
            Set<Position> visited = new HashSet<>();

            queue.add(start);
            visited.add(start);
            cameFrom.put(start, null);

            while (!queue.isEmpty()) {
                Position current = queue.poll();
                if (current.equals(goal)) return reconstructPath(cameFrom, current);

                board.getWalkableNeighbors(current).stream()
                    .filter(n -> !visited.contains(n))
                    .forEach(n -> {
                        visited.add(n);
                        cameFrom.put(n, current);
                        queue.add(n);
                    });
            }
            return Collections.emptyList();
        }
    }
}
