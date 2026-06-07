package com.the.matrix.arsw.The_matrix_escape.engine;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;
import com.the.matrix.arsw.The_matrix_escape.pathfinding.PathFinder;

import java.util.*;
import java.util.concurrent.*;

public class GameEngine {

    public record GameConfig(
        int rows, int cols,
        int agentCount, int phoneCount, int wallCount,
        String mode
    ) {}

    public enum GameStatus { PLAYING, NEO_WINS, AGENTS_WIN }

    public enum Direction {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1),
        UP_LEFT(-1, -1), UP_RIGHT(-1, 1), DOWN_LEFT(1, -1), DOWN_RIGHT(1, 1);
        final int dr, dc;
        Direction(int dr, int dc) { this.dr = dr; this.dc = dc; }
    }

    public record GameState(char[][] board, String status, int turn, String mode) {}

    private Board board;
    private GameConfig config;
    private volatile GameStatus status;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final PathFinder.BFS bfs = new PathFinder.BFS();
    private final PathFinder.AStar astar = new PathFinder.AStar();
    private int turn = 0;

    public synchronized GameState start(GameConfig cfg) {
        this.config = cfg;
        this.status = GameStatus.PLAYING;
        this.turn = 0;

        Board.reset(cfg.rows(), cfg.cols());
        board = Board.getInstance(cfg.rows(), cfg.cols());
        placeEntities();

        return getState();
    }

    private void placeEntities() {
        Random rand = new Random();
        List<Position> occupied = new ArrayList<>();

        Position neoPos = randomFreePosition(rand, occupied);
        board.setCell(neoPos.row(), neoPos.col(), 'N');
        occupied.add(neoPos);

        for (int i = 0; i < config.phoneCount(); i++) {
            Position phonePos = randomFreePosition(rand, occupied);
            board.setCell(phonePos.row(), phonePos.col(), 'T');
            occupied.add(phonePos);
        }

        for (int i = 0; i < config.agentCount(); i++) {
            Position agentPos = randomFreePosition(rand, occupied);
            board.setCell(agentPos.row(), agentPos.col(), 'A');
            occupied.add(agentPos);
        }

        for (int i = 0; i < config.wallCount(); i++) {
            Position wallPos = randomFreePosition(rand, occupied);
            board.setCell(wallPos.row(), wallPos.col(), '#');
            occupied.add(wallPos);
        }
    }

    private Position randomFreePosition(Random rand, List<Position> occupied) {
        int rows = config.rows(), cols = config.cols();
        Position pos;
        do {
            pos = new Position(rand.nextInt(rows), rand.nextInt(cols));
        } while (occupied.contains(pos));
        return pos;
    }

    public synchronized GameState processTurn(Direction dir) {
        if (status != GameStatus.PLAYING) return getState();

        Position neoPos = board.findNeoPosition();
        if (neoPos == null) return getState();

        Position newNeoPos = new Position(
            neoPos.row() + dir.dr,
            neoPos.col() + dir.dc
        );

        if (!board.isValidPosition(newNeoPos)) return getState();

        // Verificar si Neo llegó a un teléfono ANTES de sobrescribir la celda
        boolean reachedPhone = board.getCell(newNeoPos.row(), newNeoPos.col()) == 'T';

        board.setCell(neoPos.row(), neoPos.col(), '.');
        board.setCell(newNeoPos.row(), newNeoPos.col(), 'N');

        if (reachedPhone) {
            status = GameStatus.NEO_WINS;
            threadPool.shutdownNow();
            return getState();
        }

        List<Position> agentPositions = board.findAgentPositions();
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Position agentPos : agentPositions) {
            futures.add(threadPool.submit(() -> {
                synchronized (board) {
                    Position currentNeoPos = board.findNeoPosition();
                    if (currentNeoPos == null) return false;
                    List<Position> path = bfs.findPath(board, agentPos, currentNeoPos);

                    if (path.size() > 1) {
                        Position nextStep = path.get(1);
                        board.setCell(agentPos.row(), agentPos.col(), '.');
                        board.setCell(nextStep.row(), nextStep.col(), 'A');
                        return nextStep.equals(currentNeoPos);
                    }
                    return false;
                }
            }));
        }

        try {
            for (Future<Boolean> f : futures) {
                if (f.get()) {
                    status = GameStatus.AGENTS_WIN;
                    threadPool.shutdownNow();
                    return getState();
                }
            }
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }

        turn++;
        return getState();
    }

    public Direction computeAutoDirection() {
        Position neoPos = board.findNeoPosition();
        if (neoPos == null) return null;
        List<Position> phones = board.findPhonePositions();
        if (phones.isEmpty()) return null;

        Position nearestPhone = phones.stream()
            .min(Comparator.comparingInt(p -> neoPos.chebyshevDistance(p)))
            .orElse(null);
        if (nearestPhone == null) return null;

        List<Position> path = astar.findPath(board, neoPos, nearestPhone);
        if (path.size() <= 1) return null;

        int dr = path.get(1).row() - neoPos.row();
        int dc = path.get(1).col() - neoPos.col();
        for (Direction d : Direction.values()) {
            if (d.dr == dr && d.dc == dc) return d;
        }
        return null;
    }

    public GameState getState() {
        return new GameState(
            board.cloneGrid(),
            status.name(),
            turn,
            config != null ? config.mode() : "NONE"
        );
    }

    public GameStatus getStatus() {
        return status;
    }
}
