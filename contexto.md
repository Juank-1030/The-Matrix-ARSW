# Contexto del Proyecto: "Matrix - El Escape"

## Descripción General

Es un juego de simulación basado en el universo de The Matrix, implementado sobre un tablero bidimensional (matriz de N×M celdas de tamaño configurable). El juego modela una persecución dentro de la simulación: **Neo debe escapar hacia un teléfono antes de ser capturado por los Agentes**.

---

## Entidades del Tablero

| Símbolo | Entidad | Rol |
|---|---|---|
| `N` | Neo | Jugador / protagonista |
| `A` | Agente | Perseguidor autónomo (IA) |
| `T` | Teléfono | Portal de escape (objetivo) |
| `#` | Muro | Obstáculo infranqueable |

### Neo (`N`)
Es el único personaje controlado (ya sea por el jugador o por una IA defensiva). Su objetivo es alcanzar el teléfono más cercano sin ser capturado. Representa al hacker que intenta escapar de la simulación.

### Agentes (`A`)
Son entidades autónomas controladas por el sistema. Pueden existir **uno o varios simultáneamente** en el tablero. Cada Agente opera de forma independiente mediante su propio hilo de ejecución, y su único objetivo es interceptar a Neo. Son la amenaza principal del juego.

### Teléfonos (`T`)
Son los puntos de salida del mundo virtual. Pueden existir **uno o varios** en el tablero. Neo debe llegar a cualquiera de ellos para ganar. Si hay múltiples teléfonos, el sistema (o Neo) debe identificar el más accesible según la situación actual.

### Muros (`#`)
Son celdas bloqueadas que ninguna entidad puede atravesar. Definen la topografía del laberinto y condicionan las rutas posibles tanto para Neo como para los Agentes.

---

## Mecánica Central

El juego es esencialmente una **carrera con obstáculos**: Neo intenta llegar al teléfono más cercano mientras los Agentes calculan rutas para interceptarlo. La partida termina en dos condiciones:

- **Neo gana:** llega a una celda `T` antes de ser atrapado.
- **Los Agentes ganan:** uno de ellos ocupa la misma celda que Neo.

---

## Concurrencia y Manejo de Hilos

Este es el componente técnico más importante del proyecto. Cada Agente corre en su **propio hilo de ejecución independiente**, lo que significa que varios Agentes pueden calcular y mover simultáneamente. Esto introduce desafíos reales de programación concurrente:

- **Memoria compartida:** el tablero es un recurso compartido entre todos los hilos (Neo + Agentes). Se requieren mecanismos de sincronización (mutex, semáforos, locks) para evitar condiciones de carrera al leer/escribir posiciones.
- **Coordinación sin centralización:** los Agentes no se "comunican" entre sí explícitamente, pero comparten el estado del tablero, lo que genera una persecución distribuida naturalmente.
- **Actualización del estado:** cada vez que un Agente o Neo se mueve, el tablero debe actualizarse de forma segura para que todos los hilos vean el estado correcto.

---

## Algunos algoritmos de Búsqueda

Cada Agente usa un algoritmo de búsqueda de caminos para perseguir a Neo. Las opciones viables son:

- **BFS (Búsqueda en Anchura):** garantiza el camino más corto en grafos sin pesos. Simple y efectivo para tableros uniformes.
- **Dijkstra:** útil si en el futuro se añaden celdas con costos de movimiento distintos.
- **A\* (A-estrella):** el más eficiente para este escenario. Combina distancia recorrida con una heurística (por ejemplo, distancia Manhattan hacia Neo), lo que lo hace más inteligente y rápido que BFS en tableros grandes.


> Un detalle importante: como Neo se mueve, los Agentes deben **recalcular su ruta periódicamente**, no solo al inicio. Esto hace que el algoritmo deba ejecutarse de forma repetida y eficiente.

---

## Alguno patrones de Diseño Aplicables

Dado que el proyecto involucra concurrencia, entidades autónomas y un estado compartido, algunos patrones relevantes son:

- **Observer / Event-driven:** el tablero notifica a los hilos cuando el estado cambia (Neo se movió, un Agente llegó a su destino, etc.).
- **Strategy:** permite intercambiar el algoritmo de búsqueda de cada Agente en tiempo de ejecución (un Agente usa BFS, otro usa A\*, etc.).
- **Singleton:** para garantizar que el tablero sea una única instancia compartida.
- **Thread Pool:** en lugar de crear un hilo nuevo por Agente cada turno, se puede mantener un pool de hilos reutilizables.

---

## Configurabilidad del Sistema

El tablero y las reglas deben ser configurables sin recompilar el código:

- Tamaño del tablero (N × M)
- Número de Agentes
- Número de teléfonos
- Posición inicial de Neo, Agentes, teléfonos y muros
- Algoritmo de búsqueda a usar
- Velocidad de movimiento de cada entidad (relevante en concurrencia)