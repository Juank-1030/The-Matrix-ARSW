package com.the.matrix.arsw.The_matrix_escape;

import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.Direction;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameConfig;
import com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.GameState;

import java.util.*;
import java.util.stream.*;

import static com.the.matrix.arsw.The_matrix_escape.engine.GameEngine.*;

public class TheMatrixEscapeApplication {

    private static final GameEngine engine = new GameEngine();
    private static final Scanner scanner = new Scanner(System.in);

    private static final String R = "\u001B[0m", GREEN = "\u001B[92m", RED = "\u001B[91m";
    private static final String BLUE = "\u001B[94m", GRAY = "\u001B[90m", YELLOW = "\u001B[93m", CYAN = "\u001B[96m", BOLD = "\u001B[1m", CLS = "\033[H\033[2J";

    public static void main(String[] args) {
        welcome();
        GameConfig config = askConfig();
        engine.start(config);
        if (config.mode().equals("SIMULATION")) simLoop(); else playLoop();
    }

    private static void welcome() {
        System.out.print(CLS);
        System.out.println(BOLD + CYAN + """
  ╔══════════════════════════════════════════╗
  ║       MATRIX - EL ESCAPE                ║
  ║   Neo debe escapar al teléfono...       ║
  ║   ...antes de que los Agentes lo atrapen║
  ╚══════════════════════════════════════════╝""" + R);
        sleep(1500);
    }

    private static GameConfig askConfig() {
        System.out.print(CLS + BOLD + YELLOW + "⚙ CONFIGURACIÓN" + R + "\n");
        return new GameConfig(
            readInt("Filas (8): ", 8), readInt("Columnas (8): ", 8),
            readInt("Agentes (2): ", 2), readInt("Teléfonos (1): ", 1),
            readInt("Muros (10): ", 10), readMode()
        );
    }

    private static int readInt(String p, int def) {
        System.out.print(p);
        try { return Integer.parseInt(scanner.nextLine().trim()); } catch (Exception e) { return def; }
    }

    private static String readMode() {
        System.out.print(("\n1-" + GREEN + "Jugable" + R + "  2-" + CYAN + "Simulación" + R + "\nModo (1): "));
        return scanner.nextLine().trim().equals("2") ? "SIMULATION" : "PLAYABLE";
    }

    private static void playLoop() {
        System.out.println(BOLD + GREEN + "\n🎮 JUGABLE  " + R + "WASD ortogonal | Q/E/Z/X diagonal | K=salir | P=pausa" + YELLOW + "\nEnter..." + R);
        try { scanner.nextLine(); } catch (Exception e) {}

        while (true) {
            render();
            if (!engine.getState().status().equals("PLAYING")) return;

            String in = scanner.nextLine().trim().toLowerCase();
            if (in.equals("k")) { System.out.println(YELLOW + "Adiós." + R); return; }
            if (in.equals("p")) { System.out.println(YELLOW + "Pausa. Enter..." + R); scanner.nextLine(); continue; }

            Direction dir = switch (in) {
                case "w","8" -> Direction.UP; case "s","2" -> Direction.DOWN; case "a","4" -> Direction.LEFT; case "d","6" -> Direction.RIGHT;
                case "q","7" -> Direction.UP_LEFT; case "e","9" -> Direction.UP_RIGHT; case "z","1" -> Direction.DOWN_LEFT; case "x","3" -> Direction.DOWN_RIGHT;
                default -> null;
            };
            if (dir == null) { System.out.println(RED + "❌ W,A,S,D,Q,E,Z,X" + R); sleep(800); continue; }

            GameState r = engine.processTurn(dir);
            if (!r.status().equals("PLAYING")) { render(); result(r.status()); return; }
        }
    }

    private static void simLoop() {
        System.out.println(BOLD + CYAN + "\n🤖 SIMULACIÓN" + R + "  Neo=A* | Agentes=BFS" + YELLOW + "\nEnter..." + R);
        try { scanner.nextLine(); } catch (Exception e) {}

        while (true) {
            render();
            var state = engine.getState();
            if (!state.status().equals("PLAYING")) { result(state.status()); return; }

            System.out.print(CYAN + "[Enter=paso | A=auto | K=salir]: " + R);
            String in = scanner.nextLine().trim().toLowerCase();
            if (in.equals("k")) { System.out.println(YELLOW + "Adiós." + R); return; }
            if (in.equals("a")) { autoSim(); return; }

            Direction dir = engine.computeAutoDirection();
            if (dir == null) { System.out.println(RED + "Neo sin ruta." + R); return; }

            var r = engine.processTurn(dir);
            if (!r.status().equals("PLAYING")) { render(); result(r.status()); return; }
        }
    }

    private static void autoSim() {
        System.out.println(CYAN + "\n▶ Auto (K=detener)" + R);
        sleep(1000);
        while (true) {
            render();
            var state = engine.getState();
            if (!state.status().equals("PLAYING")) { result(state.status()); return; }

            Direction dir = engine.computeAutoDirection();
            if (dir == null) { System.out.println(RED + "Neo sin ruta." + R); return; }

            var r = engine.processTurn(dir);
            if (!r.status().equals("PLAYING")) { render(); result(r.status()); return; }

            try { if (System.in.available() > 0 && Character.toLowerCase((char)System.in.read()) == 'k') {
                System.out.println(YELLOW + "Detenido." + R); return; }
            } catch (java.io.IOException e) {}
            sleep(400);
        }
    }

    private static void render() {
        System.out.print(CLS);
        GameState state = engine.getState();
        char[][] grid = state.board();
        int rows = grid.length, cols = rows > 0 ? grid[0].length : 0;

        System.out.println(BOLD + CYAN + "  MATRIX" + R + "  " + (state.mode().equals("SIMULATION") ? "🤖" : "🎮")
            + "  Turno: " + state.turn() + "  " + (state.status().equals("PLAYING") ? GREEN : RED) + state.status() + R);

        System.out.print("     "); IntStream.range(0, cols).forEach(c -> System.out.print(GRAY + c % 10 + " " + R));
        System.out.println("\n");

        IntStream.range(0, rows).forEach(r -> {
            System.out.print(GRAY + r % 10 + "    " + R);
            IntStream.range(0, cols).forEach(c -> {
                char cell = grid[r][c];
                System.out.print(switch (cell) {
                    case 'N' -> GREEN + BOLD + "N "; case 'A' -> RED + BOLD + "A ";
                    case 'T' -> BLUE + BOLD + "T "; case '#' -> GRAY + "# "; default -> GRAY + ". ";
                } + R);
            });
            System.out.println();
        });

        var positions = IntStream.range(0, rows).boxed()
            .flatMap(r -> IntStream.range(0, cols).mapToObj(c -> new AbstractMap.SimpleEntry<>(grid[r][c], new int[]{r, c})))
            .collect(Collectors.groupingBy(Map.Entry::getKey,
                Collectors.mapping(e -> e.getValue(), Collectors.toList())));

        String neoPos = positions.getOrDefault('N', List.of()).stream().findFirst()
            .map(p -> "(" + p[0] + "," + p[1] + ")").orElse("?");
        String phonePos = positions.getOrDefault('T', List.of()).stream().findFirst()
            .map(p -> "(" + p[0] + "," + p[1] + ")").orElse("?");
        String agentStr = positions.getOrDefault('A', List.of()).stream()
            .map(p -> "(" + p[0] + "," + p[1] + ")").collect(Collectors.joining(" "));

        System.out.println("\n  " + GREEN + "N" + R + "=Neo  " + RED + "A" + R + "=Agente  " + BLUE + "T" + R + "=Teléfono  " + GRAY + "#" + R + "=Muro");
        System.out.println("  " + GREEN + "Neo: " + neoPos + R + "  " + BLUE + "Tel: " + phonePos + R + "  " + RED + "Agentes:" + agentStr + R);
    }

    private static void result(String s) {
        System.out.println(s.equals("NEO_WINS")
            ? BOLD + GREEN + "\n  🏆 NEO ESCAPÓ 🏆" + R
            : BOLD + RED + "\n  💀 AGENTES GANARON 💀" + R);
        System.out.print(YELLOW + "¿Otra? (s/n): " + R);
        if (scanner.nextLine().trim().equalsIgnoreCase("s")) main(new String[]{});
        else System.out.println(CYAN + "Gracias." + R);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
