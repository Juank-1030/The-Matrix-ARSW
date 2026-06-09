package com.the.matrix.arsw.The_matrix_escape.model;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Tablero del juego "The Matrix Escape".
 * Implementa el patron Singleton: solo existe una unica instancia del tablero
 * en toda la aplicacion.
 * <p>
 * Internamente guarda una matriz de caracteres (char[][]) donde cada celda
 * representa un elemento del escenario:
 * <ul>
 *   <li>'.' espacio vacio</li>
 *   <li>'N' Neo</li>
 *   <li>'A' Agente</li>
 *   <li>'T' Telefono (meta de Neo)</li>
 *   <li>'#' Muro (intransitable)</li>
 * </ul>
 * Todos los metodos que leen o escriben la matriz son synchronized para
 * garantizar seguridad entre hilos (Neo se mueve en el hilo principal,
 * los Agentes en hilos separados).
 */
public class Board {

    private static Board instance;

    private final int rows;
    private final int cols;
    private final char[][] grid;

    /**
     * Constructor privado: solo se llama desde getInstance() o reset().
     * Inicializa toda la matriz con el caracter '.' (vacio).
     */
    private Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            Arrays.fill(grid[r], '.');
        }
    }

    /**
     * Obtiene la instancia unica del tablero (Singleton).
     * Si no existe o si las dimensiones cambiaron, crea una nueva.
     */
    public static synchronized Board getInstance(int rows, int cols) {
        if (instance == null || instance.rows != rows || instance.cols != cols) {
            instance = new Board(rows, cols);
        }
        return instance;
    }

    /** Reinicia el tablero creando una nueva instancia. */
    public static synchronized void reset(int rows, int cols) {
        instance = new Board(rows, cols);
    }

    /** Lee el caracter de una celda especifica. */
    public synchronized char getCell(int row, int col) {
        return grid[row][col];
    }

    /** Escribe un caracter en una celda especifica. */
    public synchronized void setCell(int row, int col, char value) {
        grid[row][col] = value;
    }

    /**
     * Verifica si una posicion esta dentro de los limites del tablero
     * y no es un muro ('#').
     */
    public synchronized boolean isValidPosition(Position pos) {
        return pos.row() >= 0 && pos.row() < rows
            && pos.col() >= 0 && pos.col() < cols
            && grid[pos.row()][pos.col()] != '#';
    }

    /**
     * Escanea toda la matriz y devuelve una lista con las posiciones
     * que contienen el caracter especificado.
     */
    public synchronized List<Position> scan(char target) {
        return IntStream.range(0, rows)
            .boxed()
            .flatMap(r -> IntStream.range(0, cols)
                .filter(c -> grid[r][c] == target)
                .mapToObj(c -> new Position(r, c)))
            .collect(Collectors.toList());
    }

    /** @return posicion de Neo, o null si no esta en el tablero */
    public Position findNeoPosition() {
        return scan('N').stream().findFirst().orElse(null);
    }

    /** @return lista con las posiciones de todos los telefonos */
    public List<Position> findPhonePositions() {
        return scan('T');
    }

    /** @return lista con las posiciones de todos los agentes */
    public List<Position> findAgentPositions() {
        return scan('A');
    }

    /**
     * Obtiene los vecinos transitables de una posicion (hasta 8 direcciones).
     * Filtra posiciones fuera del tablero o que son muros.
     * Incluye los telefonos como transitables (para Neo).
     */
    public synchronized List<Position> getNeighbors(Position pos) {
        return pos.getNeighbors().stream()
            .filter(this::isValidPosition)
            .collect(Collectors.toList());
    }

    /**
     * Obtiene los vecinos transitables excluyendo telefonos.
     * Es usado por los Agentes, que no pueden pisar telefonos.
     */
    public synchronized List<Position> getWalkableNeighbors(Position pos) {
        return getNeighbors(pos).stream()
            .filter(p -> grid[p.row()][p.col()] != 'T')
            .collect(Collectors.toList());
    }

    /**
     * Crea y devuelve una copia profunda de la matriz del tablero.
     * Se usa para entregar el estado a la UI sin exponer la matriz original.
     */
    public synchronized char[][] cloneGrid() {
        char[][] copy = new char[rows][cols];
        for (int r = 0; r < rows; r++) {
            System.arraycopy(grid[r], 0, copy[r], 0, cols);
        }
        return copy;
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
}
