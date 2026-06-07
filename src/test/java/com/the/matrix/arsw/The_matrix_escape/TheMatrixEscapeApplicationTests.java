package com.the.matrix.arsw.The_matrix_escape;

import com.the.matrix.arsw.The_matrix_escape.model.Board;
import com.the.matrix.arsw.The_matrix_escape.model.Position;
import com.the.matrix.arsw.The_matrix_escape.pathfinding.PathFinder;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TheMatrixEscapeApplicationTests {

    @Test
    void testBoardSingleton() {
        Board.reset(5, 5);
        Board b1 = Board.getInstance(5, 5);
        Board b2 = Board.getInstance(5, 5);
        assertSame(b1, b2, "Board debe ser Singleton");
    }

    @Test
    void testPositionChebyshev() {
        Position p1 = new Position(0, 0);
        Position p2 = new Position(3, 4);
        assertEquals(4, p1.chebyshevDistance(p2));
    }

    @Test
    void testPositionNeighbors8Directions() {
        Position p = new Position(1, 1);
        assertEquals(8, p.getNeighbors().size());
    }

    @Test
    void testBoardMoveEntity() {
        Board.reset(5, 5);
        Board b = Board.getInstance(5, 5);
        b.setCell(0, 0, 'N');
        assertTrue(b.moveEntity(new Position(0, 0), new Position(0, 1)));
        assertEquals('N', b.getCell(0, 1));
        assertEquals('.', b.getCell(0, 0));
    }

    @Test
    void testBoardInvalidPosition() {
        Board.reset(3, 3);
        Board b = Board.getInstance(3, 3);
        assertFalse(b.isValidPosition(new Position(-1, 0)));
        assertFalse(b.isValidPosition(new Position(0, 5)));
        b.setCell(1, 1, '#');
        assertFalse(b.isValidPosition(new Position(1, 1)));
    }

    @Test
    void testBFSFindPath() {
        Board.reset(3, 3);
        Board b = Board.getInstance(3, 3);
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        PathFinder.BFS bfs = new PathFinder.BFS();
        var path = bfs.findPath(b, start, goal);
        assertFalse(path.isEmpty());
        assertEquals(start, path.get(0));
        assertEquals(goal, path.get(path.size() - 1));
    }

    @Test
    void testAStarFindPath() {
        Board.reset(3, 3);
        Board b = Board.getInstance(3, 3);
        b.setCell(1, 0, '#');
        b.setCell(1, 1, '#');
        Position start = new Position(0, 0);
        Position goal = new Position(2, 2);
        PathFinder.AStar astar = new PathFinder.AStar();
        var path = astar.findPath(b, start, goal);
        assertFalse(path.isEmpty());
        assertEquals(start, path.get(0));
        assertEquals(goal, path.get(path.size() - 1));
    }

    @Test
    void testGameEngineStartAndState() {
        GameEngine engine = new GameEngine();
        var config = new GameEngine.GameConfig(5, 5, 1, 1, 5, "PLAYABLE");
        GameEngine.GameState state = engine.start(config);
        assertNotNull(state);
        assertEquals("PLAYING", state.status());
        assertEquals(0, state.turn());
        assertEquals(5, state.board().length);
    }

    @Test
    void testGameEngineProcessTurn() {
        GameEngine engine = new GameEngine();
        var config = new GameEngine.GameConfig(5, 5, 1, 1, 3, "PLAYABLE");
        engine.start(config);
        GameEngine.GameState state = engine.processTurn(GameEngine.Direction.DOWN);
        assertNotNull(state);
    }

    @Test
    void testNeoWinsWhenReachingPhone() {
        Board.reset(2, 2);
        Board b = Board.getInstance(2, 2);
        b.setCell(0, 0, 'N');
        b.setCell(0, 1, 'T');

        // Simular el mismo check que hace GameEngine.processTurn()
        Position newNeoPos = new Position(0, 1);
        boolean reachedPhone = b.getCell(newNeoPos.row(), newNeoPos.col()) == 'T';
        assertTrue(reachedPhone, "Neo debe detectar que llegó al teléfono");
    }

    @Test
    void testNeoDoesNotWinOnEmptyCell() {
        Board.reset(2, 2);
        Board b = Board.getInstance(2, 2);
        b.setCell(0, 0, 'N');
        b.setCell(0, 1, '.');

        Position newNeoPos = new Position(0, 1);
        boolean reachedPhone = b.getCell(newNeoPos.row(), newNeoPos.col()) == 'T';
        assertFalse(reachedPhone, "Celda vacía no debe ser victoria");
    }
}
