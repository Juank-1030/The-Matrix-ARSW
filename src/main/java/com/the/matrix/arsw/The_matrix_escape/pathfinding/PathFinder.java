package com.the.matrix.arsw.The_matrix_escape.pathfinding;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;

import java.util.*;

public interface PathFinder {

    List<Position> findPath(Board board, Position start, Position goal);

    class AStar implements PathFinder {

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            PriorityQueue<Node> openSet = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.g + n.h)
            );
            Set<Position> closedSet = new HashSet<>();
            Map<Position, Position> cameFrom = new HashMap<>();
            Map<Position, Integer> gScore = new HashMap<>();

            openSet.add(new Node(start, 0, start.chebyshevDistance(goal)));
            gScore.put(start, 0);

            while (!openSet.isEmpty()) {
                Node current = openSet.poll();
                Position pos = current.pos;

                if (pos.equals(goal)) {
                    return reconstructPath(cameFrom, pos);
                }

                closedSet.add(pos);

                for (Position neighbor : board.getNeighbors(pos)) {
                    if (closedSet.contains(neighbor)) continue;

                    int tentativeG = gScore.get(pos) + 1;

                    if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                        cameFrom.put(neighbor, pos);
                        gScore.put(neighbor, tentativeG);
                        int f = tentativeG + neighbor.chebyshevDistance(goal);
                        openSet.add(new Node(neighbor, tentativeG, f));
                    }
                }
            }
            return Collections.emptyList();
        }

        private record Node(Position pos, int g, int h) {}

        private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
            List<Position> path = new LinkedList<>();
            Position node = current;
            while (node != null) {
                path.add(0, node);
                node = cameFrom.get(node);
            }
            return path;
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

                if (current.equals(goal)) {
                    return reconstructPath(cameFrom, current);
                }

                for (Position neighbor : board.getNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        cameFrom.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }
            return Collections.emptyList();
        }

        private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
            List<Position> path = new LinkedList<>();
            Position node = current;
            while (node != null) {
                path.add(0, node);
                node = cameFrom.get(node);
            }
            return path;
        }
    }
}
