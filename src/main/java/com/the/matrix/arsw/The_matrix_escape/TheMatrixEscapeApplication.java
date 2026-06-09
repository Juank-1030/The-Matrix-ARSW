package com.the.matrix.arsw.The_matrix_escape;

import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.Direction;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameConfig;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameState;

import java.util.*;
import java.util.stream.*;

import static com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.*;

/**
 * Punto de entrada de la aplicacion "The Matrix Escape".
 * Proporciona la interfaz de usuario por consola con dos modos:
 * jugable (control manual con teclado) y simulacion (Neo automatico con A*).
 * El tablero se renderiza con colores ANSI y coordenadas.
 */
public class TheMatrixEscapeApplication {

    private static final GameEngine engine = new GameEngine();
    private static final Scanner scanner = new Scanner(System.in);

    private static final String RESET      = "\u001B[0m";
    private static final String GREEN      = "\u001B[92m";
    private static final String RED        = "\u001B[91m";
    private static final String BLUE       = "\u001B[94m";
    private static final String GRAY       = "\u001B[90m";
    private static final String YELLOW     = "\u001B[93m";
    private static final String CYAN       = "\u001B[96m";
    private static final String BOLD       = "\u001B[1m";
    private static final String CLEAR_SCREEN = "\033[H\033[2J";

    /**
     * Punto de entrada de la aplicacion.
     * Muestra la bienvenida, solicita configuracion e inicia el modo correspondiente.
     */
    public static void main(String[] args) {
        welcome();
        GameConfig config = askConfig();
        engine.start(config);
        if (config.mode().equals("SIMULATION")) {
            simLoop();
        } else {
            playLoop();
        }
    }

    /** Muestra el banner de bienvenida con el titulo del juego. */
    private static void welcome() {
        System.out.print(CLEAR_SCREEN);
        System.out.println(BOLD + CYAN + """
  ╔══════════════════════════════════════════╗
  ║       MATRIX - EL ESCAPE                ║
  ║   Neo debe escapar al telefono...       ║
  ║   ...antes de que los Agentes lo atrapen║
  ╚══════════════════════════════════════════╝""" + RESET);
        sleep(1500);
    }

    /**
     * Solicita al usuario los parametros de configuracion de la partida.
     * @return objeto GameConfig con los valores ingresados (o valores por defecto)
     */
    private static GameConfig askConfig() {
        System.out.print(CLEAR_SCREEN + BOLD + YELLOW + "INSTRUMENTACION" + RESET + "\n");
        return new GameConfig(
            readInt("Filas (8): ", 8),
            readInt("Columnas (8): ", 8),
            readInt("Agentes (2): ", 2),
            readInt("Telefonos (1): ", 1),
            readInt("Muros (10): ", 10),
            readMode()
        );
    }

    /**
     * Lee un entero desde la consola con un mensaje y valor por defecto.
     * @param prompt mensaje mostrado al usuario
     * @param defaultValue valor por defecto si la entrada no es valida
     */
    private static int readInt(String prompt, int defaultValue) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(scanner.nextLine().trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Pregunta al usuario el modo de juego.
     * @return "SIMULATION" si el usuario eligio 2, "PLAYABLE" en caso contrario
     */
    private static String readMode() {
        System.out.print(("\n1-" + GREEN + "Jugable" + RESET + "  2-" + CYAN + "Simulacion" + RESET + "\nModo (1): "));
        return scanner.nextLine().trim().equals("2") ? "SIMULATION" : "PLAYABLE";
    }

    /**
     * Bucle del modo jugable.
     * El usuario controla a Neo con:
     * W/S/A/D = arriba/abajo/izquierda/derecha
     * Q/E/Z/X = diagonales, K = salir, P = pausa
     */
    private static void playLoop() {
        System.out.println(BOLD + GREEN + "\nJUGABLE  " + RESET
            + "WASD ortogonal | Q/E/Z/X diagonal | K=salir | P=pausa"
            + YELLOW + "\nEnter..." + RESET);
        try { scanner.nextLine(); } catch (Exception e) {}

        while (true) {
            render();
            if (!engine.getState().status().equals("PLAYING")) return;

            String userInput = scanner.nextLine().trim().toLowerCase();
            if (userInput.equals("k")) {
                System.out.println(YELLOW + "Adios." + RESET);
                return;
            }
            if (userInput.equals("p")) {
                System.out.println(YELLOW + "Pausa. Enter..." + RESET);
                scanner.nextLine();
                continue;
            }

            Direction direction = switch (userInput) {
                case "w","8" -> Direction.UP;
                case "s","2" -> Direction.DOWN;
                case "a","4" -> Direction.LEFT;
                case "d","6" -> Direction.RIGHT;
                case "q","7" -> Direction.UP_LEFT;
                case "e","9" -> Direction.UP_RIGHT;
                case "z","1" -> Direction.DOWN_LEFT;
                case "x","3" -> Direction.DOWN_RIGHT;
                default -> null;
            };

            if (direction == null) {
                System.out.println(RED + "W,A,S,D,Q,E,Z,X" + RESET);
                sleep(800);
                continue;
            }

            GameState gameResult = engine.processTurn(direction);
            if (!gameResult.status().equals("PLAYING")) {
                render();
                result(gameResult.status());
                return;
            }
        }
    }

    /**
     * Bucle del modo simulacion (paso a paso).
     * Neo se mueve automaticamente con A* hacia el telefono mas cercano.
     * El usuario avanza turno a turno con Enter, o ingresa 'a' para modo automatico.
     */
    private static void simLoop() {
        System.out.println(BOLD + CYAN + "\nSIMULACION" + RESET
            + "  Neo=A* | Agentes=BFS"
            + YELLOW + "\nEnter..." + RESET);
        try { scanner.nextLine(); } catch (Exception e) {}

        while (true) {
            render();
            GameState gameState = engine.getState();
            if (!gameState.status().equals("PLAYING")) {
                result(gameState.status());
                return;
            }

            System.out.print(CYAN + "[Enter=paso | A=auto | K=salir]: " + RESET);
            String userInput = scanner.nextLine().trim().toLowerCase();
            if (userInput.equals("k")) {
                System.out.println(YELLOW + "Adios." + RESET);
                return;
            }
            if (userInput.equals("a")) {
                autoSim();
                return;
            }

            Direction direction = engine.computeAutoDirection();
            if (direction == null) {
                System.out.println(RED + "Neo sin ruta." + RESET);
                return;
            }

            GameState gameResult = engine.processTurn(direction);
            if (!gameResult.status().equals("PLAYING")) {
                render();
                result(gameResult.status());
                return;
            }
        }
    }

    /**
     * Modo automatico: Neo se mueve solo con A* hasta que el juego termina
     * o el usuario presiona K para detener.
     * Cada turno tiene una pausa de 400ms para visualizar el movimiento.
     */
    private static void autoSim() {
        System.out.println(CYAN + "\nAuto (K=detener)" + RESET);
        sleep(1000);

        while (true) {
            render();
            GameState gameState = engine.getState();
            if (!gameState.status().equals("PLAYING")) {
                result(gameState.status());
                return;
            }

            Direction direction = engine.computeAutoDirection();
            if (direction == null) {
                System.out.println(RED + "Neo sin ruta." + RESET);
                return;
            }

            GameState gameResult = engine.processTurn(direction);
            if (!gameResult.status().equals("PLAYING")) {
                render();
                result(gameResult.status());
                return;
            }

            try {
                if (System.in.available() > 0
                    && Character.toLowerCase((char) System.in.read()) == 'k') {
                    System.out.println(YELLOW + "Detenido." + RESET);
                    return;
                }
            } catch (java.io.IOException e) {
            }

            sleep(400);
        }
    }

    /**
     * Renderiza el estado actual del tablero en la consola con colores ANSI.
     * Muestra encabezado con modo/turno/estado, numeros de fila y columna,
     * el tablero coloreado, leyenda y coordenadas de cada entidad.
     */
    private static void render() {
        System.out.print(CLEAR_SCREEN);
        GameState state = engine.getState();
        char[][] boardGrid = state.board();
        int rows = boardGrid.length;
        int cols = rows > 0 ? boardGrid[0].length : 0;

        String modeIcon = state.mode().equals("SIMULATION") ? "SIM" : "JUG";
        String statusColor = state.status().equals("PLAYING") ? GREEN : RED;
        System.out.println(BOLD + CYAN + "  MATRIX" + RESET
            + "  " + modeIcon
            + "  Turno: " + state.turn()
            + "  " + statusColor + state.status() + RESET);

        System.out.print("     ");
        IntStream.range(0, cols).forEach(c -> System.out.print(GRAY + c % 10 + " " + RESET));
        System.out.println("\n");

        IntStream.range(0, rows).forEach(row -> {
            System.out.print(GRAY + row % 10 + "    " + RESET);
            IntStream.range(0, cols).forEach(col -> {
                char cell = boardGrid[row][col];
                String color = switch (cell) {
                    case 'N' -> GREEN + BOLD;
                    case 'A' -> RED + BOLD;
                    case 'T' -> BLUE + BOLD;
                    case '#' -> GRAY;
                    default  -> GRAY;
                };
                System.out.print(color + cell + " " + RESET);
            });
            System.out.println();
        });

        String neoCoord = findEntity(boardGrid, 'N');
        String phoneCoord = findEntity(boardGrid, 'T');
        String agentsCoords = findAllEntities(boardGrid, 'A');

        System.out.println("\n  " + GREEN + "N" + RESET + "=Neo  "
            + RED + "A" + RESET + "=Agente  "
            + BLUE + "T" + RESET + "=Telefono  "
            + GRAY + "#" + RESET + "=Muro");
        System.out.println("  " + GREEN + "Neo: " + neoCoord + RESET
            + "  " + BLUE + "Tel: " + phoneCoord + RESET
            + "  " + RED + "Agentes:" + agentsCoords + RESET);
    }

    /**
     * Busca la primera ocurrencia de un caracter en el tablero
     * y devuelve sus coordenadas formateadas como "(fila,columna)".
     */
    private static String findEntity(char[][] grid, char target) {
        return IntStream.range(0, grid.length)
            .boxed()
            .flatMap(row -> IntStream.range(0, grid[0].length)
                .filter(col -> grid[row][col] == target)
                .limit(1)
                .mapToObj(col -> "(" + row + "," + col + ")"))
            .findFirst()
            .orElse("?");
    }

    /**
     * Busca todas las ocurrencias de un caracter en el tablero
     * y devuelve sus coordenadas separadas por espacio.
     */
    private static String findAllEntities(char[][] grid, char target) {
        return IntStream.range(0, grid.length)
            .boxed()
            .flatMap(row -> IntStream.range(0, grid[0].length)
                .filter(col -> grid[row][col] == target)
                .mapToObj(col -> "(" + row + "," + col + ")"))
            .collect(Collectors.joining(" "));
    }

    /**
     * Muestra el resultado final de la partida y pregunta si desea jugar otra vez.
     */
    private static void result(String resultStatus) {
        System.out.println(resultStatus.equals("NEO_WINS")
            ? BOLD + GREEN + "\n  NEO ESCAPO" + RESET
            : BOLD + RED + "\n  AGENTES GANARON" + RESET);
        System.out.print(YELLOW + "Otra? (s/n): " + RESET);
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) {
            main(new String[]{});
        } else {
            System.out.println(CYAN + "Gracias." + RESET);
        }
    }

    /** Pausa la ejecucion por una cantidad de milisegundos. */
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
