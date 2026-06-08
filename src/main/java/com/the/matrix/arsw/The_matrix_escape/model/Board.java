package com.the.matrix.arsw.The_matrix_escape.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Board {

    private static Board instance;
    private final int rows;
    private final int cols;
    private final char[][] grid;

    private Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) Arrays.fill(grid[r], '.');
    }

    public static synchronized Board getInstance(int rows, int cols) {
        if (instance == null || instance.rows != rows || instance.cols != cols)
            instance = new Board(rows, cols);
        return instance;
    }

    public static synchronized void reset(int rows, int cols) {
        instance = new Board(rows, cols);
    }

    public synchronized char getCell(int row, int col) { return grid[row][col]; }
    public synchronized void setCell(int row, int col, char value) { grid[row][col] = value; }

    public synchronized boolean isValidPosition(Position pos) {
        return pos.row() >= 0 && pos.row() < rows
            && pos.col() >= 0 && pos.col() < cols
            && grid[pos.row()][pos.col()] != '#';
    }

    public synchronized List<Position> scan(char target) {
        return IntStream.range(0, rows)
            .boxed()
            .flatMap(r -> IntStream.range(0, cols)
                .filter(c -> grid[r][c] == target)
                .mapToObj(c -> new Position(r, c)))
            .collect(Collectors.toList());
    }

    public Position findNeoPosition() { return scan('N').stream().findFirst().orElse(null); }
    public List<Position> findPhonePositions() { return scan('T'); }
    public List<Position> findAgentPositions() { return scan('A'); }

    public synchronized List<Position> getNeighbors(Position pos) {
        return pos.getNeighbors().stream()
            .filter(this::isValidPosition)
            .collect(Collectors.toList());
    }

    public synchronized List<Position> getWalkableNeighbors(Position pos) {
        return getNeighbors(pos).stream()
            .filter(p -> grid[p.row()][p.col()] != 'T')
            .collect(Collectors.toList());
    }

    public synchronized char[][] cloneGrid() {
        char[][] copy = new char[rows][cols];
        for (int r = 0; r < rows; r++)
            System.arraycopy(grid[r], 0, copy[r], 0, cols);
        return copy;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
