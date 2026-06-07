# Desarrollo — Matrix: El Escape

## Índice

1. [Introducción](#1-introducción)
2. [Diagrama de Scaffolding](#2-diagrama-de-scaffolding)
3. [Estructura del Proyecto](#3-estructura-del-proyecto)
4. [Clase 1: `Position.java`](#4-clase-1-positionjava)
5. [Clase 2: `Board.java`](#5-clase-2-boardjava)
6. [Clase 3: `PathFinder.java`](#6-clase-3-pathfinderjava)
7. [Clase 4: `GameEngine.java`](#7-clase-4-gameenginejava)
8. [Clase 5: `GameController.java`](#8-clase-5-gamecontrollerjava)
9. [Frontend: `index.html`](#9-frontend-indexhtml)
10. [Flujo Completo del Programa](#10-flujo-completo-del-programa)
11. [Algoritmos de Búsqueda Implementados](#11-algoritmos-de-búsqueda-implementados)
12. [Patrones de Diseño](#12-patrones-de-diseño)
13. [Conclusión](#13-conclusión)

---

## 1. Introducción

**Matrix: El Escape** es un juego de simulación por turnos implementado en Java con Spring Boot. El juego modela una persecución dentro de la matriz: **Neo debe escapar hacia un teléfono** antes de ser capturado por los **Agentes**.

El sistema tiene **dos modos de operación**:

| Modo | Neo | Agentes | Rol del usuario |
|------|-----|---------|-----------------|
| **Jugable** | Controlado por teclado (8 direcciones) | IA autónoma (BFS) | Juega activamente moviendo a Neo |
| **Simulación** | IA autónoma (A* hacia teléfono) | IA autónoma (BFS) | Observa el comportamiento de todas las entidades |

Cada turno sigue esta secuencia:
1. Neo se mueve **1 casilla** (input del usuario o IA)
2. Se verifica si Neo alcanzó un teléfono → **Neo gana**
3. Todos los Agentes se mueven **1 casilla en paralelo** (thread pool)
4. Se verifica si algún Agente alcanzó a Neo → **Agentes ganan**
5. Incrementa el contador de turno

### Movimiento en 8 direcciones

Ambos personajes (Neo y Agentes) pueden moverse en **8 direcciones**: 4 ortogonales (↑ ↓ ← →) y 4 diagonales (↖ ↗ ↙ ↘). Cada movimiento cuesta **1 turno**, ya sea ortogonal o diagonal. Esto hace que los caminos sean más cortos y naturales.

```
         ↖  ↑  ↗
         ←  N  →
         ↙  ↓  ↘

  Teclas modo Jugable:
  W/A/S/D = ortogonal  (↑ ← ↓ →)
  Q/E/Z/X = diagonal   (↖ ↗ ↙ ↘)
  Flechas = ortogonal
```

---

## 2. Diagrama de Scaffolding

```
┌────────────────────────────────────────────────────────────────────────────┐
│                                                                             │
│                        ┌──────────────────────┐                            │
│                        │    index.html         │                            │
│                        │  (UI + CSS + JS)      │                            │
│                        │  Renderiza tablero    │                            │
│                        │  Escucha teclado       │                            │
│                        │  Polling cada 300ms    │                            │
│                        └──────────┬───────────┘                            │
│                                   │ HTTP REST                              │
│                                   ▼                                         │
│                        ┌──────────────────────┐                            │
│                        │   GameController      │                            │
│                        │   @RestController     │                            │
│                        │   /api/game/*         │                            │
│                        └──────────┬───────────┘                            │
│                                   │                                         │
│                                   ▼                                         │
│                        ┌──────────────────────┐                            │
│                        │     GameEngine        │                            │
│                        │   Orquestador del     │                            │
│                        │   juego por turnos    │                            │
│                        │                       │                            │
│                        │  ┌─────────────────┐  │                            │
│                        │  │  Thread Pool     │  │  ← ExecutorService        │
│                        │  │  ┌─────┐┌─────┐  │  │    (agentes en paralelo) │
│                        │  │  │A* BFS││A* BFS│  │  │                            │
│                        │  │  │Agente││Agente│  │  │                            │
│                        │  │  └─────┘└─────┘  │  │                            │
│                        │  └─────────────────┘  │                            │
│                        └──────────┬───────────┘                            │
│                                   │                                         │
┌───────────────────────────────────┼───────────────────────────────────────┐
│              MODELO               │                                         │
│                                   ▼                                         │
│  ┌──────────────────────────────────────────────────────┐                  │
│  │                      Board                             │                  │
│  │                   (Singleton)                          │                  │
│  │              char[][] grid compartido                  │                  │
│  │          Métodos sincronizados (synchronized)          │                  │
│  └────────────────────────┬─────────────────────────────┘                  │
│                           │                                                │
│              ┌────────────┴────────────┐                                   │
│              ▼                         ▼                                   │
│  ┌──────────────────┐    ┌──────────────────────┐                          │
│  │    Position       │    │     PathFinder        │                          │
│  │    (record)       │    │     (interface)       │                          │
│  │  int row, col     │    │  ┌────────┐┌──────┐  │                          │
│  │  chebyshevDist()  │    │  │ AStar  ││ BFS  │  │                          │
│  │  getNeighbors()   │    │  └────────┘└──────┘  │                          │
│  │  (8 direcciones)  │    │  8 direcciones       │                          │
│  └──────────────────┘    └──────────────────────┘                          │
└────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Estructura del Proyecto

```
src/
└── main/
    ├── java/com/the/matrix/arsw/The_matrix_escape/
    │   ├── TheMatrixEscapeApplication.java   ← Entry point (sin cambios)
    │   ├── model/
    │   │   ├── Position.java
    │   │   └── Board.java
    │   ├── pathfinding/
    │   │   └── PathFinder.java
    │   ├── engine/
    │   │   └── GameEngine.java
    │   └── controller/
    │       └── GameController.java
    └── resources/
        ├── application.properties
        └── static/
            └── index.html                    ← Frontend completo
```

**Total: 5 clases Java + 1 HTML** ≈ 600 líneas de código.

---

## 4. Clase 1: `Position.java`

### Propósito

Representa una coordenada dentro del tablero. Es un `record` de Java, lo que significa que es inmutable y genera automáticamente `equals()`, `hashCode()`, `toString()`.

### Código conceptual

```java
package com.the.matrix.arsw.The_matrix_escape.model;

public record Position(int row, int col) {

    /**
     * Distancia Chebyshev entre esta posición y otra.
     * Fórmula: max(|row1 - row2|, |col1 - col2|)
     * Es la heurística correcta para movimiento en 8 direcciones
     * (ortogonal + diagonal con costo 1).
     */
    public int chebyshevDistance(Position other) {
        return Math.max(
            Math.abs(row - other.row),
            Math.abs(col - other.col)
        );
    }

    /**
     * Devuelve las 8 posiciones adyacentes (UP, DOWN, LEFT, RIGHT,
     * UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT).
     * Nota: no valida si están dentro del tablero — eso lo hace Board.
     */
    public List<Position> getNeighbors() {
        return List.of(
            new Position(row - 1, col - 1),  // UP_LEFT
            new Position(row - 1, col),      // UP
            new Position(row - 1, col + 1),  // UP_RIGHT
            new Position(row, col - 1),      // LEFT
            new Position(row, col + 1),      // RIGHT
            new Position(row + 1, col - 1),  // DOWN_LEFT
            new Position(row + 1, col),      // DOWN
            new Position(row + 1, col + 1)   // DOWN_RIGHT
        );
    }
}
```

### Métodos

| Método | Retorno | Descripción |
|--------|---------|-------------|
| `chebyshevDistance(Position)` | `int` | Distancia Chebyshev: `max(\|dr\|, \|dc\|)` — heurística para 8 direcciones |
| `getNeighbors()` | `List<Position>` | Las 8 celdas adyacentes (ortogonal + diagonal) |

---

## 5. Clase 2: `Board.java`

### Propósito

Representa el tablero de juego como una matriz bidimensional de caracteres. Es la **memoria compartida** entre todos los hilos (Neo + Agentes). Implementa el patrón **Singleton** para garantizar una única instancia.

### Matriz interna

```
char[][] grid

'N' = Neo
'A' = Agente
'T' = Teléfono
'#' = Muro
'.' = Vacío
```

### Código conceptual

```java
package com.the.matrix.arsw.The_matrix_escape.model;

public class Board {

    private static Board instance;
    private final int rows;
    private final int cols;
    private final char[][] grid;

    // Constructor privado (Singleton)
    private Board(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.grid = new char[rows][cols];
        for (int r = 0; r < rows; r++)
            Arrays.fill(grid[r], '.');  // Inicializar todo como vacío
    }

    // Obtener instancia única
    public static synchronized Board getInstance(int rows, int cols) {
        if (instance == null)
            instance = new Board(rows, cols);
        return instance;
    }

    // Reiniciar para nueva partida
    public static synchronized void reset(int rows, int cols) {
        instance = new Board(rows, cols);
    }

    // Leer una celda
    public synchronized char getCell(int row, int col) {
        return grid[row][col];
    }

    // Escribir una celda
    public synchronized void setCell(int row, int col, char value) {
        grid[row][col] = value;
    }

    // Validar si una posición está dentro del tablero y no es muro
    public synchronized boolean isValidPosition(Position pos) {
        return pos.row() >= 0 && pos.row() < rows &&
               pos.col() >= 0 && pos.col() < cols &&
               grid[pos.row()][pos.col()] != '#';
    }

    // Mover una entidad de una posición a otra
    public synchronized boolean moveEntity(Position from, Position to) {
        if (!isValidPosition(to)) return false;
        char entity = grid[from.row()][from.col()];
        grid[from.row()][from.col()] = '.';
        grid[to.row()][to.col()] = entity;
        return true;
    }

    // Obtener vecinos válidos (dentro del tablero y no muros)
    public synchronized List<Position> getNeighbors(Position pos) {
        return pos.getNeighbors().stream()
            .filter(this::isValidPosition)
            .collect(Collectors.toList());
    }
}
```

### Métodos principales

| Método | Visibilidad | Descripción |
|--------|-------------|-------------|
| `getInstance(rows, cols)` | `public static synchronized` | Singleton: devuelve o crea la única instancia |
| `reset(rows, cols)` | `public static synchronized` | Destruye la instancia actual y crea una nueva |
| `getCell(row, col)` | `public synchronized` | Devuelve el carácter en (row, col) |
| `setCell(row, col, value)` | `public synchronized` | Establece el carácter en (row, col) |
| `isValidPosition(pos)` | `public synchronized` | True si la posición está en rango y no es muro |
| `moveEntity(from, to)` | `public synchronized` | Mueve la entidad de `from` a `to` si es válido |
| `getNeighbors(pos)` | `public synchronized` | Lista de posiciones adyacentes transitables |
| `findNeoPosition()` | `public synchronized` | Escanea el grid y devuelve la posición de 'N' |
| `findPhonePositions()` | `public synchronized` | Escanea y devuelve todas las posiciones 'T' |
| `findAgentPositions()` | `public synchronized` | Escanea y devuelve todas las posiciones 'A' |
| `cloneGrid()` | `public synchronized` | Devuelve una copia del grid (para el DTO) |

### Sincronización

Todos los métodos que acceden al grid son `synchronized`. Esto garantiza que cuando un hilo está leyendo o escribiendo el tablero, ningún otro hilo puede interferir. Es el mecanismo principal para evitar **condiciones de carrera** (race conditions).

---

## 6. Clase 3: `PathFinder.java`

### Propósito

Implementa el patrón **Strategy** para los algoritmos de búsqueda de caminos. Define una interfaz común y dos implementaciones internas:

| Implementación | Lo usa | Algoritmo |
|----------------|--------|-----------|
| `AStar` | **Neo** (modo simulación) | A* con heurística Chebyshev |
| `BFS` | **Agentes** (todos) | Búsqueda en Anchura |

### Código conceptual

```java
package com.the.matrix.arsw.The_matrix_escape.pathfinding;

public interface PathFinder {

    /**
     * Encuentra el camino más corto desde 'start' hasta 'goal'
     * en el tablero dado, evitando obstáculos.
     * @return Lista de posiciones desde start hasta goal (inclusive),
     *         o lista vacía si no hay camino.
     */
    List<Position> findPath(Board board, Position start, Position goal);

    // ─────────────────────────────────────────────
    //  Implementación A* (para Neo)
    // ─────────────────────────────────────────────
    class AStar implements PathFinder {

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            // Cola de prioridad ordenada por f(n) = g(n) + h(n)
            // donde g(n) = distancia recorrida, h(n) = Chebyshev a la meta
            // Chebyshev es la heurística correcta para 8 direcciones:
            // max(|dr|, |dc|) con costo 1 por paso ortogonal o diagonal
            PriorityQueue<Node> openSet = new PriorityQueue<>(
                Comparator.comparingInt(n -> n.g + n.h)
            );
            Set<Position> closedSet = new HashSet<>();
            Map<Position, Position> cameFrom = new HashMap<>();
            Map<Position, Integer> gScore = new HashMap<>();

            openSet.add(new Node(start, 0, start.chebyshevDistance(goal)));
            gScore.put(start, 0);

            while (!openSet.isEmpty()) {
                Node current = openSet.poll();
                Position pos = current.pos;

                if (pos.equals(goal)) {
                    return reconstructPath(cameFrom, pos);
                }

                closedSet.add(pos);

                for (Position neighbor : board.getNeighbors(pos)) {
                    if (closedSet.contains(neighbor)) continue;

                    int tentativeG = gScore.get(pos) + 1;

                    if (!gScore.containsKey(neighbor) || tentativeG < gScore.get(neighbor)) {
                        cameFrom.put(neighbor, pos);
                        gScore.put(neighbor, tentativeG);
                        int f = tentativeG + neighbor.chebyshevDistance(goal);
                        openSet.add(new Node(neighbor, tentativeG, f));
                    }
                }
            }
            return Collections.emptyList(); // No hay camino
        }

        private record Node(Position pos, int g, int h) {}

        private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
            List<Position> path = new LinkedList<>();
            Position node = current;
            while (node != null) {
                path.add(0, node);
                node = cameFrom.get(node);
            }
            return path;
        }
    }

    // ─────────────────────────────────────────────
    //  Implementación BFS (para Agentes)
    // ─────────────────────────────────────────────
    class BFS implements PathFinder {

        @Override
        public List<Position> findPath(Board board, Position start, Position goal) {
            Queue<Position> queue = new LinkedList<>();
            Map<Position, Position> cameFrom = new HashMap<>();
            Set<Position> visited = new HashSet<>();

            queue.add(start);
            visited.add(start);
            cameFrom.put(start, null);

            while (!queue.isEmpty()) {
                Position current = queue.poll();

                if (current.equals(goal)) {
                    return reconstructPath(cameFrom, current);
                }

                for (Position neighbor : board.getNeighbors(current)) {
                    if (!visited.contains(neighbor)) {
                        visited.add(neighbor);
                        cameFrom.put(neighbor, current);
                        queue.add(neighbor);
                    }
                }
            }
            return Collections.emptyList(); // No hay camino
        }

        private List<Position> reconstructPath(Map<Position, Position> cameFrom, Position current) {
            List<Position> path = new LinkedList<>();
            Position node = current;
            while (node != null) {
                path.add(0, node);
                node = cameFrom.get(node);
            }
            return path;
        }
    }
}
```

### Comparativa A* vs BFS

| Característica | A* | BFS |
|----------------|-----|-----|
| Tipo | Búsqueda informada (heurística) | Búsqueda no informada |
| Heurística | Distancia **Chebyshev** — `max(\|dr\|,\|dc\|)` | Ninguna |
| Direcciones | **8** (ortogonal + diagonal) | **8** (ortogonal + diagonal) |
| Estructura | PriorityQueue (por f = g + h) | Queue (FIFO) |
| Exploración | Dirigida hacia la meta | Circular, en todas direcciones |
| Nodos explorados | ~15-30% del tablero | ~50-70% del tablero |
| Optimalidad | ✅ Sí (heurística admisible) | ✅ Sí |
| Velocidad | Más rápido en tableros grandes | Más lento en tableros grandes |
| Ideal para | Tableros medianos/grandes con obstáculos | Tableros pequeños o laberintos simples |

### ¿Por qué A* para Neo y BFS para Agentes?

- **Neo (A* con Chebyshev)**: Necesita llegar al teléfono lo más rápido posible. A* con distancia Chebyshev es la combinación óptima para movimiento en 8 direcciones — la heurística es admisible (nunca sobreestima) y dirigida. Neo puede moverse en diagonal para acortar caminos significativamente.
- **Agentes (BFS)**: BFS garantiza el camino más corto pero explora en todas direcciones (ahora 8), lo que lo hace más lento. Esto le da una **oportunidad justa a Neo** para escapar. Si los agentes usaran A*, serían casi imbatibles gracias a las diagonales.

---

## 7. Clase 4: `GameEngine.java`

### Propósito

Es el **orquestador central** del juego. Gestiona el ciclo de vida de la partida, coordina los turnos, maneja la concurrencia de los agentes y detecta las condiciones de victoria o derrota.

### Código conceptual

```java
package com.the.matrix.arsw.The_matrix_escape.engine;

public class GameEngine {

    // ─── Configuración ───
    public record GameConfig(
        int rows, int cols,
        int agentCount, int phoneCount, int wallCount,
        String mode   // "PLAYABLE" | "SIMULATION"
    ) {}

    // ─── Estado del juego ───
    public enum GameStatus { PLAYING, NEO_WINS, AGENTS_WIN }

    // ─── Dirección (8 direcciones: ortogonal + diagonal) ───
    public enum Direction {
        UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1),
        UP_LEFT(-1, -1), UP_RIGHT(-1, 1), DOWN_LEFT(1, -1), DOWN_RIGHT(1, 1);
        final int dr, dc;
        Direction(int dr, int dc) { this.dr = dr; this.dc = dc; }
    }

    // ─── DTO de estado para la UI ───
    public record GameState(char[][] board, String status, int turn) {}

    // ─── Campos ───
    private Board board;
    private GameConfig config;
    private volatile GameStatus status;
    private final ExecutorService threadPool = Executors.newCachedThreadPool();
    private final PathFinder.BFS bfs = new PathFinder.BFS();
    private final PathFinder.AStar astar = new PathFinder.AStar();
    private int turn = 0;
    private volatile boolean paused = false;

    // ─── Iniciar partida ───
    public synchronized GameState start(GameConfig cfg) {
        this.config = cfg;
        this.status = GameStatus.PLAYING;
        this.turn = 0;

        // Crear/reiniciar Board (Singleton)
        Board.reset(cfg.rows(), cfg.cols());
        board = Board.getInstance(cfg.rows(), cfg.cols());

        // Colocar entidades aleatoriamente
        placeEntities();

        // Si es simulación, lanzar el loop automático en un hilo aparte
        if (cfg.mode().equals("SIMULATION")) {
            new Thread(this::simulationLoop).start();
        }

        return getState();
    }

    // ─── Colocar entidades en posiciones aleatorias ───
    private void placeEntities() {
        Random rand = new Random();
        List<Position> occupied = new ArrayList<>();

        // 1. Colocar Neo
        Position neoPos = randomFreePosition(rand, occupied);
        board.setCell(neoPos.row(), neoPos.col(), 'N');
        occupied.add(neoPos);

        // 2. Colocar Teléfonos
        for (int i = 0; i < config.phoneCount(); i++) {
            Position phonePos = randomFreePosition(rand, occupied);
            board.setCell(phonePos.row(), phonePos.col(), 'T');
            occupied.add(phonePos);
        }

        // 3. Colocar Agentes
        for (int i = 0; i < config.agentCount(); i++) {
            Position agentPos = randomFreePosition(rand, occupied);
            board.setCell(agentPos.row(), agentPos.col(), 'A');
            occupied.add(agentPos);
        }

        // 4. Colocar Muros
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

    // ─── Loop de Simulación (corre en su propio hilo) ───
    private void simulationLoop() {
        while (status == GameStatus.PLAYING) {
            // Neo elige dirección automática con A* hacia el teléfono más cercano
            Position neoPos = board.findNeoPosition();
            List<Position> phones = board.findPhonePositions();

            // Encontrar el teléfono más cercano (Chebyshev para 8 direcciones)
            Position nearestPhone = phones.stream()
                .min(Comparator.comparingInt(p -> neoPos.chebyshevDistance(p)))
                .orElse(null);

            if (nearestPhone != null) {
                List<Position> path = astar.findPath(board, neoPos, nearestPhone);
                if (path.size() > 1) {
                    Direction dir = directionFromTo(neoPos, path.get(1));
                    if (dir != null) {
                        processTurn(dir);
                    }
                }
            }

            // Pequeña pausa para que la UI pueda seguir el ritmo
            try { Thread.sleep(500); } catch (InterruptedException e) { break; }
        }
    }

    // ─── Procesar un turno completo ───
    public synchronized GameState processTurn(Direction dir) {
        if (status != GameStatus.PLAYING) return getState();

        // 1. Mover a Neo
        Position neoPos = board.findNeoPosition();
        Position newNeoPos = new Position(
            neoPos.row() + dir.dr,
            neoPos.col() + dir.dc
        );

        // Validar movimiento
        if (!board.isValidPosition(newNeoPos)) return getState();

        board.setCell(neoPos.row(), neoPos.col(), '.');
        board.setCell(newNeoPos.row(), newNeoPos.col(), 'N');

        // 2. ¿Neo llegó a un teléfono?
        for (Position phone : board.findPhonePositions()) {
            if (newNeoPos.equals(phone)) {
                status = GameStatus.NEO_WINS;
                threadPool.shutdownNow();
                return getState();
            }
        }

        // 3. Agentes se mueven en PARALELO
        List<Position> agentPositions = board.findAgentPositions();
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Position agentPos : agentPositions) {
            futures.add(threadPool.submit(() -> {
                synchronized (board) {
                    // BFS hacia la posición actual de Neo
                    Position currentNeoPos = board.findNeoPosition();
                    List<Position> path = bfs.findPath(board, agentPos, currentNeoPos);

                    if (path.size() > 1) {
                        Position nextStep = path.get(1);
                        board.setCell(agentPos.row(), agentPos.col(), '.');
                        board.setCell(nextStep.row(), nextStep.col(), 'A');
                        return nextStep.equals(currentNeoPos); // ¿Atrapó a Neo?
                    }
                    return false;
                }
            }));
        }

        // 4. Verificar si algún agente alcanzó a Neo
        try {
            for (Future<Boolean> f : futures) {
                if (f.get()) {  // Espera a que cada agente termine
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

    // ─── Obtener estado actual ───
    public GameState getState() {
        return new GameState(
            board.cloneGrid(),
            status.name(),
            turn
        );
    }

    // ─── Calcular dirección entre dos posiciones ───
    private Direction directionFromTo(Position from, Position to) {
        int dr = to.row() - from.row();
        int dc = to.col() - from.col();
        for (Direction d : Direction.values()) {
            if (d.dr == dr && d.dc == dc) return d;
        }
        return null;
    }
}
```

### Flujo detallado de `processTurn`

```
processTurn("UP_RIGHT")   ← diagonal
│
├─ 1. Encontrar posición actual de Neo
│     board.findNeoPosition() → (2, 3)
│
├─ 2. Calcular nueva posición (diagonal)
│     (2 + (-1), 3 + 1) = (1, 4)
│
├─ 3. Validar movimiento
│     board.isValidPosition((1,4)) → true (no es muro, está en rango)
│
├─ 4. Mover a Neo en el Board
│     board[2][3] = '.'
│     board[1][4] = 'N'
│
├─ 5. ¿Neo ganó?
│     ¿(1,4) está en phones [(0,5), (4,2)]? → No
│
├─ 6. Lanzar agentes en PARALELO (8 direcciones)
│     ┌──────────────────────────────────────────────┐
│     │ Thread Pool (ExecutorService)                 │
│     │                                               │
│     │ Hilo-1: Agente en (0,1)                       │
│     │   synchronized(board) {                       │
│     │     path = BFS(board, (0,1), (1,4))           │
│     │     → [(0,1), (0,2), (0,3), (0,4), (1,4)]     │
│     │     mover A a (0,2)                           │
│     │     return (0,2).equals((1,4))? → false       │
│     │   }                                           │
│     │                                               │
│     │ Hilo-2: Agente en (4,5)                       │
│     │   synchronized(board) {                       │
│     │     path = BFS(board, (4,5), (1,4))           │
│     │     → [(4,5), (3,4), (2,3), (1,4)] ← diagonal│
│     │     mover A a (3,4)                           │
│     │     return (3,4).equals((1,4))? → false       │
│     │   }                                           │
│     └──────────────────────────────────────────────┘
│
├─ 7. Esperar a que TODOS terminen
│     future1.get() → false (Agente 1 no atrapó)
│     future2.get() → false (Agente 2 no atrapó)
│
├─ 8. turno++ (ahora es turno 5)
│
└─ 9. Devolver GameState { board, status: "PLAYING", turn: 5 }
```

> Nota: Con 8 direcciones, los caminos son hasta ~30% más cortos que con 4.
> Ejemplo: ir de (0,0) a (4,4) en 4 direcciones toma 8 pasos; en 8 direcciones toma solo 4 pasos (diagonales).

### Concurrencia aplicada

| Elemento | Cómo se implementa |
|----------|-------------------|
| **Thread Pool** | `ExecutorService.newCachedThreadPool()` — crea hilos según demanda y los reusa |
| **Agentes en paralelo** | Cada agente se ejecuta como `Future<Boolean>` en el pool. Todos se lanzan simultáneamente |
| **Sincronización** | `synchronized` en todos los métodos de Board que acceden al grid |
| **Barrera de sincronización** | `Future.get()` bloquea hasta que cada agente termine su movimiento |
| **Hilo de simulación** | `new Thread(this::simulationLoop)` — un hilo dedicado que ejecuta el loop automático de Neo |
| **Flag volatile** | `status` y `paused` son `volatile` para visibilidad entre hilos |

---

## 8. Clase 5: `GameController.java`

### Propósito

Expone la API REST para que el frontend se comunique con el motor del juego.

### Endpoints

| Método | Ruta | Body | Respuesta | Descripción |
|--------|------|------|-----------|-------------|
| `POST` | `/api/game/start` | `StartRequest` | `GameState` | Inicia una nueva partida con la configuración dada |
| `GET` | `/api/game/state` | — | `GameState` | Obtiene el estado actual del tablero |
| `POST` | `/api/game/move` | `MoveRequest` | `GameState` | Mueve a Neo en una dirección (solo modo JUGABLE) |
| `POST` | `/api/game/pause` | — | — | Pausa la partida |
| `POST` | `/api/game/resume` | — | — | Reanuda la partida |
| `POST` | `/api/game/reset` | — | — | Reinicia el juego |

### Código conceptual

```java
package com.the.matrix.arsw.The_matrix_escape.controller;

@RestController
@RequestMapping("/api/game")
public class GameController {

    private final GameEngine engine = new GameEngine();

    @PostMapping("/start")
    public GameState start(@RequestBody StartRequest req) {
        GameConfig config = new GameConfig(
            req.rows(), req.cols(), req.agents(),
            req.phones(), req.walls(), req.mode()
        );
        return engine.start(config);
    }

    @GetMapping("/state")
    public GameState state() {
        return engine.getState();
    }

    @PostMapping("/move")
    public GameState move(@RequestBody MoveRequest req) {
        Direction dir = Direction.valueOf(req.direction().toUpperCase());
        return engine.processTurn(dir);
    }

    @PostMapping("/pause")
    public void pause() { /* engine.pause() */ }

    @PostMapping("/resume")
    public void resume() { /* engine.resume() */ }

    @PostMapping("/reset")
    public void reset() { /* engine.stop() */ }

    // ─── DTOs internos ───

    record StartRequest(
        int rows, int cols, int agents,
        int phones, int walls, String mode
    ) {}

    record MoveRequest(String direction) {}

    record GameState(char[][] board, String status, int turn) {}
}
```

---

## 9. Frontend: `index.html`

### Propósito

Único archivo frontend que contiene HTML, CSS y JavaScript. Se sirve desde `src/main/resources/static/`.

### Estructura de la UI

```
┌──────────────────────────────────────────────────────────────┐
│  ⬛ MATRIX - EL ESCAPE                                       │
├───────────────────────────────┬──────────────────────────────┤
│                               │  ⚙ CONFIGURACIÓN            │
│     ┌──┬──┬──┬──┬──┐         │                              │
│     │N │  │# │  │  │         │  Filas:      [ 10 ]         │
│     ├──┼──┼──┼──┼──┤         │  Columnas:   [ 10 ]         │
│     │  │A │  │  │  │         │  Agentes:    [  3 ]         │
│     ├──┼──┼──┼──┼──┤         │  Teléfonos:  [  1 ]         │
│     │  │  │  │T │  │         │  Muros:      [ 12 ]         │
│     ├──┼──┼──┼──┼──┤         │                              │
│     │# │  │  │  │A │         │  Modo:                       │
│     ├──┼──┼──┼──┼──┤         │  ● Jugable  ○ Simulación    │
│     │  │  │  │  │  │         │                              │
│     └──┴──┴──┴──┴──┘         │  [▶ INICIAR PARTIDA]        │
│                               │                              │
│  📊 Estado: 🟢 PLAYING       │  Turno: 12                  │
│                               │                              │
│  🎮 Controles (8 direcciones):│                              │
│        ↖ ↑ ↗                  │                              │
│        ← N →                  │  [⏸ Pausar] [↺ Reiniciar]  │
│        ↙ ↓ ↘                  │                              │
│  Teclas: WASD ortogonal +     │                              │
│  Q/E/Z/X diagonal             │                              │
└───────────────────────────────┴──────────────────────────────┘
```

### Componentes JS

| Función | Descripción |
|---------|-------------|
| `startGame()` | Lee el formulario de configuración y hace `POST /api/game/start` |
| `renderBoard(grid)` | Toma la matriz `char[][]` y la renderiza como una cuadrícula de `<div>` |
| `pollState()` | `setInterval` cada 300ms: `GET /api/game/state` → actualiza la UI |
| `handleKeydown(e)` | Captura teclas y mapea a 8 direcciones: `W/A/S/D` (ortogonal) + `Q/E/Z/X` (diagonal) + flechas |
| `updateStatus(state)` | Actualiza el panel de estado (turno, resultado) |
| `showOverlay(message)` | Muestra "🏆 Neo Wins!" o "💀 Agents Win!" cuando termina la partida |

**Mapa de teclas → 8 direcciones:**

```
   Q    W    E
    ↖   ↑   ↗
  A ←   N   → D
    ↙   ↓   ↘
   Z    S    X

Flechas: ←↑↓→ = LEFT, UP, DOWN, RIGHT
WASD: igual que flechas (ortogonal)
Q = UP_LEFT    E = UP_RIGHT
Z = DOWN_LEFT  X = DOWN_RIGHT
```

### Esquema de colores (tema Matrix)

| Elemento | Color | Hex |
|----------|-------|-----|
| Fondo | Negro | `#000000` |
| Texto general | Verde Matrix | `#00ff41` |
| Neo (N) | Verde brillante con sombra | `#00ff41` + `text-shadow` |
| Agente (A) | Rojo intenso | `#ff0040` |
| Teléfono (T) | Azul eléctrico | `#0088ff` |
| Muro (#) | Gris oscuro | `#333333` |
| Celda vacía | Casi negro con borde | `#0a0a0a` + borde `#003300` |

---

## 10. Flujo Completo del Programa

### Diagrama de Secuencia (Modo Jugable)

```
USUARIO          index.html          GameController      GameEngine          Board
   │                  │                    │                  │                │
   │  Abre página     │                    │                  │                │
   │─────────────────>│                    │                  │                │
   │                  │                    │                  │                │
   │  Configura y     │                    │                  │                │
   │  click INICIAR   │                    │                  │                │
   │─────────────────>│                    │                  │                │
   │                  │  POST /api/game/   │                  │                │
   │                  │  start             │                  │                │
   │                  │───────────────────>│                  │                │
   │                  │                    │  start(config)   │                │
   │                  │                    │─────────────────>│                │
   │                  │                    │                  │  Board.reset() │
   │                  │                    │                  │───────────────>│
   │                  │                    │                  │  placeEntities │
   │                  │                    │                  │───────*        │
   │                  │                    │                  │  (random)      │
   │                  │                    │                  │<──────*        │
   │                  │                    │<─────────────────│                │
   │                  │<───────────────────│                  │                │
   │                  │                    │                  │                │
   │  Renderiza       │                    │                  │                │
   │  tablero         │                    │                  │                │
   │<─────────────────│                    │                  │                │
   │                  │                    │                  │                │
   │  Polling cada    │                    │                  │                │
   │  300ms           │                    │                  │                │
   │─────────────────>│                    │                  │                │
   │                  │  GET /api/game/    │                  │                │
   │                  │  state             │                  │                │
   │                  │───────────────────>│──────────────────>│                │
   │                  │<───────────────────│<──────────────────│                │
   │<─────────────────│                    │                  │                │
   │                  │                    │                  │                │
   │  Presiona →      │                    │                  │                │
   │─────────────────>│                    │                  │                │
   │                  │  POST /api/game/   │                  │                │
   │                  │  move {RIGHT}      │                  │                │
   │                  │───────────────────>│                  │                │
   │                  │                    │  processTurn()   │                │
   │                  │                    │─────────────────>│                │
   │                  │                    │                  │  Mover Neo     │
   │                  │                    │                  │──────────────>│
   │                  │                    │                  │  Check win     │
   │                  │                    │                  │  (NEO_WINS?)   │
   │                  │                    │                  │<──────────────│
   │                  │                    │                  │                │
   │                  │                    │                  │  Lanzar agentes│
   │                  │                    │                  │  en PARALELO   │
   │                  │                    │                  │  ┌────┐┌────┐  │
   │                  │                    │                  │  │ A1  ││ A2  │  │
   │                  │                    │                  │  │BFS→A││BFS→A│  │
   │                  │                    │                  │  └────┘└────┘  │
   │                  │                    │                  │  Future.get()  │
   │                  │                    │                  │  (esperar)     │
   │                  │                    │                  │                │
   │                  │                    │                  │  Check loss    │
   │                  │                    │                  │  (AGENTS_WIN?) │
   │                  │                    │                  │                │
   │                  │                    │                  │  turno++       │
   │                  │                    │<─────────────────│                │
   │                  │<───────────────────│                  │                │
   │                  │                    │                  │                │
   │  Renderiza       │                    │                  │                │
   │  nuevo estado    │                    │                  │                │
   │<─────────────────│                    │                  │                │
   │                  │                    │                  │                │
```

### Diagrama de Secuencia (Modo Simulación)

```
USUARIO          index.html          GameController      GameEngine          Board
   │                  │                    │                  │                │
   │  Click INICIAR   │                    │                  │                │
   │─────────────────>│                    │                  │                │
   │                  │  POST /api/game/   │                  │                │
   │                  │  start (SIMULATION)│                  │                │
   │                  │───────────────────>│                  │                │
   │                  │                    │  start()         │                │
   │                  │                    │─────────────────>│                │
   │                  │                    │                  │  Crea Board    │
   │                  │                    │                  │  placeEntities │
   │                  │                    │                  │                │
   │                  │                    │                  │  Lanza hilo    │
   │                  │                    │                  │  simulationLoop│
   │                  │                    │                  │  (nuevo Thread)│
   │                  │                    │<─────────────────│                │
   │                  │<───────────────────│                  │                │
   │  Renderiza       │                    │                  │                │
   │<─────────────────│                    │                  │                │
   │                  │                    │                  │                │
   │  (cada 500ms)    │                    │                  │  simulationLoop│
   │                  │                    │                  │       │        │
   │                  │                    │                  │  A* busca      │
   │                  │                    │                  │  teléfono      │
   │                  │                    │                  │  más cercano   │
   │                  │                    │                  │       │        │
   │                  │                    │                  │  processTurn() │
   │                  │                    │                  │  (mueve Neo +  │
   │                  │                    │                  │   Agentes)     │
   │                  │                    │                  │       │        │
   │                  │                    │                  │  Thread.sleep  │
   │                  │                    │                  │  (500ms)       │
   │                  │                    │                  │       │        │
   │                  │  GET /api/game/    │                  │  (repite)      │
   │                  │  state (polling)   │                  │       │        │
   │                  │───────────────────>│──────────────────>│                │
   │                  │<───────────────────│<──────────────────│                │
   │  Actualiza       │                    │                  │                │
   │  tablero         │                    │                  │                │
   │<─────────────────│                    │                  │                │
```

---

## 11. Algoritmos de Búsqueda Implementados

### A* (A-Star) — Usado por Neo

```
Propósito: Encontrar el camino más corto desde Neo hasta el teléfono más cercano.

Características:
- Búsqueda informada (usa heurística)
- Heurística: Distancia Chebyshev (max(|dr|, |dc|)) — para 8 direcciones
- Estructura: PriorityQueue ordenada por f(n) = g(n) + h(n)
  donde g(n) = costo real desde el inicio hasta n
        h(n) = Chebyshev desde n hasta la meta
- Direcciones: 8 (ortogonal + diagonal con costo 1)
- Optimalidad: Sí (heurística admisible, nunca sobreestima)

Ejemplo de ejecución en tablero 5x5 con 8 direcciones:

  Tablero inicial:               A* exploración (8 dir):
  . . . . .                      . . . . .
  . N . # .                      . N═══# .     ← diagonal directa
  . . . # .                      .   ║ # .
  . . T . .                      .   ║ T . 
  . . A . .                      .   ║ A .
                                  ═══╝
  
  Nodos explorados: ~7 de 25      (28% del tablero)
  Camino con 4 dir: N→(1,1)→(1,2)→(2,2)→(2,3)→(3,3)→T (5 pasos)
  Camino con 8 dir: N→(1,1)→(2,2)→(3,3)→T         (3 pasos) ← diagonal ahorra 2 pasos
```

### BFS (Breadth-First Search) — Usado por Agentes

```
Propósito: Encontrar el camino más corto desde un Agente hasta Neo.

Características:
- Búsqueda no informada (sin heurística)
- Explora en anchura (todas las 8 direcciones por igual)
- Estructura: Queue FIFO (Cola)
- Direcciones: 8 (ortogonal + diagonal con costo 1)
- Optimalidad: Sí (garantiza el camino más corto en grafos sin pesos)

Ejemplo de ejecución en tablero 5x5 con 8 direcciones:

  Tablero inicial:               BFS exploración (8 dir):
  . . . . .                      ●●●●●
  . N . # .                      ●N●#●     ← explora en círculo
  . . . # .                      ●●●#●       pero con diagonales
  . . T . .                      ●●T●●
  . . A . .                      ●●A●●

  Camino con 4 dir: A→(2,2)→(1,2)→(1,1)→(0,1)→N (4 pasos)
  Camino con 8 dir: A→(2,2)→(1,1)→N               (2 pasos) ← diagonal directa
```

### Comparación visual (8 direcciones)

```
Tablero 10×10 con obstáculos:

A* (Neo → Teléfono):               BFS (Agente → Neo):
  ════╗                              ●●●●●●●●●●
  ═⬛═║                              ●⬛●●●●●●●●
  ═⬛═║  🟩 = explorado               ●⬛●●●●●●●●
  N⬛══╝                             N⬛●●●●●●●
  ═════T                              ●●●●●●●●●T
  
  Con 8 direcciones vs 4 direcciones:
  - A* explora ~25% menos nodos
  - BFS encuentra diagonales, acortando rutas ~30%
  - Los caminos son más naturales (línea recta diagonal)
```

---

## 12. Patrones de Diseño

### 1. **Singleton** — `Board`

| Propósito | Garantizar que solo exista una instancia del tablero compartida por todos los hilos |
|-----------|-------------------------------------------------------------------------------------|
| Cómo | Constructor privado + `getInstance()` estático sincronizado |
| Por qué | El tablero es un recurso compartido que Neo y todos los Agentes deben ver igual |

### 2. **Strategy** — `PathFinder`

| Propósito | Permitir intercambiar algoritmos de búsqueda sin cambiar el código cliente |
|-----------|-----------------------------------------------------------------------------|
| Cómo | Interface `PathFinder` con implementaciones internas `AStar` y `BFS` |
| Por qué | Neo usa A* y los Agentes usan BFS. Con Strategy se pueden intercambiar fácilmente |

### 3. **Thread Pool** — `ExecutorService`

| Propósito | Reutilizar hilos para ejecutar los movimientos de los agentes en paralelo |
|-----------|---------------------------------------------------------------------------|
| Cómo | `Executors.newCachedThreadPool()` — crea y reusa hilos según demanda |
| Por qué | Cada turno se lanzan N agentes en paralelo. El pool evita crear/destruir hilos constantemente |

### 4. **Producer-Consumer** (implícito) — Player Input

| Propósito | El usuario produce direcciones (teclado) y el engine las consume (turnos) |
|-----------|---------------------------------------------------------------------------|
| Cómo | HTTP request → Controller → `processTurn(Direction)` |
| Por qué | Separa la entrada del usuario de la lógica del juego |

---

## 13. Conclusión

**Matrix: El Escape** es un juego por turnos que implementa conceptos fundamentales de programación concurrente y algoritmos de búsqueda en un entorno práctico y visual.

### Resumen técnico

| Aspecto | Implementación |
|---------|---------------|
| **Lenguaje** | Java 21 con Spring Boot |
| **Clases** | 5 (`Position`, `Board`, `PathFinder`, `GameEngine`, `GameController`) |
| **Frontend** | 1 archivo HTML con CSS y JS embebido |
| **Patrones** | Singleton, Strategy, Thread Pool |
| **Algoritmos** | A* (Neo) y BFS (Agentes) |
| **Direcciones** | **8** (ortogonal + diagonal) con distancia Chebyshev |
| **Concurrencia** | Agentes se mueven en paralelo cada turno, Board sincronizado |
| **Modos** | Jugable (teclado: WASD + QEZX) y Simulación (automático) |
| **Comunicación** | REST API con polling cada 300ms |

### Lo que aprendimos

1. **Concurrencia real**: Múltiples hilos accediendo a un recurso compartido (Board) con sincronización (`synchronized`) para evitar condiciones de carrera.

2. **Algoritmos de búsqueda**: A* vs BFS — cómo la heurística afecta drásticamente la cantidad de nodos explorados y el rendimiento.

3. **Patrones de diseño**: Cómo Singleton, Strategy y Thread Pool resuelven problemas concretos de arquitectura de software.

4. **ARQUITECTURA (no ARSW)**: El sistema sigue un modelo sencillo de **capas** (Frontend → Controller → Engine → Model) sin frameworks complejos, demostrando que se puede construir software funcional con pocas clases bien diseñadas.

### Estado final del proyecto

```
 5 Clases Java
 1 Archivo HTML
 ~600 Líneas de código
 2 Modos de juego
 2 Algoritmos de búsqueda (A* + BFS)
 8 Direcciones de movimiento (ortogonal + diagonal)
 Concurrencia con hilos reales
 UI interactiva con tema Matrix
```

---

*Documento de desarrollo — Proyecto Matrix: El Escape*
*Arquitectura de Software — ARSW*
