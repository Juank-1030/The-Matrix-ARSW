package com.the.matrix.arsw.The_matrix_escape.model;

import java.util.List;

/**
 * Representa una coordenada (fila, columna) dentro del tablero.
 * Es un record inmutable de Java, por lo que genera automaticamente
 * constructor, getters row()/col(), equals() y hashCode().
 *
 * @param row fila en la matriz del tablero
 * @param col columna en la matriz del tablero
 */
public record Position(int row, int col) {

    /**
     * Calcula la distancia de Chebyshev entre esta posicion y otra.
     * En un tablero con movimiento en 8 direcciones, esta distancia representa
     * la cantidad minima de pasos necesaria para ir de un punto a otro.
     * Formula: max(|row1 - row2|, |col1 - col2|)
     *
     * @param other la posicion destino
     * @return el maximo de las diferencias absolutas entre filas y columnas
     */
    public int chebyshevDistance(Position other) {
        return Math.max(
            Math.abs(row - other.row),
            Math.abs(col - other.col)
        );
    }

    /**
     * Genera las 8 posiciones vecinas ortogonales y diagonales.
     * NOTA: este metodo NO verifica si las posiciones estan dentro del tablero
     * o si son transitables; esa responsabilidad es de Board.isValidPosition()
     * y Board.getNeighbors().
     *
     * @return lista con 8 posiciones alrededor de esta
     */
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
