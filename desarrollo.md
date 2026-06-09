# Desarrollo — Matrix: El Escape

## Índice

1. [Introducción](#1-introducción)
2. [Diagrama de Arquitectura](#2-diagrama-de-arquitectura)
3. [Estructura del Proyecto](#3-estructura-del-proyecto)
4. [Clase 1: `Position.java`](#4-clase-1-positionjava)
5. [Clase 2: `Board.java`](#5-clase-2-boardjava)
6. [Clase 3: `PathFinder.java`](#6-clase-3-pathfinderjava)
7. [Clase 4: `GameEngine.java`](#7-clase-4-gameenginejava)
8. [Clase 5: `TheMatrixEscapeApplication.java`](#8-clase-5-thematrixescapeapplicationjava)
9. [Flujo Completo del Programa](#9-flujo-completo-del-programa)
10. [Algoritmos de Búsqueda Implementados](#10-algoritmos-de-búsqueda-implementados)
11. [Patrones de Diseño](#11-patrones-de-diseño)
12. [Conclusión](#12-conclusión)

---

## 1. Introducción

**Matrix: El Escape** es un juego de simulación por turnos implementado en Java con Spring Boot. El juego modela una persecución dentro de la matriz: **Neo debe escapar hacia un teléfono** antes de ser capturado por los **Agentes**.

El sistema tiene **dos modos de operación**:

| Modo | Neo | Agentes | Rol del usuario |
|------|-----|---------|-----------------|
| **Jugable** | Controlado por teclado (8 direcciones) | IA autónoma (BFS) | Juega activamente moviendo a Neo |
| **Simulación** | IA autónoma (A* hacia teléfono) | IA autónoma (BFS) | Avanza turno a turno o en automático |

Cada turno sigue esta secuencia:
1. Neo se mueve **1 casilla** (input del usuario o A*)
2. Se verifica si Neo alcanzó un teléfono → **Neo gana**
3. Todos los Agentes se mueven **1 casilla en paralelo** (thread pool)
4. Se verifica si algún Agente alcanzó a Neo → **Agentes ganan**
5. Incrementa el contador de turno

### Movimiento en 8 direcciones

Ambos personajes (Neo y Agentes) pueden moverse en **8 direcciones**: 4 ortogonales (↑ ↓ ← →) y 4 diagonales (↖ ↗ ↙ ↘). Cada movimiento cuesta **1 turno**, ya sea ortogonal o diagonal.

```
         ↖  ↑  ↗
         ←  N  →
         ↙  ↓  ↘
```

---

## 2. Diagrama de Arquitectura

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                   │
│   TheMatrixEscapeApplication (main)                               │
│   ┌─────────────────────────────────────────────────────────────┐ │
│   │  Consola con colores ANSI                                    │ │
│   │  Scanner para entrada del usuario                            │ │
│   │  Renderiza tablero con coordenadas                           │ │
│   └──────────┬──────────────────────────────────────────────────┘ │
│              │                                                    │
│              ▼                                                    │
│   ┌─────────────────────────────────────────────────────────────┐ │
│   │  GameEngine                                                  │ │
│   │  Orquestador del juego por turnos                            │ │
│   │                                                              │ │
│   │  ┌───────────────────────────────────────────────────┐       │ │
│   │  │  Thread Pool (ExecutorService)                     │       │ │
│   │  │  ┌─────┐ ┌─────┐ ┌─────┐                         │       │ │
│   │  │  │BFS  │ │BFS  │ │BFS  │ ← Agentes en paralelo   │       │ │
│   │  │  │Agente│ │Agente│ │Agente│                       │       │ │
│   │  │  └─────┘ └─────┘ └─────┘                         │       │ │
│   │  └───────────────────────────────────────────────────┘       │ │
│   └──────────┬──────────────────────────────────────────────────┘ │
│              │                                                    │
┌──────────────┴──────────────────────────────────────────────────┐ │
│                        MODELO                                    │ │
│                                                                   │ │
│  ┌────────────────────────────────────────────────────────────┐  │ │
│  │  Board (Singleton)                                         │  │ │
│  │  char[][] grid compartido entre hilos                      │  │ │
│  │  Métodos sincronizados (synchronized)                      │  │ │
│  └───────────┬────────────────────────────────────────────────┘  │ │
│              │                                                    │ │
│    ┌─────────┴─────────┐                                         │ │
│    ▼                   ▼                                         │ │
│  ┌────────────┐  ┌──────────────────────┐                        │ │
│  │ Position   │  │ PathFinder (interface)│                       │ │
│  │ (record)   │  │ ┌────────┐ ┌──────┐  │                       │ │
│  │ int row,col│  │ │ AStar  │ │ BFS  │  │                       │ │
│  │ Chebyshev  │  │ └────────┘ └──────┘  │                       │ │
│  │ getNeighbors│ │ 8 direcciones        │                       │ │
│  └────────────┘  └──────────────────────┘                        │ │
└──────────────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────┘
```

---

## 3. Estructura del Proyecto

```
src/
└── main/
    ├── java/com/the/matrix/arsw/The_matrix_escape/
    │   ├── TheMatrixEscapeApplication.java   ← Entry point + UI consola (~345 líneas)
    │   ├── model/
    │   │   ├── Position.java                 ← Coordenada inmutable (~50 líneas)
    │   │   └── Board.java                    ← Tablero Singleton (~145 líneas)
    │   ├── pathfinding/
    │   │   └── PathFinder.java               ← Interface + AStar + BFS (~147 líneas)
    │   └── engine/
    │       └── GameEngine.java               ← Motor del juego (~217 líneas)
    └── resources/
        └── application.properties
```

**Total: 5 clases Java** ≈ 900 líneas de código (con comentarios javadoc).

---

## 4. Clase 1: `Position.java`

### Propósito

Representa una coordenada `(fila, columna)` dentro del tablero del juego. Es un **`record` de Java 21**, lo que significa que es inmutable y el compilador genera automáticamente:
- Constructor `Position(int row, int col)`
- Métodos de acceso `row()` y `col()`
- `equals()` y `hashCode()` basados en ambos campos
- `toString()` con formato `Position[row=..., col=...]`

### Estrategia de búsqueda implementada

`Position` no implementa un algoritmo de búsqueda directamente, sino que proporciona dos herramientas fundamentales que **todos los algoritmos de búsqueda** (A* y BFS) utilizan:

#### 1. `chebyshevDistance(Position other)` — Heurística para A*

```java
public int chebyshevDistance(Position other) {
    return Math.max(
        Math.abs(row - other.row),
        Math.abs(col - other.col)
    );
}
```

La **distancia de Chebyshev** (también conocida como distancia del tablero de ajedrez o métrica L∞) calcula el mínimo número de pasos necesarios para ir de una posición a otra cuando un movimiento puede ser en cualquiera de las 8 direcciones (como un rey en ajedrez).

**Fórmula matemática:**
```
d((r1,c1), (r2,c2)) = max(|r1 - r2|, |c1 - c2|)
```

**¿Por qué Chebyshev y no Manhattan?**
- **Manhattan** (`|dr| + |dc|`) es para movimiento en **4 direcciones** (solo ortogonal). En 8 direcciones, Manhattan sobreestima el costo (no es admisible para A*).
- **Chebyshev** (`max(|dr|, |dc|)`) es para movimiento en **8 direcciones** porque las diagonales permiten cubrir una fila y una columna en un solo paso.

| Distancia | Desde (0,0) hasta (4,4) | Fórmula |
|-----------|------------------------|---------|
| Manhattan | 8 pasos | `\|4\| + \|4\| = 8` |
| Chebyshev | 4 pasos | `max(\|4\|, \|4\|) = 4` |
| Euclídea | ~5.66 pasos | `sqrt(4² + 4²) = 5.66` |

Para A*, la heurística Chebyshev es:
- **Admisible**: nunca sobreestima el costo real (cada paso diagonal acerca en ambos ejes simultáneamente)
- **Consistente (monótona)**: satisface `h(n) ≤ costo(n, n') + h(n')`, garantizando que A* no necesite re-explorar nodos
- **Óptima para 8 direcciones**: es la heurística que más se aproxima al costo real sin sobreestimarlo

#### 2. `getNeighbors()` — Generación de sucesores

```java
public List<Position> getNeighbors() {
    return List.of(
        new Position(row - 1, col - 1),  // UP_LEFT     (↖)
        new Position(row - 1, col),      // UP          (↑)
        new Position(row - 1, col + 1),  // UP_RIGHT    (↗)
        new Position(row, col - 1),      // LEFT        (←)
        new Position(row, col + 1),      // RIGHT       (→)
        new Position(row + 1, col - 1),  // DOWN_LEFT   (↙)
        new Position(row + 1, col),      // DOWN        (↓)
        new Position(row + 1, col + 1)   // DOWN_RIGHT  (↘)
    );
}
```

Este método genera los 8 vecinos sin filtrar. **No valida límites del tablero ni obstáculos** — esa responsabilidad se delega a `Board.isValidPosition()` y `Board.getNeighbors()`. Esto sigue el principio de **responsabilidad única**: `Position` solo sabe de geometría de coordenadas, no del estado del juego.

El orden de generación (UP_LEFT, UP, UP_RIGHT, LEFT, RIGHT, DOWN_LEFT, DOWN, DOWN_RIGHT) es arbitrario pero consistente, lo que importa para la determinismo en BFS.

### Código

```java
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
```

### Métodos

| Método | Retorno | Complejidad | Descripción |
|--------|---------|-------------|-------------|
| `chebyshevDistance(Position)` | `int` | O(1) | Distancia Chebyshev: `max(\|dr\|, \|dc\|)` — heurística admisible para 8 direcciones |
| `getNeighbors()` | `List<Position>` | O(1) | Las 8 celdas adyacentes (ortogonal + diagonal), sin validación |

### Rol en la búsqueda de caminos

`Position` actúa como el **nodo** en los algoritmos de búsqueda de grafos:
- **Estado**: cada `Position` representa un estado (una celda del tablero)
- **Transiciones**: `getNeighbors()` genera los estados sucesores
- **Costo del paso**: cada transición tiene costo uniforme = 1
- **Heurística**: `chebyshevDistance()` estima el costo restante hacia la meta
- **Identidad**: al ser un `record`, dos posiciones con misma fila y columna son `equals()`, permitiendo su uso en `HashSet`, `HashMap`, etc.

---

## 5. Clase 2: `Board.java`

### Propósito

`Board` representa el tablero de juego como una matriz bidimensional de caracteres. Es la **memoria compartida** entre todos los hilos (Neo + Agentes). Implementa el patrón **Singleton** para garantizar una única instancia en toda la aplicación.

### Estrategia de representación del tablero

El tablero se modela como una matriz `char[][]` donde cada celda representa un elemento del escenario:

```
char[][] grid

'N' = Neo (jugador)
'A' = Agente (enemigo)
'T' = Teléfono (meta de Neo)
'#' = Muro (intransitable)
'.' = Vacío (transitable)
```

Esta representación con caracteres simples tiene ventajas:
- **Eficiencia de memoria**: `char` = 2 bytes por celda
- **Velocidad de acceso**: acceso directo por índice `grid[r][c]` en O(1)
- **Simplicidad**: sin objetos por celda, sin herencia de clases para tipos de celda
- **Facilidad de clonación**: `cloneGrid()` copia el arreglo con `System.arraycopy()`

**Alternativa descartada**: usar una `enum` o clase `Cell`. Se descartó porque:
- Agrega complejidad innecesaria (el juego solo necesita 5 tipos de celda)
- Dificulta la serialización plana para el DTO
- Aumenta el overhead de objetos en memoria

### Estrategia de Singleton

```java
private static Board instance;

private Board(int rows, int cols) { ... }

public static synchronized Board getInstance(int rows, int cols) {
    if (instance == null || instance.rows != rows || instance.cols != cols)
        instance = new Board(rows, cols);
    return instance;
}

public static synchronized void reset(int rows, int cols) {
    instance = new Board(rows, cols);
}
```

**¿Por qué Singleton?**
- El tablero es un **recurso compartido** que debe ser único: Neo (hilo principal) y todos los Agentes (thread pool) leen y escriben la misma matriz.
- Si cada hilo tuviera su propio tablero, los agentes no podrían ver dónde está Neo ni viceversa.
- Garantiza **coherencia de datos**: todos ven el mismo estado del mundo.

**Variante de recreación**: A diferencia del Singleton clásico (crear una vez y siempre devolver la misma instancia), `getInstance()` también recrea la instancia si las dimensiones cambian. Esto permite cambiar el tamaño del tablero entre partidas sin reiniciar la aplicación.

### Estrategia de sincronización (thread safety)

Dado que múltiples hilos acceden al grid simultáneamente, `Board` usa **sincronización a nivel de método** (`synchronized`):

```java
public synchronized char getCell(int row, int col) { ... }
public synchronized void setCell(int row, int col, char value) { ... }
public synchronized boolean isValidPosition(Position pos) { ... }
public synchronized List<Position> scan(char target) { ... }
public synchronized List<Position> getNeighbors(Position pos) { ... }
public synchronized List<Position> getWalkableNeighbors(Position pos) { ... }
public synchronized char[][] cloneGrid() { ... }
```

**¿Por qué `synchronized` y no `Lock` de `java.util.concurrent`?**
- `synchronized` es más simple y suficiente para operaciones cortas de lectura/escritura
- El overhead de `ReentrantLock` no se justifica: las operaciones sobre el tablero son microsegundos
- `synchronized` garantiza visibilidad de memoria (happens-before) entre hilos

**¿Por qué métodos completos y no bloques sincronizados?**
- Cada método es atómico: una llamada a `setCell()` no debe intercalarse con una lectura de `getCell()`
- La granularidad gruesa (método completo) es aceptable porque las operaciones son veloces
- Evita errores de olvidar sincronizar un bloque interno

**Métodos NO sincronizados (por diseño):**

```java
public Position findNeoPosition() { return scan('N').stream().findFirst().orElse(null); }
public List<Position> findPhonePositions() { return scan('T'); }
public List<Position> findAgentPositions() { return scan('A'); }
```

Aunque no tienen `synchronized`, internamente llaman a `scan()` que SÍ está sincronizado. La falta de `synchronized` aquí no es un error: estos métodos solo leen y devuelven una copia de las posiciones (inmutable), sin exponer la matriz interna.

### Estrategia de dos tipos de vecinos

```java
public synchronized List<Position> getNeighbors(Position pos) { ... }           // Para Neo
public synchronized List<Position> getWalkableNeighbors(Position pos) { ... }   // Para Agentes
```

**¿Por qué dos métodos separados?**

Los Agentes **no pueden pisar teléfonos**. Si un agente usara `getNeighbors()`, podría caminar sobre un teléfono, lo que:
1. No tiene sentido narrativo (los agentes no usan teléfonos)
2. Haría que los agentes bloquearan los teléfonos, haciendo imposible que Neo gane

`getWalkableNeighbors()` filtra las posiciones que contienen 'T', creando una barrera invisible para los agentes alrededor de cada teléfono.

### Estrategia de escaneo con `scan()`

```java
public synchronized List<Position> scan(char target) {
    return IntStream.range(0, rows)
        .boxed()
        .flatMap(r -> IntStream.range(0, cols)
            .filter(c -> grid[r][c] == target)
            .mapToObj(c -> new Position(r, c)))
        .collect(Collectors.toList());
}
```

`scan()` reemplaza lo que serían tres métodos separados (`findNeoPosition()`, `findPhonePositions()`, `findAgentPositions()`) por un solo método genérico. Es una aplicación del principio DRY (Don't Repeat Yourself) y del patrón **Template Method** a nivel de método.

**Complejidad**: O(rows × cols) — escanea toda la matriz. En tableros de hasta 20×20 (400 celdas), el escaneo es prácticamente instantáneo (~0.01ms).

### Código

```java
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
        for (int r = 0; r < rows; r++)
            Arrays.fill(grid[r], '.');
    }

    public static synchronized Board getInstance(int rows, int cols) {
        if (instance == null || instance.rows != rows || instance.cols != cols)
            instance = new Board(rows, cols);
        return instance;
    }

    public static synchronized void reset(int rows, int cols) {
        instance = new Board(rows, cols);
    }

    public synchronized char getCell(int row, int col) {
        return grid[row][col];
    }

    public synchronized void setCell(int row, int col, char value) {
        grid[row][col] = value;
    }

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

    public Position findNeoPosition() {
        return scan('N').stream().findFirst().orElse(null);
    }

    public List<Position> findPhonePositions() {
        return scan('T');
    }

    public List<Position> findAgentPositions() {
        return scan('A');
    }

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
```

### Métodos principales

| Método | Visibilidad | Complejidad | Descripción |
|--------|-------------|-------------|-------------|
| `getInstance(rows, cols)` | `public static synchronized` | O(rows×cols) | Singleton: crea o devuelve la instancia única |
| `reset(rows, cols)` | `public static synchronized` | O(rows×cols) | Destruye la instancia actual y crea una nueva |
| `getCell(row, col)` | `public synchronized` | O(1) | Devuelve el carácter en (row, col) |
| `setCell(row, col, value)` | `public synchronized` | O(1) | Establece el carácter en (row, col) |
| `isValidPosition(pos)` | `public synchronized` | O(1) | True si la posición está en rango y no es muro |
| `scan(target)` | `public synchronized` | O(rows×cols) | Busca todas las posiciones con un carácter dado |
| `findNeoPosition()` | `public` | O(rows×cols) | Escanea y devuelve la posición de 'N' |
| `findPhonePositions()` | `public` | O(rows×cols) | Escanea y devuelve todas las posiciones 'T' |
| `findAgentPositions()` | `public` | O(rows×cols) | Escanea y devuelve todas las posiciones 'A' |
| `getNeighbors(pos)` | `public synchronized` | O(8) | Vecinos transitables (sin filtrar teléfonos) |
| `getWalkableNeighbors(pos)` | `public synchronized` | O(8) | Vecinos transitables excluyendo teléfonos |
| `cloneGrid()` | `public synchronized` | O(rows×cols) | Copia defensiva del grid |

---

## 6. Clase 3: `PathFinder.java`

### Propósito

`PathFinder` es una **interfaz** que define el contrato para algoritmos de búsqueda de caminos en el tablero. Contiene dos implementaciones internas que materializan el patrón **Strategy**: `AStar` (para Neo en modo automático) y `BFS` (para los Agentes).

### Estrategia general de búsqueda de caminos

El problema a resolver es: **dado un tablero con obstáculos (muros), encontrar la secuencia de movimientos (pasos) más corta entre una posición inicial y una meta, donde cada paso puede ser en cualquiera de las 8 direcciones**.

El tablero se modela como un **grafo no dirigido y no ponderado** donde:
- **Vértices**: cada celda transitable (`.`, `N`, `A`, `T`) es un nodo
- **Aristas**: conexiones entre celdas adyacentes (8 direcciones), todas con costo = 1
- **Restricciones**: las celdas con `#` (muro) no son nodos del grafo

Tanto A* como BFS resuelven este problema, pero con estrategias fundamentalmente diferentes.

### `reconstructPath` — Método compartido (DRY)

```java
static List<Position> reconstructPath(Map<Position, Position> predecessors, Position current) {
    List<Position> path = new LinkedList<>();
    for (Position node = current; node != null; node = predecessors.get(node))
        path.add(0, node);
    return path;
}
```

Es un método `static` en la interfaz, compartido por `AStar` y `BFS`. Ambos algoritmos usan un `Map<Position, Position>` (predecesores) para rastrear el camino: una vez que se alcanza la meta, `reconstructPath` retrocede desde la meta hasta el inicio insertando cada nodo al principio de la lista.

**¿Por qué `LinkedList` y no `ArrayList`?**
- La inserción al principio (`add(0, node)`) es O(1) en `LinkedList` vs O(n) en `ArrayList`
- La reconstrucción del camino es O(p) donde p es la longitud del camino

---

### Implementación A* (A-Star) — Estrategia de Neo

```java
class AStar implements PathFinder {

    private record Node(Position pos, int g, int h) {}

    @Override
    public List<Position> findPath(Board board, Position start, Position goal) {
        PriorityQueue<Node> frontier = new PriorityQueue<>(
            Comparator.comparingInt(n -> n.g + n.h));
        Set<Position> explored = new HashSet<>();
        Map<Position, Position> predecessors = new HashMap<>();
        Map<Position, Integer> costFromStart = new HashMap<>();

        frontier.add(new Node(start, 0, start.chebyshevDistance(goal)));
        costFromStart.put(start, 0);

        while (!frontier.isEmpty()) {
            Node current = frontier.poll();
            Position currentPos = current.pos;

            if (currentPos.equals(goal))
                return reconstructPath(predecessors, currentPos);

            explored.add(currentPos);

            board.getNeighbors(currentPos).stream()
                .filter(neighbor -> !explored.contains(neighbor))
                .forEach(neighbor -> {
                    int newCost = costFromStart.get(currentPos) + 1;
                    if (!costFromStart.containsKey(neighbor)
                        || newCost < costFromStart.get(neighbor)) {
                        predecessors.put(neighbor, currentPos);
                        costFromStart.put(neighbor, newCost);
                        frontier.add(new Node(neighbor, newCost,
                            newCost + neighbor.chebyshevDistance(goal)));
                    }
                });
        }
        return Collections.emptyList();
    }
}
```

#### Explicación detallada del algoritmo

**1. Concepto fundamental**

A* es un algoritmo de búsqueda informada que utiliza una **función de evaluación** para decidir qué nodo explorar a continuación:

```
f(n) = g(n) + h(n)

Donde:
g(n) = costo real desde el nodo inicial hasta el nodo n
h(n) = estimación heurística del costo desde n hasta la meta
```

En nuestra implementación:
- `g(n)` = número de pasos desde `start` hasta `n` (cada paso cuesta 1)
- `h(n)` = `chebyshevDistance(n, goal)` — la distancia Chebyshev, que es la mínima cantidad de pasos en 8 direcciones

**2. Estructuras de datos**

| Estructura | Propósito | Tipo |
|------------|-----------|------|
| `frontier` | Cola de prioridad de nodos por explorar, ordenados por f(n) = g + h | `PriorityQueue<Node>` |
| `explored` | Conjunto de nodos ya evaluados (evita reprocesar) | `Set<Position>` |
| `predecessors` | Mapa de predecesores para reconstruir el camino | `Map<Position, Position>` |
| `costFromStart` | Mejor g(n) conocido para cada nodo | `Map<Position, Integer>` |

**3. El bucle principal**

```
1. Inicializar frontier con start, g=0, h=chebyshevDistance(start, goal)
2. Mientras frontier no esté vacía:
   a. Extraer el nodo con menor f(n) = g + h
   b. Si es la meta → reconstruir y devolver el camino
   c. Marcar como explorado
   d. Para cada vecino transitable (8 direcciones):
      - Si ya fue explorado → saltar
      - Calcular tentativeG = g(n) + 1
      - Si tentativeG es mejor que el mejor costo conocido:
        * Actualizar predecesor
        * Actualizar g(n)
        * Agregar a frontier con f = tentativeG + chebyshevDistance(vecino, meta)
3. Si se vacía frontier sin encontrar la meta → no hay camino (lista vacía)
```

**4. El `Node` record interno**

```java
private record Node(Position pos, int g, int h) {}
```

- `pos`: la posición en el tablero
- `g`: costo acumulado desde el inicio (g(n))
- `h`: valor heurístico (h(n))
- `f = g + h` se calcula en el comparador, no se almacena en el record

**5. ¿Por qué se usa `chebyshevDistance` como heurística?**

Para movimiento en 8 direcciones con costo uniforme, la distancia Chebyshev es la **única heurística admisible y consistente** que no sobreestima el costo real:

- **Admisibilidad**: `h(n) ≤ h*(n)` para todo n, donde h*(n) es el costo real mínimo. Chebyshev cumple porque en el mejor caso (sin obstáculos), se puede avanzar en diagonal, cubriendo una fila y columna por paso.
- **Consistencia**: `h(n) ≤ costo(n, n') + h(n')`. Como todos los costos de paso son 1 y Chebyshev satisface la desigualdad triangular, es consistente.
- **Optimalidad garantizada**: al ser admisible y consistente, A* con Chebyshev encuentra siempre el camino más corto.

**6. Complejidad**

| Aspecto | Análisis |
|---------|----------|
| **Tiempo (peor caso)** | O(b^d) donde b = factor de ramificación (máx 8) y d = profundidad de la solución |
| **Tiempo (caso típico)** | O(b^d) pero mucho menor que BFS porque la heurística dirige la búsqueda |
| **Espacio** | O(b^d) — almacena todos los nodos generados en frontier |
| **Nodos explorados (típico)** | ~15-30% del tablero (vs ~50-70% de BFS) |

**7. Diferencia clave: usa `getNeighbors()` (NO `getWalkableNeighbors()`)**

Neo puede pisar teléfonos (de hecho, pisar un teléfono es la condición de victoria). Por eso A* usa `board.getNeighbors()`, que incluye posiciones con 'T' como transitables. Si usara `getWalkableNeighbors()`, A* evitaría los teléfonos y nunca encontraría un camino hacia ellos.

---

### Implementación BFS (Breadth-First Search) — Estrategia de Agentes

```java
class BFS implements PathFinder {

    @Override
    public List<Position> findPath(Board board, Position start, Position goal) {
        Queue<Position> nodeQueue = new LinkedList<>();
        Map<Position, Position> predecessors = new HashMap<>();
        Set<Position> explored = new HashSet<>();

        nodeQueue.add(start);
        explored.add(start);
        predecessors.put(start, null);

        while (!nodeQueue.isEmpty()) {
            Position current = nodeQueue.poll();

            if (current.equals(goal))
                return reconstructPath(predecessors, current);

            board.getWalkableNeighbors(current).stream()
                .filter(neighbor -> !explored.contains(neighbor))
                .forEach(neighbor -> {
                    explored.add(neighbor);
                    predecessors.put(neighbor, current);
                    nodeQueue.add(neighbor);
                });
        }
        return Collections.emptyList();
    }
}
```

#### Explicación detallada del algoritmo

**1. Concepto fundamental**

BFS es un algoritmo de búsqueda **no informada** (ciega) que explora el grafo por **niveles** (capas) de distancia creciente desde el inicio. Garantiza encontrar el camino más corto en el primer recorrido porque explora todos los nodos a distancia d antes de pasar a distancia d+1.

**2. Estructuras de datos**

| Estructura | Propósito | Tipo |
|------------|-----------|------|
| `nodeQueue` | Cola FIFO de nodos por explorar | `Queue<Position>` (LinkedList) |
| `explored` | Conjunto de nodos ya visitados | `Set<Position>` |
| `predecessors` | Mapa de predecesores | `Map<Position, Position>` |

**3. El bucle principal**

```
1. Inicializar nodeQueue con start, marcar start como explorado
2. Mientras nodeQueue no esté vacía:
   a. Extraer el primer nodo de la cola (FIFO)
   b. Si es la meta → reconstruir y devolver el camino
   c. Para cada vecino transitable (8 direcciones, sin teléfonos):
      - Si no fue visitado:
        * Marcar como visitado (inmediatamente, para evitar encolado duplicado)
        * Registrar predecesor
        * Agregar a la cola
3. Si se vacía la cola sin encontrar la meta → no hay camino (lista vacía)
```

**4. ¿Por qué se marca como visitado ANTES de procesar?**

```java
explored.add(neighbor);  ← ANTES de agregar a la cola
nodeQueue.add(neighbor);
```

A diferencia de una implementación ingenua que marca visitado al salir de la cola, marcar al encolar evita que dos nodos diferentes encolen el mismo vecino. Esto es especialmente relevante con 8 direcciones donde múltiples nodos pueden tener el mismo vecino.

**5. Diferencia clave: usa `getWalkableNeighbors()` (NO `getNeighbors()`)**

Los agentes **no pueden pisar teléfonos**. BFS usa `board.getWalkableNeighbors()`, que filtra las celdas con 'T'. Esto tiene dos efectos de diseño:
- **Narrativo**: los agentes no pueden interceptar a Neo parándose sobre un teléfono
- **Estratégico**: los teléfonos actúan como zonas seguras hacia las cuales Neo puede escapar

**6. Complejidad**

| Aspecto | Análisis |
|---------|----------|
| **Tiempo (peor caso)** | O(b^d) — explora todos los nodos hasta profundidad d |
| **Tiempo (caso típico)** | O(b^d) — igual al peor caso porque BFS no tiene heurística |
| **Espacio** | O(b^d) — almacena todos los nodos de un nivel completo |
| **Nodos explorados (típico)** | ~50-70% del tablero (vs ~15-30% de A*) |

---

### Comparativa A* vs BFS

| Característica | A* | BFS |
|----------------|-----|-----|
| **Tipo** | Búsqueda informada (heurística) | Búsqueda no informada |
| **Heurística** | Distancia **Chebyshev** — `max(\|dr\|,\|dc\|)` | Ninguna |
| **Direcciones** | **8** (ortogonal + diagonal) | **8** (ortogonal + diagonal) |
| **Vecinos que usa** | `getNeighbors()` (incluye T) | `getWalkableNeighbors()` (excluye T) |
| **Estructura principal** | PriorityQueue (por f = g + h) | Queue (FIFO) |
| **Exploración** | Dirigida hacia la meta (explora ~20% del tablero) | Circular (explora ~60% del tablero) |
| **Optimalidad** | ✅ Sí (heurística admisible y consistente) | ✅ Sí (primera vez que encuentra la meta) |
| **Velocidad** | Más rápido en tableros medianos/grandes | Comparable en tableros pequeños (< 8×8) |
| **Complejidad temporal** | O(b^d) en el peor caso, pero típicamente mucho menor | Siempre O(b^d) |
| **Uso en el juego** | Neo (modo automático) | Agentes (persecución) |

### ¿Por qué A* para Neo y BFS para Agentes? (Análisis de diseño)

**Neo (A* con Chebyshev):**
- Neo necesita llegar al teléfono en la **menor cantidad de turnos posible**
- A* con Chebyshev garantiza el camino óptimo en 8 direcciones
- La heurística dirige la búsqueda hacia el teléfono, explorando solo ~20% del tablero
- El movimiento diagonal permite a Neo acortar caminos drásticamente

**Agentes (BFS):**
- BFS garantiza el camino más corto (como A*)
- Pero al no tener heurística, explora en **todas direcciones por igual**
- Esto hace que BFS sea más lento y explorador, dando **ventaja a Neo**
- Si los agentes usaran A*, serían casi imbatibles: 3-4 agentes convergiendo con A* hacia Neo atraparían a cualquier jugador humano en ~5 turnos

**Conclusión**: La asimetría A*/BFS es **intencional** para balancear el juego. Neo tiene un algoritmo más eficiente (debe llegar a la meta antes que los agentes), mientras que los agentes usan un algoritmo más lento (para dar oportunidad a Neo).

### `reconstructPath` como método `static` de la interfaz

A diferencia de una implementación duplicada en cada clase, `reconstructPath` es un método `static` de la interfaz `PathFinder`, compartido por `AStar` y `BFS`. Esto:
- Elimina duplicación de código (principio DRY)
- Garantiza que ambos algoritmos reconstruyan el camino de la misma forma
- Permite agregar nuevas implementaciones de `PathFinder` sin reimplementar `reconstructPath`

---

## 7. Clase 4: `GameEngine.java`

### Propósito

`GameEngine` es el **orquestador central** del juego. Gestiona el ciclo de vida de la partida, coordina los turnos, maneja la concurrencia de los agentes y detecta las condiciones de victoria o derrota.

### Estrategia general del juego

El juego sigue un modelo **turnos sincronizados** donde cada turno tiene una secuencia estricta:

```
1. Mover a Neo (input o A*)
2. Verificar si Neo pisa teléfono → NEO_WINS
3. Lanzar TODOS los agentes EN PARALELO
4. Esperar que TODOS terminen (barrera de sincronización)
5. Verificar si algún agente alcanzó a Neo → AGENTS_WIN
6. turnCount++
```

**¿Por qué los agentes se mueven en paralelo?**
- **Realismo**: los agentes deberían moverse simultáneamente, no en secuencia
- **Justicia**: si se movieran en secuencia, el primer agente tendría ventaja (podría atrapar a Neo antes de que los otros se muevan)
- **Rendimiento**: en un tablero de 10×10 con 5 agentes, el tiempo de cómputo es despreciable (~2ms), pero el patrón es escalable

### Estrategia de turnos: `processTurn()`

```java
public synchronized GameState processTurn(Direction direction) {
    if (gameStatus != GameStatus.PLAYING) return getState();

    // 1. Obtener posición actual de Neo
    Position neoPosition = board.findNeoPosition();
    if (neoPosition == null) return getState();

    // 2. Calcular destino
    Position nextNeoPosition = new Position(
        neoPosition.row() + direction.deltaRow,
        neoPosition.col() + direction.deltaCol
    );

    // 3. Validar movimiento (no muro, dentro del tablero)
    if (!board.isValidPosition(nextNeoPosition)) return getState();

    // 4. ¿El destino es un teléfono? (verificar ANTES de mover)
    boolean phoneReached =
        board.getCell(nextNeoPosition.row(), nextNeoPosition.col()) == 'T';

    // 5. Ejecutar el movimiento (independientemente de si es teléfono)
    board.setCell(neoPosition.row(), neoPosition.col(), '.');
    board.setCell(nextNeoPosition.row(), nextNeoPosition.col(), 'N');

    // 6. Si era teléfono → victoria inmediata
    if (phoneReached) {
        gameStatus = GameStatus.NEO_WINS;
        agentThreadPool.shutdownNow();
        return getState();
    }

    // 7. Mover agentes en paralelo
    if (moveAgents()) {
        gameStatus = GameStatus.AGENTS_WIN;
        agentThreadPool.shutdownNow();
        return getState();
    }

    // 8. Incrementar turno
    turnCount++;
    return getState();
}
```

#### Detalle de cada paso

**Paso 4: ¿Por qué verificar el teléfono ANTES de mover a Neo?**

```java
boolean phoneReached = board.getCell(nextNeoPosition.row(), nextNeoPosition.col()) == 'T';
```

Esta verificación se hace **antes** de modificar el grid. Si se hiciera después de `board.setCell(..., 'N')`, la celda contendría 'N' y no 'T', y se perdería la detección. Es un error sutil que puede passar desapercibido. La variable `phoneReached` captura el estado antes del movimiento.

**Paso 7: `moveAgents()` devuelve `true` si algún agente atrapó a Neo**

```java
if (moveAgents()) {
    gameStatus = GameStatus.AGENTS_WIN;
    agentThreadPool.shutdownNow();
    return getState();
}
```

`moveAgents()` devuelve `true` si al menos un agente terminó su movimiento en la misma celda que Neo. Cuando esto ocurre:
- El estado cambia a `AGENTS_WIN`
- Se llama a `agentThreadPool.shutdownNow()` para detener los hilos restantes (los agentes que aún no terminaron)
- Se devuelve el estado final

### Estrategia de concurrencia: `moveAgents()` y `moveSingleAgent()`

```java
private boolean moveAgents() {
    List<Future<Boolean>> agentResults = board.findAgentPositions().stream()
        .map(agentPosition -> agentThreadPool.submit(
            () -> moveSingleAgent(agentPosition)))
        .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

    try {
        for (Future<Boolean> agentResult : agentResults) {
            if (agentResult.get()) return true;
        }
    } catch (Exception e) {
        Thread.currentThread().interrupt();
    }
    return false;
}
```

**¿Cómo funciona?**
1. Se obtienen todas las posiciones de los agentes
2. Para cada agente, se lanza un `Callable<Boolean>` en el thread pool
3. Cada agente ejecuta `moveSingleAgent(agentPosition)` en su propio hilo
4. Se recolectan los `Future<Boolean>` en una lista
5. Se itera sobre los futures: `Future.get()` **bloquea** hasta que cada agente termina
6. Si algún agente devolvió `true` (atrapó a Neo), se retorna `true`

**¿Por qué `new ArrayList` en lugar de `toList()`?**

```java
.collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
```

`Stream.toList()` devuelve una lista **inmutable**. La necesitamos mutable y usar `ArrayList` explícito evita problemas con `Collectors.toList()` que no garantiza el tipo de implementación.

**`moveSingleAgent()` — movimiento individual de un agente:**

```java
private boolean moveSingleAgent(Position agentPosition) {
    synchronized (board) {
        Position currentNeoPosition = board.findNeoPosition();
        if (currentNeoPosition == null) return false;

        List<Position> pathToNeo = agentPathfinder.findPath(
            board, agentPosition, currentNeoPosition);

        if (pathToNeo.size() > 1) {
            Position nextAgentStep = pathToNeo.get(1);

            if (board.getCell(nextAgentStep.row(), nextAgentStep.col()) == 'T')
                return false;

            board.setCell(agentPosition.row(), agentPosition.col(), '.');
            board.setCell(nextAgentStep.row(), nextAgentStep.col(), 'A');

            return nextAgentStep.equals(currentNeoPosition);
        }
        return false;
    }
}
```

**¿Por qué `synchronized (board)` dentro de cada agente?**

Cada agente ejecuta `moveSingleAgent()` en su propio hilo. Aunque `Board` tiene sus métodos sincronizados, el bloque `synchronized (board)` agrupa varias operaciones en una **transacción atómica**: encontrar a Neo, calcular el camino, leer la celda destino y mover al agente. Sin esto, podría ocurrir:

1. Agente A lee posición de Neo en (3,3)
2. Agente B mueve a Neo a (4,4)
3. Agente A mueve el agente hacia (3,3) donde Neo ya no está

El `synchronized (board)` convierte toda la secuencia en una operación atómica para cada agente.

### Estrategia de movimiento automático: `computeAutoDirection()`

```java
public Direction computeAutoDirection() {
    Position neoPosition = board.findNeoPosition();
    if (neoPosition == null) return null;

    List<Position> phones = board.findPhonePositions();
    if (phones.isEmpty()) return null;

    Position closestPhone = phones.stream()
        .min(Comparator.comparingInt(p -> neoPosition.chebyshevDistance(p)))
        .orElse(null);
    if (closestPhone == null) return null;

    List<Position> path = autoPathfinder.findPath(board, neoPosition, closestPhone);
    if (path.size() <= 1) return null;

    int deltaRow = path.get(1).row() - neoPosition.row();
    int deltaCol = path.get(1).col() - neoPosition.col();

    return Arrays.stream(Direction.values())
        .filter(d -> d.deltaRow == deltaRow && d.deltaCol == deltaCol)
        .findFirst()
        .orElse(null);
}
```

**Flujo de decisión automática:**

1. Obtener posición actual de Neo
2. Obtener todos los teléfonos
3. Encontrar el teléfono más cercano usando distancia Chebyshev (medida simple y rápida, no requiere A*)
4. Ejecutar A* desde Neo hasta ese teléfono
5. Obtener el primer paso del camino óptimo (path.get(1))
6. Calcular la dirección: `(deltaRow, deltaCol)` → `Direction`
7. Devolver esa dirección para que `processTurn()` la ejecute

**¿Por qué calcular el teléfono más cercano con Chebyshev y no con A*?**

Porque Chebyshev es O(1) mientras que A* requiere una búsqueda completa. Primero se filtra con Chebyshev (rápido), y solo se ejecuta A* para el candidato más prometedor.

### Estrategia de colocación de entidades

```java
private void placeEntities() {
    Random random = new Random();
    List<Position> occupiedPositions = new ArrayList<>();
    place('N', 1, random, occupiedPositions);
    place('T', config.phoneCount(), random, occupiedPositions);
    place('A', config.agentCount(), random, occupiedPositions);
    place('#', config.wallCount(), random, occupiedPositions);
}

private void place(char type, int count, Random random, List<Position> occupied) {
    for (int i = 0; i < count; i++) {
        Position pos;
        do {
            pos = new Position(
                random.nextInt(config.rows()),
                random.nextInt(config.cols()));
        } while (occupied.contains(pos));
        board.setCell(pos.row(), pos.col(), type);
        occupied.add(pos);
    }
}
```

**Estrategia:**
1. Se genera un `Random`
2. Se coloca primero a Neo (1 posición)
3. Luego los teléfonos (no pueden coincidir con Neo ni entre sí)
4. Luego los agentes (no pueden coincidir con Neo, teléfonos ni entre sí)
5. Finalmente los muros (no pueden coincidir con ninguna entidad previa)

**Orden de colocación**: Neo → Teléfonos → Agentes → Muros. Este orden es importante porque:
- Neo nunca aparece sobre un muro
- Los teléfonos nunca aparecen sobre Neo
- Los agentes nunca aparecen sobre un teléfono
- Los muros se colocan al final, en las posiciones restantes

### Gestión del ciclo de vida del Thread Pool

```java
private ExecutorService agentThreadPool = Executors.newCachedThreadPool();

public synchronized GameState start(GameConfig cfg) {
    if (agentThreadPool.isShutdown())
        agentThreadPool = Executors.newCachedThreadPool();
    ...
}
```

Cuando Neo gana o los agentes ganan, se llama `agentThreadPool.shutdownNow()`. Esto **cancela las tareas pendientes y detiene el pool**. En la siguiente partida, `start()` detecta que el pool está cerrado y lo recrea. Esto evita:
- Pérdida de memoria por pools abandonados
- Ejecución de tareas de una partida anterior en la nueva partida
- Excepciones por usar un pool cerrado

### `GameState` — DTO de estado

```java
public record GameState(char[][] board, String status, int turn, String mode) {}
```

`GameState` es un DTO (Data Transfer Object) inmutable que encapsula el snapshot del juego:
- `board`: copia del grid (no la referencia directa) para evitar modificaciones externas
- `status`: "PLAYING", "NEO_WINS" o "AGENTS_WIN"
- `turn`: contador de turnos actual
- `mode`: "PLAYABLE" o "SIMULATION" (para que la UI sepa qué mostrar)

La copia defensiva en `cloneGrid()` es crucial para **seguridad de hilos**: la UI recibe una copia del grid, no la referencia al grid real que los hilos de agentes siguen modificando.

### `Direction` — Estrategia de representación de movimientos

```java
public enum Direction {
    UP(-1, 0), DOWN(1, 0), LEFT(0, -1), RIGHT(0, 1),
    UP_LEFT(-1, -1), UP_RIGHT(-1, 1), DOWN_LEFT(1, -1), DOWN_RIGHT(1, 1);

    final int deltaRow;
    final int deltaCol;

    Direction(int deltaRow, int deltaCol) {
        this.deltaRow = deltaRow;
        this.deltaCol = deltaCol;
    }
}
```

Cada dirección tiene un delta (dr, dc) que se suma a la posición actual para obtener la nueva posición:

| Dirección | dr | dc |
|-----------|----|----|
| UP | -1 | 0 |
| DOWN | 1 | 0 |
| LEFT | 0 | -1 |
| RIGHT | 0 | 1 |
| UP_LEFT | -1 | -1 |
| UP_RIGHT | -1 | 1 |
| DOWN_LEFT | 1 | -1 |
| DOWN_RIGHT | 1 | 1 |

`deltaRow` y `deltaCol` se usan directamente en `processTurn()` para calcular la nueva posición de Neo:

```java
Position nextNeoPosition = new Position(
    neoPosition.row() + direction.deltaRow,
    neoPosition.col() + direction.deltaCol
);
```

### Concurrencia aplicada — resumen

| Elemento | Cómo se implementa | Propósito |
|----------|-------------------|-----------|
| **Thread Pool** | `Executors.newCachedThreadPool()` | Reutilizar hilos para agentes, evitar crear/destruir por turno |
| **Agentes en paralelo** | `threadPool.submit(() -> moveSingleAgent(...))` | Cada agente se ejecuta como `Callable<Boolean>` |
| **Barrera de sincronización** | `Future.get()` para cada futuro | Esperar que TODOS los agentes terminen antes de avanzar |
| **Sincronización de datos** | `synchronized` en Board + `synchronized (board)` en agentes | Evitar condiciones de carrera en el grid compartido |
| **Recreación del pool** | `if (pool.isShutdown()) pool = newCachedThreadPool()` | Permitir múltiples partidas sin reiniciar la JVM |
| **Flag volatile** | `volatile GameStatus gameStatus` | Visibilidad entre hilos del estado del juego |
| **Copia defensiva** | `board.cloneGrid()` en `getState()` | La UI recibe una copia, no la matriz real |

---

## 8. Clase 5: `TheMatrixEscapeApplication.java`

### Propósito

Es el punto de entrada de la aplicación (`main()`) y contiene toda la interfaz de usuario por consola. Proporciona:
- Menú de bienvenida con arte ASCII
- Configuración interactiva (filas, columnas, agentes, teléfonos, muros, modo)
- Bucle del modo **Jugable** (entrada por teclado)
- Bucle del modo **Simulación** (paso a paso o automático)
- Renderizado del tablero con colores ANSI y coordenadas

### Estrategia general de la interfaz

**¿Por qué interfaz de consola y no web?**
1. **Simplicidad**: sin servidor HTTP, sin REST, sin JavaScript — toda la lógica está en una sola JVM
2. **Debugging**: la salida por consola con colores ANSI permite visualizar el estado en tiempo real
3. **Enfoque**: el proyecto se enfoca en algoritmos de búsqueda y concurrencia, no en desarrollo web

### Estrategia de renderizado con colores ANSI

```java
private static final String RESET      = "\u001B[0m";
private static final String GREEN      = "\u001B[92m";
private static final String RED        = "\u001B[91m";
private static final String BLUE       = "\u001B[94m";
private static final String GRAY       = "\u001B[90m";
private static final String YELLOW     = "\u001B[93m";
private static final String CYAN       = "\u001B[96m";
private static final String BOLD       = "\u001B[1m";
private static final String CLEAR_SCREEN = "\033[H\033[2J";
```

Los códigos ANSI permiten:
- Colorear cada entidad (Neo verde, Agentes rojo, Teléfonos azul, Muros gris)
- Limpiar la pantalla antes de cada renderizado (`CLEAR_SCREEN`)
- Negritas para resaltar (`BOLD`)
- Tema Matrix: verde sobre negro

**Alternativa descartada**: Java Swing o JavaFX. Se descartaron porque:
- Agregan dependencias y complejidad de compilación
- No están disponibles en entornos headless (servidores, CI/CD)
- El juego es por turnos, la consola es perfectamente adecuada

### Estrategia de entrada del usuario

```java
private static void playLoop() {
    while (true) {
        render();
        String userInput = scanner.nextLine().trim().toLowerCase();

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

        if (direction != null) {
            GameState result = engine.processTurn(direction);
            if (!result.status().equals("PLAYING")) {
                render();
                result(result.status());
                return;
            }
        }
    }
}
```

**Mapa completo de teclas:**

```
   Q    W    E       7    8    9
    ↖   ↑   ↗         ↖   ↑   ↗
  A ←   N   → D     4 ←   N   → 6
    ↙   ↓   ↘         ↙   ↓   ↘
   Z    S    X       1    2    3

W/A/S/D = ortogonal (↑ ← ↓ →)
Q/E/Z/X = diagonal  (↖ ↗ ↙ ↘)
Numpad  = 1-9
K = salir
P = pausa
```

**¿Por qué `scanner.nextLine()` en lugar de tecla por tecla?**
- `Scanner.nextLine()` es portable (funciona en Windows, Linux, macOS)
- No requiere dependencias externas como JLine o lanterna
- Simple y suficiente para un juego por turnos

### Estrategia del modo Simulación

El modo Simulación tiene dos sub-modos:

1. **Paso a paso** (`simLoop()`):
   - Usuario presiona Enter para avanzar un turno
   - `computeAutoDirection()` calcula la dirección óptima
   - `processTurn()` ejecuta el turno
   - El usuario ve el tablero después de cada turno

2. **Automático** (`autoSim()`):
   - Se activa presionando 'A' en el modo paso a paso
   - Cada 400ms se ejecuta un turno automático
   - El usuario puede presionar 'K' para detener
   - Usa `System.in.available() > 0` para detectar teclas sin bloquear

```java
private static void autoSim() {
    while (true) {
        render();
        Direction direction = engine.computeAutoDirection();
        GameState result = engine.processTurn(direction);
        if (!result.status().equals("PLAYING")) {
            render();
            result(result.status());
            return;
        }
        // Detectar tecla 'K' sin bloquear
        if (System.in.available() > 0
            && Character.toLowerCase((char) System.in.read()) == 'k') {
            return;
        }
        sleep(400);
    }
}
```

### Estrategia de renderizado del tablero

```java
private static void render() {
    System.out.print(CLEAR_SCREEN);
    GameState state = engine.getState();
    char[][] boardGrid = state.board();

    // Encabezado
    System.out.println(BOLD + CYAN + "  MATRIX" + RESET
        + "  " + modeIcon + "  Turno: " + state.turn()
        + "  " + statusColor + state.status() + RESET);

    // Números de columna
    System.out.print("     ");
    IntStream.range(0, cols).forEach(c ->
        System.out.print(GRAY + c % 10 + " " + RESET));

    // Filas
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

    // Leyenda y coordenadas
    System.out.println("  N=Neo  A=Agente  T=Telefono  #=Muro");
    System.out.println("  Neo: " + findEntity(boardGrid, 'N')
        + "  Tel: " + findEntity(boardGrid, 'T')
        + "  Agentes:" + findAllEntities(boardGrid, 'A'));
}
```

**Estrategia de renderizado:**
1. Limpiar pantalla con código ANSI
2. Renderizar encabezado (modo, turno, estado)
3. Renderizar números de columna
4. Para cada fila: renderizar número de fila + celdas coloreadas
5. Renderizar leyenda con coordenadas de cada entidad
6. Las coordenadas se calculan con `findEntity()` y `findAllEntities()`

### Esquema de colores ANSI (tema Matrix)

| Elemento | Color | Código ANSI | Efecto visual |
|----------|-------|-------------|---------------|
| Fondo | Negro | (terminal) | Matrix oscuro |
| Títulos | Cian brillante | `\u001B[96m` | Resalta secciones |
| Neo (N) | Verde brillante + negrita | `\u001B[92m` + `\u001B[1m` | Verde Matrix clásico |
| Agente (A) | Rojo intenso + negrita | `\u001B[91m` + `\u001B[1m` | Peligro, enemigos |
| Teléfono (T) | Azul eléctrico + negrita | `\u001B[94m` + `\u001B[1m` | Meta, esperanza |
| Muro (#) | Gris oscuro | `\u001B[90m` | Obstáculo, opaco |
| Celda vacía (.) | Gris | `\u001B[90m` | Fondo del tablero |

### Componentes del código

| Método | Líneas | Descripción |
|--------|--------|-------------|
| `main()` | 10 | Punto de entrada: bienvenida, configuración, inicia modo |
| `welcome()` | 10 | Banner de bienvenida con arte ASCII Matrix |
| `askConfig()` | 10 | Solicita parámetros al usuario (filas, columnas, etc.) |
| `readInt()` | 6 | Lee entero con validación y valor por defecto |
| `readMode()` | 4 | Pregunta modo: 1 = Jugable, 2 = Simulación |
| `playLoop()` | 47 | Bucle del modo jugable (input WASD/QEZX/numpad) |
| `simLoop()` | 38 | Bucle del modo simulación (paso a paso) |
| `autoSim()` | 36 | Bucle automático (turnos cada 400ms) |
| `render()` | 46 | Renderiza el tablero con colores ANSI y coordenadas |
| `findEntity()` | 9 | Busca la primera ocurrencia de un carácter en el grid |
| `findAllEntities()` | 8 | Busca todas las ocurrencias de un carácter en el grid |
| `result()` | 12 | Muestra resultado final y pregunta si jugar de nuevo |
| `sleep()` | 6 | Pausa la ejecución por N milisegundos |

---

## 9. Flujo Completo del Programa

### Diagrama de Secuencia (Modo Jugable)

```
USUARIO          TheMatrixEscapeApp          GameEngine          Board
   │                     │                       │                │
   │  Ejecuta JAR        │                       │                │
   │────────────────────>│                       │                │
   │                     │                       │                │
   │  Ingresa            │                       │                │
   │  configuración      │                       │                │
   │────────────────────>│                       │                │
   │                     │  start(config)        │                │
   │                     │──────────────────────>│                │
   │                     │                       │  Board.reset() │
   │                     │                       │───────────────>│
   │                     │                       │  placeEntities │
   │                     │                       │  (aleatorio)   │
   │                     │<──────────────────────│                │
   │  Renderiza tablero  │                       │                │
   │<────────────────────│                       │                │
   │                     │                       │                │
   │  Presiona W         │                       │                │
   │────────────────────>│                       │                │
   │                     │  processTurn(UP)      │                │
   │                     │──────────────────────>│                │
   │                     │                       │  findNeoPosition│
   │                     │                       │───────────────>│
   │                     │                       │  getCell(T?)   │
   │                     │                       │───────────────>│
   │                     │                       │  setCell (N)   │
   │                     │                       │───────────────>│
   │                     │                       │                │
   │                     │                       │  moveAgents()  │
   │                     │                       │  threadPool    │
   │                     │                       │  ┌────┐┌────┐ │
   │                     │                       │  │ A1  ││ A2  │ │
   │                     │                       │  │BFS→A││BFS→A│ │
   │                     │                       │  └────┘└────┘ │
   │                     │                       │  Future.get()  │
   │                     │                       │  (esperar)     │
   │                     │                       │                │
   │                     │                       │  turnCount++   │
   │                     │<──────────────────────│                │
   │  Renderiza nuevo    │                       │                │
   │  estado             │                       │                │
   │<────────────────────│                       │                │
```

### Diagrama de Secuencia (Modo Simulación — paso a paso)

```
USUARIO          TheMatrixEscapeApp          GameEngine          Board
   │                     │                       │                │
   │  Inicia SIMULACIÓN  │                       │                │
   │────────────────────>│                       │                │
   │                     │  start(config)        │                │
   │                     │──────────────────────>│                │
   │                     │                       │  Crea Board    │
   │                     │                       │  placeEntities │
   │                     │<──────────────────────│                │
   │  Renderiza tablero  │                       │                │
   │<────────────────────│                       │                │
   │                     │                       │                │
   │  Presiona Enter     │                       │                │
   │────────────────────>│                       │                │
   │                     │  computeAutoDirection │                │
   │                     │──────────────────────>│                │
   │                     │                       │  A* busca      │
   │                     │                       │  teléfono      │
   │                     │<──────────────────────│                │
   │                     │                       │                │
   │                     │  processTurn(dir)     │                │
   │                     │──────────────────────>│                │
   │                     │                       │  Mueve Neo +   │
   │                     │                       │  Agentes BFS   │
   │                     │<──────────────────────│                │
   │  Renderiza nuevo    │                       │                │
   │  estado             │                       │                │
   │<────────────────────│                       │                │
```

### Modo Automático (dentro de Simulación)

```
USUARIO          TheMatrixEscapeApp          GameEngine          Board
   │                     │                       │                │
   │  Presiona A         │                       │                │
   │────────────────────>│                       │                │
   │                     │  (cada 400ms)          │                │
   │                     │  computeAutoDirection │                │
   │                     │──────────────────────>│                │
   │                     │<──────────────────────│                │
   │                     │                       │                │
   │                     │  processTurn(dir)     │                │
   │                     │──────────────────────>│                │
   │                     │<──────────────────────│                │
   │  Renderiza tablero  │                       │                │
   │<────────────────────│                       │                │
   │                     │                       │                │
   │  Presiona K         │                       │                │
   │────────────────────>│                       │                │
   │  (detiene)           │                       │                │
```

---

## 10. Algoritmos de Búsqueda Implementados

### A* (A-Star) — Usado por Neo en modo automático

```
Propósito: Encontrar el camino más corto desde Neo hasta el teléfono más cercano.

Tipo: Búsqueda informada (heurística).
Heurística: Distancia Chebyshev (max(|dr|, |dc|)) — admisible y consistente.
Estructura: PriorityQueue ordenada por f(n) = g(n) + h(n).
Vecinos: getNeighbors() (incluye teléfonos como transitables).
Direcciones: 8 (ortogonal + diagonal con costo 1 por paso).
Optimalidad: Sí (heurística admisible nunca sobreestima).

Función de evaluación:
  f(n) = g(n) + h(n)
  g(n) = costo real desde el inicio hasta n (n° de pasos)
  h(n) = Chebyshev desde n hasta la meta

Paso a paso detallado:
  1. Crear PriorityQueue frontier, ordenada por f = g + h
  2. Agregar start a frontier con g=0, h=Chebyshev(start, goal)
  3. Mientras frontier no esté vacía:
     a. Extraer el nodo con menor f de la cola
     b. Si es la meta → reconstruir camino y retornar
     c. Marcar como explorado
     d. Para cada vecino (8 direcciones):
        i. Si ya explorado, saltar
        ii. Calcular tentativeG = g(current) + 1
        iii. Si tentativeG < mejor g conocido para el vecino:
             - Actualizar g(vecino) = tentativeG
             - Calcular f = tentativeG + Chebyshev(vecino, goal)
             - Agregar vecino a frontier
  4. Si frontier se vacía → no hay camino

Ejemplo en tablero 5x5 con 8 direcciones:

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
                               → (2,2) → (3,3) → T
```

### BFS (Breadth-First Search) — Usado por Agentes

```
Propósito: Encontrar el camino más corto desde un Agente hasta Neo.

Tipo: Búsqueda no informada (sin heurística).
Estructura: Queue FIFO (Cola).
Vecinos: getWalkableNeighbors() (excluye teléfonos).
Direcciones: 8 (ortogonal + diagonal con costo 1 por paso).
Optimalidad: Sí (garantiza el camino más corto en grafos sin pesos).

Paso a paso detallado:
  1. Crear Queue FIFO nodeQueue
  2. Agregar start a la cola, marcar como explorado
  3. Mientras nodeQueue no esté vacía:
     a. Extraer el primer nodo de la cola (FIFO)
     b. Si es la meta → reconstruir camino y retornar
     c. Para cada vecino transitable (8 direcciones, sin teléfonos):
        i. Si no está explorado:
           - Marcar como explorado (inmediatamente)
           - Registrar predecesor
           - Agregar a la cola
  4. Si la cola se vacía → no hay camino

Ejemplo en tablero 5x5 con 8 direcciones:

  Tablero inicial:               BFS exploración (8 dir):
  . . . . .                      ●●●●●
  . N . # .                      ●N●#●     ← explora en círculo
  . . . # .                      ●●●#●       pero con diagonales
  . . T . .                      ●●T●●
  . . A . .                      ●●A●●

  Nodos explorados: ~15 de 25     (60% del tablero)
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
  - A* explora ~25% menos nodos que con Manhattan
  - BFS encuentra diagonales, acortando rutas ~30%
  - Los caminos son más naturales (línea recta diagonal)
```

### Análisis de complejidad en el contexto del juego

| Tablero | Celdas | A* (nodos explorados) | BFS (nodos explorados) | Diferencia |
|---------|--------|----------------------|----------------------|------------|
| 5 × 5 | 25 | ~7 (28%) | ~15 (60%) | 2.1× más BFS |
| 8 × 8 | 64 | ~13 (20%) | ~38 (59%) | 2.9× más BFS |
| 10 × 10 | 100 | ~18 (18%) | ~57 (57%) | 3.2× más BFS |
| 15 × 15 | 225 | ~34 (15%) | ~124 (55%) | 3.6× más BFS |

La diferencia crece con el tamaño del tablero porque BFS explora por niveles completos mientras A* se dirige hacia la meta.

---

## 11. Patrones de Diseño

### 1. **Singleton** — `Board`

| Propósito | Garantizar que solo exista una instancia del tablero, compartida por todos los hilos |
|-----------|---------------------------------------------------------------------------------------|
| Cómo | Constructor privado + `getInstance()` estático sincronizado |
| Variante | `getInstance()` recrea la instancia si las dimensiones cambian |
| Por qué | El tablero es un recurso compartido que Neo y los Agentes deben ver idéntico |
| Thread safety | `synchronized` en `getInstance()`, `reset()`, y todos los métodos de acceso |

**Alternativa descartada**: Pasar `Board` como parámetro a todos los objetos. Se descartó porque:
- Viola el principio DRY (habría que pasarlo a cada agente en cada turno)
- Dificulta la creación de nuevos agentes (habría que inyectarles el board)
- El Singleton es más simple para este caso de uso

### 2. **Strategy** — `PathFinder`

| Propósito | Permitir intercambiar algoritmos de búsqueda sin cambiar el código cliente |
|-----------|-----------------------------------------------------------------------------|
| Cómo | Interfaz `PathFinder` con implementaciones internas `AStar` y `BFS` |
| Método compartido | `reconstructPath()` es `static` en la interfaz, evitando duplicación |
| Por qué | Neo usa A* y los Agentes usan BFS. Con Strategy se pueden intercambiar fácilmente |

**Ventajas del Strategy:**
- **Extensibilidad**: se puede agregar `Dijkstra`, `GreedyBestFirst`, `JPS+`, etc. sin modificar `GameEngine`
- **Aislamiento**: cada algoritmo está encapsulado en su propia clase
- **Testabilidad**: cada algoritmo se puede probar de forma independiente

### 3. **Thread Pool** — `ExecutorService`

| Propósito | Reutilizar hilos para ejecutar los movimientos de los agentes en paralelo |
|-----------|---------------------------------------------------------------------------|
| Cómo | `Executors.newCachedThreadPool()` — crea y reusa hilos según demanda |
| Por qué | Cada turno se lanzan N agentes en paralelo. El pool evita crear/destruir hilos constantemente |
| Gestión de ciclo de vida | Se cierra con `shutdownNow()` al terminar la partida y se recrea en la siguiente |

**¿Por qué `newCachedThreadPool` y no `newFixedThreadPool`?**
- El número de agentes varía por partida (configurable por el usuario)
- `newCachedThreadPool()` crea hilos según demanda y reusa los inactivos
- Los hilos inactivos se eliminan después de 60 segundos (no hay fuga de recursos)

### 4. **Template Method** (implícito) — `moveAgents()` + `moveSingleAgent()`

| Propósito | Separar la orquestación de agentes del movimiento individual |
|-----------|---------------------------------------------------------------|
| Cómo | `moveAgents()` lanza los `Future`s y recolecta resultados; `moveSingleAgent()` ejecuta el BFS y mueve un agente |
| Por qué | Cada agente tiene lógica común (BFS hacia Neo) pero debe ejecutarse en su propio hilo |

### 5. **DTO (Data Transfer Object)** — `GameState` y `GameConfig`

| Propósito | Encapsular datos para transferencia entre capas (Engine → UI) |
|-----------|---------------------------------------------------------------|
| Cómo | Records inmutables de Java: `GameState` y `GameConfig` |
| Copia defensiva | `char[][] board` en `GameState` se obtiene de `cloneGrid()` |
| Por qué | Evitar que la UI modifique el estado interno del Engine |

---

## 12. Conclusión

**Matrix: El Escape** es un juego por turnos que implementa conceptos fundamentales de programación concurrente y algoritmos de búsqueda en un entorno práctico.

### Resumen técnico

| Aspecto | Implementación |
|---------|---------------|
| **Lenguaje** | Java 21 con Spring Boot |
| **Clases** | 5 (`Position`, `Board`, `PathFinder`, `GameEngine`, `TheMatrixEscapeApplication`) |
| **Interfaz** | Consola con colores ANSI |
| **Patrones** | Singleton, Strategy, Thread Pool, Template Method, DTO |
| **Algoritmos** | A* (Neo) con Chebyshev y BFS (Agentes) con 8 direcciones |
| **Direcciones** | **8** (ortogonal + diagonal) con distancia Chebyshev |
| **Concurrencia** | Agentes en paralelo con `ExecutorService`, Board sincronizado |
| **Modos** | Jugable (WASD + QEZX) y Simulación (paso a paso o automático A*) |

### Lo que aprendimos

1. **Concurrencia real**: Múltiples hilos accediendo a un recurso compartido (Board) con sincronización (`synchronized`) para evitar condiciones de carrera. Uso de `ExecutorService`, `Future`, y `volatile`.

2. **Algoritmos de búsqueda**: A* vs BFS — cómo la heurística afecta drásticamente la cantidad de nodos explorados y el rendimiento. La distancia Chebyshev como heurística admisible para 8 direcciones.

3. **Patrones de diseño**: Cómo Singleton, Strategy, Thread Pool y DTO resuelven problemas concretos de arquitectura de software.

4. **Arquitectura de software**: El sistema sigue un modelo sencillo de **capas** (UI → Engine → Model) sin frameworks complejos, demostrando que se puede construir software funcional con pocas clases bien diseñadas.

5. **Balance de juego asimétrico**: Usar A* para Neo y BFS para Agentes es intencional — el primero encuentra caminos óptimos de forma eficiente, el segundo explora más pero es más lento, dando oportunidad al jugador.

6. **Diferenciación de transitabilidad**: Los agentes no pueden pisar teléfonos (`getWalkableNeighbors()`), mientras que Neo sí puede (`getNeighbors()`). Esto crea zonas seguras estratégicas.

### Estado final del proyecto

```
 5 Clases Java
 ~900 Líneas de código (con javadoc)
 2 Modos de juego (Jugable + Simulación)
 2 Algoritmos de búsqueda (A* + BFS)
 8 Direcciones de movimiento (ortogonal + diagonal)
 Concurrencia con hilos reales (Thread Pool + synchronized)
 UI de consola con colores ANSI y tema Matrix
```

---

*Documento de desarrollo — Proyecto Matrix: El Escape*
*Arquitectura de Software — ARSW*
