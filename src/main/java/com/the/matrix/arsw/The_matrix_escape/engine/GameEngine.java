package com.the.matrix.arsw.The_matrix_escape.engine;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;
import com.the.matrix.arsw.The_matrix_escape.pathfinding.PathFinder;

import java.util.*;
import java.util.concurrent.*;

public class GameEngine {

    public record GameConfig(int rows, int cols, int agentCount, int phoneCount, int wallCount, String mode) {}

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
    private ExecutorService threadPool = Executors.newCachedThreadPool();
    private final PathFinder.BFS bfs = new PathFinder.BFS();
    private final PathFinder.AStar astar = new PathFinder.AStar();
    private int turn = 0;

    public synchronized GameState start(GameConfig cfg) {
        if (threadPool.isShutdown())
            threadPool = Executors.newCachedThreadPool();
        this.config = cfg;
        this.status = GameStatus.PLAYING;
        this.turn = 0;
        Board.reset(cfg.rows(), cfg.cols());
        board = Board.getInstance(cfg.rows(), cfg.cols());
        placeEntities();
        return getState();
    }

    private void place(char type, int count, Random rand, List<Position> occupied) {
        for (int i = 0; i < count; i++) {
            Position pos;
            do { pos = new Position(rand.nextInt(config.rows()), rand.nextInt(config.cols())); }
            while (occupied.contains(pos));
            board.setCell(pos.row(), pos.col(), type);
            occupied.add(pos);
        }
    }

    private void placeEntities() {
        Random rand = new Random();
        List<Position> occupied = new ArrayList<>();
        place('N', 1, rand, occupied);
        place('T', config.phoneCount(), rand, occupied);
        place('A', config.agentCount(), rand, occupied);
        place('#', config.wallCount(), rand, occupied);
    }

    public synchronized GameState processTurn(Direction dir) {
        if (status != GameStatus.PLAYING) return getState();

        Position neoPos = board.findNeoPosition();
        if (neoPos == null) return getState();

        Position newNeoPos = new Position(neoPos.row() + dir.dr, neoPos.col() + dir.dc);
        if (!board.isValidPosition(newNeoPos)) return getState();

        boolean reachedPhone = board.getCell(newNeoPos.row(), newNeoPos.col()) == 'T';

        board.setCell(neoPos.row(), neoPos.col(), '.');
        board.setCell(newNeoPos.row(), newNeoPos.col(), 'N');

        if (reachedPhone) {
            status = GameStatus.NEO_WINS;
            threadPool.shutdownNow();
            return getState();
        }

        if (moveAgents()) {
            status = GameStatus.AGENTS_WIN;
            threadPool.shutdownNow();
            return getState();
        }

        turn++;
        return getState();
    }

    private boolean moveAgents() {
        List<Future<Boolean>> futures = board.findAgentPositions().stream()
            .map(agentPos -> threadPool.submit(() -> moveSingleAgent(agentPos)))
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        try {
            for (Future<Boolean> f : futures)
                if (f.get()) return true;
        } catch (Exception e) { Thread.currentThread().interrupt(); }
        return false;
    }

    private boolean moveSingleAgent(Position agentPos) {
        synchronized (board) {
            Position currentNeoPos = board.findNeoPosition();
            if (currentNeoPos == null) return false;
            List<Position> path = bfs.findPath(board, agentPos, currentNeoPos);
            if (path.size() > 1) {
                Position nextStep = path.get(1);
                if (board.getCell(nextStep.row(), nextStep.col()) == 'T') return false;
                board.setCell(agentPos.row(), agentPos.col(), '.');
                board.setCell(nextStep.row(), nextStep.col(), 'A');
                return nextStep.equals(currentNeoPos);
            }
            return false;
        }
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
        return Arrays.stream(Direction.values())
            .filter(d -> d.dr == dr && d.dc == dc)
            .findFirst()
            .orElse(null);
    }

    public GameState getState() {
        return new GameState(board.cloneGrid(), status.name(), turn, config != null ? config.mode() : "NONE");
    }

    public GameStatus getStatus() { return status; }
}
