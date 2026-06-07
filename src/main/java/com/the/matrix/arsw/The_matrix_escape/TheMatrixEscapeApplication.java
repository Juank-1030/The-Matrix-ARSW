package com.the.matrix.arsw.The_matrix_escape;

import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.Direction;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameConfig;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameState;

import java.util.Scanner;

public class TheMatrixEscapeApplication {

    private static final GameEngine engine = new GameEngine();
    private static final Scanner scanner = new Scanner(System.in);

    private static final String R = "\u001B[0m";
    private static final String GREEN = "\u001B[92m";
    private static final String RED = "\u001B[91m";
    private static final String BLUE = "\u001B[94m";
    private static final String GRAY = "\u001B[90m";
    private static final String YELLOW = "\u001B[93m";
    private static final String CYAN = "\u001B[96m";
    private static final String BOLD = "\u001B[1m";
    private static final String CLEAR = "\033[H\033[2J";

    public static void main(String[] args) {
        welcomeScreen();

        GameConfig config = askConfiguration();
        engine.start(config);

        if (config.mode().equals("SIMULATION")) {
            runSimulation();
        } else {
            runPlayable();
        }
    }

    private static void welcomeScreen() {
        System.out.print(CLEAR);
        System.out.println(BOLD + CYAN);
        System.out.println("  ╔══════════════════════════════════════════╗");
        System.out.println("  ║       MATRIX - EL ESCAPE                ║");
        System.out.println("  ║   Neo debe escapar al teléfono...       ║");
        System.out.println("  ║   ...antes de que los Agentes lo atrapen║");
        System.out.println("  ╚══════════════════════════════════════════╝");
        System.out.println(R);
        sleep(1500);
    }

    private static GameConfig askConfiguration() {
        System.out.print(CLEAR);
        System.out.println(BOLD + YELLOW + "⚙ CONFIGURACIÓN DE PARTIDA" + R + "\n");

        int rows = readInt("Filas (defecto 8): ", 8);
        int cols = readInt("Columnas (defecto 8): ", 8);
        int agents = readInt("Cantidad de Agentes (defecto 2): ", 2);
        int phones = readInt("Cantidad de Teléfonos (defecto 1): ", 1);
        int walls = readInt("Cantidad de Muros (defecto 10): ", 10);
        String mode = readMode();

        return new GameConfig(rows, cols, agents, phones, walls, mode);
    }

    private static int readInt(String prompt, int defaultValue) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        if (line.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static String readMode() {
        System.out.println("\nModo de juego:");
        System.out.println("  1 - " + GREEN + "Jugable" + R + " (controlas a Neo con teclado)");
        System.out.println("  2 - " + CYAN + "Simulación" + R + " (Neo huye automáticamente con A*)");
        System.out.print("Elige (1/2, defecto 1): ");
        String line = scanner.nextLine().trim();
        return line.equals("2") ? "SIMULATION" : "PLAYABLE";
    }

    private static void runPlayable() {
        System.out.println();
        System.out.println(BOLD + GREEN + "🎮 MODO JUGABLE" + R);
        System.out.println("  Controles:  " + YELLOW + "W/A/S/D" + R + " ortogonal  |  " + YELLOW + "Q/E/Z/X" + R + " diagonal");
        System.out.println("  Salir: " + YELLOW + "K" + R + "  |  Pausar: " + YELLOW + "P" + R);
        System.out.println(YELLOW + "\nPresiona Enter para empezar..." + R);
        try { scanner.nextLine(); } catch (Exception e) { }

        while (true) {
            render();

            GameState state = engine.getState();
            if (!state.status().equals("PLAYING")) {
                return;
            }

            System.out.print("\n" + GREEN + "➤ Dirección: " + R);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("k")) {
                System.out.println(YELLOW + "\n¡Hasta luego, Neo!" + R);
                return;
            }
            if (input.equals("p")) {
                System.out.println(YELLOW + "\n⏸ Juego pausado. Presiona Enter para continuar..." + R);
                scanner.nextLine();
                continue;
            }

            Direction dir = parseDirection(input);
            if (dir == null) {
                System.out.println(RED + "❌ Dirección inválida. Usa: W,A,S,D,Q,E,Z,X" + R);
                sleep(1000);
                continue;
            }

            GameState result = engine.processTurn(dir);
            if (!result.status().equals("PLAYING")) {
                render();
                showResult(result.status());
                return;
            }
        }
    }

    private static void runSimulation() {
        System.out.println();
        System.out.println(BOLD + CYAN + "🤖 MODO SIMULACIÓN" + R);
        System.out.println("  Neo usa " + GREEN + "A*" + R + " para buscar el teléfono más cercano.");
        System.out.println("  Los Agentes usan " + RED + "BFS" + R + " para perseguir a Neo.");
        System.out.println("  Presiona " + YELLOW + "K" + R + " para salir, " + YELLOW + "Enter" + R + " para avanzar un turno\n");
        System.out.println(YELLOW + "Presiona Enter para iniciar..." + R);
        try { scanner.nextLine(); } catch (Exception e) { }

        while (true) {
            render();

            GameState state = engine.getState();
            if (!state.status().equals("PLAYING")) {
                showResult(state.status());
                return;
            }

            System.out.print("\n" + CYAN + "[Enter=avanzar  |  A=auto  |  K=salir]: " + R);
            String input = scanner.nextLine().trim().toLowerCase();

            if (input.equals("k")) {
                System.out.println(YELLOW + "\n¡Hasta luego, Neo!" + R);
                return;
            }

            if (input.equals("a")) {
                runAutoSimulation();
                return;
            }

            Direction dir = engine.computeAutoDirection();
            if (dir == null) {
                System.out.println(RED + "❌ Neo no encuentra camino al teléfono." + R);
                return;
            }

            GameState result = engine.processTurn(dir);
            if (!result.status().equals("PLAYING")) {
                render();
                showResult(result.status());
                return;
            }
        }
    }

    private static void runAutoSimulation() {
        int delay = 400;
        System.out.println(CYAN + "\n▶ Auto-simulación iniciada (cada " + delay + "ms)" + R);
        System.out.println("  Presiona " + YELLOW + "K" + R + " para detener\n");
        sleep(1000);

        while (true) {
            render();

            GameState state = engine.getState();
            if (!state.status().equals("PLAYING")) {
                showResult(state.status());
                return;
            }

            Direction dir = engine.computeAutoDirection();
            if (dir == null) {
                System.out.println(RED + "\n❌ Neo no encuentra camino al teléfono." + R);
                return;
            }

            GameState result = engine.processTurn(dir);
            if (!result.status().equals("PLAYING")) {
                render();
                showResult(result.status());
                return;
            }

            try {
                if (System.in.available() > 0) {
                    char c = (char) System.in.read();
                    if (c == 'k' || c == 'K') {
                        System.out.println(YELLOW + "\n⏹ Simulación detenida." + R);
                        return;
                    }
                }
            } catch (java.io.IOException e) {
                // ignore
            }

            sleep(delay);
        }
    }

    private static void render() {
        System.out.print(CLEAR);
        GameState state = engine.getState();
        char[][] grid = state.board();
        int rows = grid.length;
        int cols = rows > 0 ? grid[0].length : 0;

        System.out.println(BOLD + CYAN + "  MATRIX - EL ESCAPE" + R);
        String modeLabel = state.mode().equals("SIMULATION") ? "🤖 SIMULACIÓN" : "🎮 JUGABLE";
        String statusColor = state.status().equals("PLAYING") ? GREEN : RED;
        System.out.println("  " + modeLabel + "  |  Turno: " + state.turn()
            + "  |  Estado: " + statusColor + state.status() + R + "\n");

        System.out.print("     ");
        for (int c = 0; c < cols; c++) {
            System.out.print(GRAY + (c % 10) + " " + R);
        }
        System.out.println();
        System.out.println();

        for (int r = 0; r < rows; r++) {
            System.out.print(GRAY + (r % 10) + "    " + R);
            for (int c = 0; c < cols; c++) {
                char cell = grid[r][c];
                switch (cell) {
                    case 'N' -> System.out.print(GREEN + BOLD + "N " + R);
                    case 'A' -> System.out.print(RED + BOLD + "A " + R);
                    case 'T' -> System.out.print(BLUE + BOLD + "T " + R);
                    case '#' -> System.out.print(GRAY + "# " + R);
                    default  -> System.out.print(GRAY + ". " + R);
                }
            }
            System.out.println();
        }

        System.out.println("\n  " + GREEN + "N" + R + "=Neo  "
            + RED + "A" + R + "=Agente  "
            + BLUE + "T" + R + "=Teléfono  "
            + GRAY + "#" + R + "=Muro");

        Position neoPos = null;
        java.util.List<Position> agentPositions = null;
        java.util.List<Position> phonePositions = null;
        try {
            neoPos = findNeoPos(grid);
            agentPositions = findAllPositions(grid, 'A');
            phonePositions = findAllPositions(grid, 'T');
        } catch (Exception e) {}

        if (neoPos != null) {
            System.out.println("\n  " + GREEN + "Neo:     (" + neoPos.row() + "," + neoPos.col() + ")" + R);
        }
        if (phonePositions != null && !phonePositions.isEmpty()) {
            System.out.println("  " + BLUE + "Teléfono: (" + phonePositions.get(0).row() + "," + phonePositions.get(0).col() + ")" + R);
        }
        if (agentPositions != null && !agentPositions.isEmpty()) {
            StringBuilder sb = new StringBuilder("  " + RED + "Agentes:");
            for (var p : agentPositions) sb.append(" (").append(p.row()).append(",").append(p.col()).append(")");
            System.out.println(sb.toString() + R);
        }
    }

    private static Position findNeoPos(char[][] grid) {
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[r].length; c++)
                if (grid[r][c] == 'N') return new Position(r, c);
        return null;
    }

    private static java.util.List<Position> findAllPositions(char[][] grid, char target) {
        java.util.List<Position> list = new java.util.ArrayList<>();
        for (int r = 0; r < grid.length; r++)
            for (int c = 0; c < grid[r].length; c++)
                if (grid[r][c] == target) list.add(new Position(r, c));
        return list;
    }

    private static void showResult(String status) {
        System.out.println();
        if (status.equals("NEO_WINS")) {
            System.out.println(BOLD + GREEN + "  🏆  ¡NEO HA ESCAPADO!  🏆" + R);
            System.out.println(GREEN + "  Neo llegó al teléfono y despertó de la Matrix." + R);
        } else {
            System.out.println(BOLD + RED + "  💀  LOS AGENTES GANARON  💀" + R);
            System.out.println(RED + "  Neo fue capturado. La simulación continúa..." + R);
        }

        System.out.print("\n" + YELLOW + "¿Jugar de nuevo? (s/n): " + R);
        String input = scanner.nextLine().trim().toLowerCase();
        if (input.equals("s")) {
            main(new String[]{});
        } else {
            System.out.println(CYAN + "\n¡Gracias por jugar Matrix - El Escape!" + R);
            System.exit(0);
        }
    }

    private static Direction parseDirection(String input) {
        return switch (input) {
            case "w", "8" -> Direction.UP;
            case "s", "2" -> Direction.DOWN;
            case "a", "4" -> Direction.LEFT;
            case "d", "6" -> Direction.RIGHT;
            case "q", "7" -> Direction.UP_LEFT;
            case "e", "9" -> Direction.UP_RIGHT;
            case "z", "1" -> Direction.DOWN_LEFT;
            case "x", "3" -> Direction.DOWN_RIGHT;
            default -> null;
        };
    }

    private static void pressEnter() {
        System.out.print(YELLOW + "Presiona Enter para continuar..." + R);
        scanner.nextLine();
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private record Position(int row, int col) {}
}
