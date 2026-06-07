package com.the.matrix.arsw.The_matrix_escape.model;

import java.util.List;

public record Position(int row, int col) {

    public int chebyshevDistance(Position other) {
        return Math.max(
            Math.abs(row - other.row),
            Math.abs(col - other.col)
        );
    }

    public List<Position> getNeighbors() {
        return List.of(
            new Position(row - 1, col - 1),
            new Position(row - 1, col),
            new Position(row - 1, col + 1),
            new Position(row, col - 1),
            new Position(row, col + 1),
            new Position(row + 1, col - 1),
            new Position(row + 1, col),
            new Position(row + 1, col + 1)
        );
    }
}
