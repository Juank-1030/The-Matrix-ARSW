package com.the.matrix.arsw.The_matrix_escape.pathfinding;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;

import java.util.*;

/**
 * Define el contrato para algoritmos de busqueda de caminos en el tablero.
 * Contiene dos implementaciones internas:
 * AStar (usado por Neo en modo automatico) y BFS (usado por los Agentes).
 */
public interface PathFinder {

    /**
     * Encuentra un camino desde la posicion inicial hasta la posicion objetivo.
     *
     * @param board el tablero del juego
     * @param start posicion de inicio
     * @param goal  posicion destino
     * @return lista ordenada de posiciones desde start hasta goal,
     *         o lista vacia si no hay camino posible
     */
    List<Position> findPath(Board board, Position start, Position goal);

    /**
     * Reconstruye el camino desde la meta hasta el inicio usando el mapa de predecesores.
     * Retrocede desde la meta siguiendo las referencias hasta llegar al inicio,
     * invirtiendo el orden para obtener el camino en sentido correcto (inicio -> meta).
     *
     * @param predecessors mapa de predecesores (posicion -> desdeDondeSeLlego)
     * @param current      posicion desde la cual comenzar la reconstruccion
     * @return lista ordenada desde el inicio hasta current
     */
    static List<Position> reconstructPath(Map<Position, Position> predecessors, Position current) {
        List<Position> path = new LinkedList<>();
        for (Position node = current; node != null; node = predecessors.get(node)) {
            path.add(0, node);
        }
        return path;
    }

    /**
     * Implementacion del algoritmo A* (A-Star).
     * Usado por: Neo en modo automatico (computeAutoDirection).
     * Caracteristica: busqueda informada que usa una heuristica de distancia
     * Chebyshev para estimar el costo restante hacia la meta.
     * <p>
     * Funcion de evaluacion: f(n) = g(n) + h(n)
     * donde g(n) es el costo real desde el inicio hasta el nodo n,
     * y h(n) es la heuristica (distancia Chebyshev) desde n hasta la meta.
     */
    class AStar implements PathFinder {

        private record Node(Position pos, int g, int h) {}

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            // Cola de prioridad ordenada por f = g + h (menor primero).
            // Asi siempre exploramos primero el nodo mas prometedor.
            PriorityQueue<Node> frontier = new PriorityQueue<>(Comparator.comparingInt(n -> n.g + n.h));
            // Conjunto de posiciones ya evaluadas para no reprocesarlas.
            Set<Position> explored = new HashSet<>();
            // Mapa que guarda "desde que posicion llegue a esta".
            Map<Position, Position> predecessors = new HashMap<>();
            // Mejor costo conocido (g) para llegar a cada posicion.
            Map<Position, Integer> costFromStart = new HashMap<>();

            frontier.add(new Node(start, 0, start.chebyshevDistance(goal)));
            costFromStart.put(start, 0);

            while (!frontier.isEmpty()) {
                Node current = frontier.poll();
                Position currentPos = current.pos;

                if (currentPos.equals(goal)) {
                    return reconstructPath(predecessors, currentPos);
                }

                explored.add(currentPos);

                // Obtener vecinos transitables (incluye telefonos, porque Neo puede pisarlos).
                board.getNeighbors(currentPos).stream()
                    .filter(neighbor -> !explored.contains(neighbor))
                    .forEach(neighbor -> {
                        int newCost = costFromStart.get(currentPos) + 1;
                        if (!costFromStart.containsKey(neighbor) || newCost < costFromStart.get(neighbor)) {
                            predecessors.put(neighbor, currentPos);
                            costFromStart.put(neighbor, newCost);
                            frontier.add(new Node(neighbor, newCost,
                                newCost + neighbor.chebyshevDistance(goal)));
                        }
                    });
            }

            return Collections.emptyList();
        }
    }

    /**
     * Implementacion del algoritmo BFS (Breadth-First Search).
     * Usado por: Agentes para perseguir a Neo.
     * Caracteristica: busqueda no informada que garantiza el camino mas corto
     * en terminos de cantidad de pasos. Explora el tablero por niveles
     * completos usando una cola FIFO.
     */
    class BFS implements PathFinder {

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            // Cola FIFO: el primer nodo descubierto es el primero en explorarse.
            // Esto asegura que exploramos todos los nodos de un nivel antes
            // de pasar al siguiente.
            Queue<Position> nodeQueue = new LinkedList<>();
            // Guarda "desde que posicion llegue a esta" para reconstruir el camino.
            Map<Position, Position> predecessors = new HashMap<>();
            // Conjunto de posiciones ya visitadas para evitar ciclos infinitos.
            Set<Position> explored = new HashSet<>();

            nodeQueue.add(start);
            explored.add(start);
            predecessors.put(start, null);

            while (!nodeQueue.isEmpty()) {
                Position current = nodeQueue.poll();

                if (current.equals(goal)) {
                    return reconstructPath(predecessors, current);
                }

                // Obtener vecinos transitables EXCLUYENDO telefonos,
                // porque los agentes no pueden pisarlos.
                board.getWalkableNeighbors(current).stream()
                    .filter(neighbor -> !explored.contains(neighbor))
                    .forEach(neighbor -> {
                        // Marcar como visitado inmediatamente para evitar
                        // que dos nodos encolen el mismo vecino.
                        explored.add(neighbor);
                        predecessors.put(neighbor, current);
                        nodeQueue.add(neighbor);
                    });
            }

            return Collections.emptyList();
        }
    }
}
