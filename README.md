# The-Matrix-ARSW

**Author:** Juan Carlos Bohórquez Monroy

## Project Problem

"Matrix - El Escape" is a simulation game based on The Matrix universe, implemented on a two-dimensional board (N×M grid of configurable size). The model represents a chase within the simulation: **Neo must escape to a phone before being captured by the Agents**.

### Board Entities

| Symbol | Entity | Role |
|---|---|---|
| `N` | Neo | Player / protagonist |
| `A` | Agent | Autonomous pursuer (AI) |
| `T` | Phone | Escape portal (goal) |
| `#` | Wall | Impassable obstacle |

**Neo (`N`)** is the only controlled character (either by the player or by a defensive AI). His goal is to reach the nearest phone without being captured. He represents the hacker trying to escape the simulation.

**Agents (`A`)** are autonomous entities controlled by the system. There can be **one or several simultaneously** on the board. Each Agent operates independently through its own execution thread, and its sole objective is to intercept Neo. They are the main threat of the game.

**Phones (`T`)** are the exit points of the virtual world. There can be **one or several** on the board. Neo must reach any of them to win. If there are multiple phones, the system must identify the most accessible one based on the current situation.

**Walls (`#`)** are blocked cells that no entity can traverse. They define the maze topography and condition the possible routes for both Neo and Agents.

### Core Mechanics

The game is essentially an **obstacle race**: Neo tries to reach the nearest phone while Agents calculate paths to intercept him. The match ends under two conditions:

- **Neo wins:** reaches a `T` cell before being caught.
- **Agents win:** one of them occupies the same cell as Neo.

### Concurrency and Thread Management

Each Agent runs on its **own independent execution thread**, meaning multiple Agents can calculate and move simultaneously. This introduces real concurrent programming challenges:

- **Shared memory:** the board is a shared resource among all threads (Neo + Agents). Synchronization mechanisms (mutexes, semaphores, locks) are required to avoid race conditions when reading/writing positions.
- **Decentralized coordination:** Agents do not explicitly "communicate" with each other, but they share the board state, creating a naturally distributed pursuit.
- **State updates:** every time an Agent or Neo moves, the board must be safely updated so all threads see the correct state.

### System Configurability

The board and rules must be configurable without recompiling the code:

- Board size (N × M)
- Number of Agents
- Number of phones
- Initial positions of Neo, Agents, phones, and walls
- Movement speed of each entity (relevant in concurrency)
