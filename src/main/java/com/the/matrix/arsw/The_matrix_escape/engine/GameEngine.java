package com.the.matrix.arsw.The_matrix_escape.engine;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;
import com.the.matrix.arsw.The_matrix_escape.pathfinding.PathFinder;

import java.util.*;
import java.util.concurrent.*;

/**
 * Motor del juego "The Matrix Escape".
 * Orquesta la logica del juego: colocacion inicial de entidades en el tablero,
 * procesamiento de turnos (movimiento de Neo y agentes en paralelo),
 * deteccion de victoria/derrota y computo automatico de direcciones.
 * <p>
 * Los agentes se mueven en un pool de hilos (ExecutorService) para simular
 * movimiento simultaneo, mientras Neo se mueve en el hilo principal.
 */
public class GameEngine {

    public record GameConfig(int rows, int cols, int agentCount, int phoneCount, int wallCount, String mode) {}

    public enum GameStatus { PLAYING, NEO_WINS, AGENTS_WIN }

    public enum Direction {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1),
        UP_LEFT(-1, -1), UP_RIGHT(-1, 1), DOWN_LEFT(1, -1), DOWN_RIGHT(1, 1);

        final int deltaRow;
        final int deltaCol;

        Direction(int deltaRow, int deltaCol) {
            this.deltaRow = deltaRow;
            this.deltaCol = deltaCol;
        }
    }

    public record GameState(char[][] board, String status, int turn, String mode) {}

    private Board board;
    private GameConfig config;
    private volatile GameStatus gameStatus;
    private ExecutorService agentThreadPool = Executors.newCachedThreadPool();
    private final PathFinder.BFS agentPathfinder = new PathFinder.BFS();
    private final PathFinder.AStar autoPathfinder = new PathFinder.AStar();
    private int turnCount = 0;

    /**
     * Inicia una nueva partida con la configuracion dada.
     * Si el pool de hilos fue cerrado en una partida anterior, lo recrea.
     */
    public synchronized GameState start(GameConfig cfg) {
        if (agentThreadPool.isShutdown()) {
            agentThreadPool = Executors.newCachedThreadPool();
        }
        this.config = cfg;
        this.gameStatus = GameStatus.PLAYING;
        this.turnCount = 0;
        Board.reset(cfg.rows(), cfg.cols());
        board = Board.getInstance(cfg.rows(), cfg.cols());
        placeEntities();
        return getState();
    }

    /**
     * Procesa un turno completo:
     * <ol>
     *   <li>Mueve a Neo en la direccion indicada</li>
     *   <li>Verifica si Neo alcanzo un telefono (victoria)</li>
     *   <li>Mueve a todos los agentes en paralelo hacia Neo</li>
     *   <li>Verifica si algun agente atrapo a Neo (derrota)</li>
     *   <li>Incrementa el contador de turnos</li>
     * </ol>
     */
    public synchronized GameState processTurn(Direction direction) {
        if (gameStatus != GameStatus.PLAYING) return getState();

        Position neoPosition = board.findNeoPosition();
        if (neoPosition == null) return getState();

        Position nextNeoPosition = new Position(
            neoPosition.row() + direction.deltaRow,
            neoPosition.col() + direction.deltaCol
        );

        if (!board.isValidPosition(nextNeoPosition)) return getState();

        boolean phoneReached = board.getCell(nextNeoPosition.row(), nextNeoPosition.col()) == 'T';

        board.setCell(neoPosition.row(), neoPosition.col(), '.');
        board.setCell(nextNeoPosition.row(), nextNeoPosition.col(), 'N');

        if (phoneReached) {
            gameStatus = GameStatus.NEO_WINS;
            agentThreadPool.shutdownNow();
            return getState();
        }

        if (moveAgents()) {
            gameStatus = GameStatus.AGENTS_WIN;
            agentThreadPool.shutdownNow();
            return getState();
        }

        turnCount++;
        return getState();
    }

    /**
     * Calcula automaticamente la direccion optima para que Neo
     * se acerque al telefono mas cercano usando el algoritmo A*.
     */
    public Direction computeAutoDirection() {
        Position neoPosition = board.findNeoPosition();
        if (neoPosition == null) return null;

        List<Position> phones = board.findPhonePositions();
        if (phones.isEmpty()) return null;

        Position closestPhone = phones.stream()
            .min(Comparator.comparingInt(p -> neoPosition.chebyshevDistance(p)))
            .orElse(null);
        if (closestPhone == null) return null;

        List<Position> path = autoPathfinder.findPath(board, neoPosition, closestPhone);
        if (path.size() <= 1) return null;

        int deltaRow = path.get(1).row() - neoPosition.row();
        int deltaCol = path.get(1).col() - neoPosition.col();

        return Arrays.stream(Direction.values())
            .filter(d -> d.deltaRow == deltaRow && d.deltaCol == deltaCol)
            .findFirst()
            .orElse(null);
    }

    /**
     * Construye el snapshot actual del estado del juego.
     * La matriz del tablero se entrega como copia defensiva.
     */
    public GameState getState() {
        return new GameState(
            board.cloneGrid(),
            gameStatus.name(),
            turnCount,
            config != null ? config.mode() : "NONE"
        );
    }

    public GameStatus getStatus() { return gameStatus; }

    private void placeEntities() {
        Random random = new Random();
        List<Position> occupiedPositions = new ArrayList<>();
        place('N', 1, random, occupiedPositions);
        place('T', config.phoneCount(), random, occupiedPositions);
        place('A', config.agentCount(), random, occupiedPositions);
        place('#', config.wallCount(), random, occupiedPositions);
    }

    private void place(char type, int count, Random random, List<Position> occupied) {
        for (int i = 0; i < count; i++) {
            Position pos;
            do {
                pos = new Position(random.nextInt(config.rows()), random.nextInt(config.cols()));
            } while (occupied.contains(pos));
            board.setCell(pos.row(), pos.col(), type);
            occupied.add(pos);
        }
    }

    /**
     * Mueve todos los agentes concurrentemente hacia la posicion actual de Neo.
     * Cada agente se mueve en un hilo separado usando el thread pool.
     */
    private boolean moveAgents() {
        List<Future<Boolean>> agentResults = board.findAgentPositions().stream()
            .map(agentPosition -> agentThreadPool.submit(() -> moveSingleAgent(agentPosition)))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        try {
            for (Future<Boolean> agentResult : agentResults) {
                if (agentResult.get()) {
                    return true;
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
        return false;
    }

    /**
     * Mueve un agente individual un paso hacia Neo usando BFS.
     * Se ejecuta dentro del thread pool de agentes.
     */
    private boolean moveSingleAgent(Position agentPosition) {
        synchronized (board) {
            Position currentNeoPosition = board.findNeoPosition();
            if (currentNeoPosition == null) return false;

            List<Position> pathToNeo = agentPathfinder.findPath(board, agentPosition, currentNeoPosition);

            if (pathToNeo.size() > 1) {
                Position nextAgentStep = pathToNeo.get(1);

                if (board.getCell(nextAgentStep.row(), nextAgentStep.col()) == 'T') return false;

                board.setCell(agentPosition.row(), agentPosition.col(), '.');
                board.setCell(nextAgentStep.row(), nextAgentStep.col(), 'A');

                return nextAgentStep.equals(currentNeoPosition);
            }
            return false;
        }
    }
}
