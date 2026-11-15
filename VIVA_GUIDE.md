# RescueDrone – Java Lab Viva Examination Preparation Guide
## Academic Context: Third-Semester B.Tech Computer Science & Engineering (Java Programming Lab)

This guide provides faculty-level questions, concise answers, and code citations to help you excel during your Java Lab Viva examination and project defense.

---

## 1. Project Overview & Architectural Defense

### Q1: What is the core objective of the RescueDrone project?
**Answer**:
RescueDrone is a disaster relief optimization and simulation system. It enables emergency administrators to register disasters and affected locations, receive prioritized humanitarian relief requests, allocate available drones based on physical payload and battery constraints, compute the shortest aerial transport routes using **Dijkstra's Algorithm**, simulate deliveries, and track logistics expenditures.

### Q2: Why did you implement the backend in pure Java SE without external frameworks like Spring Boot?
**Answer**:
1. **Academic Evaluation & Core Competency**: The lab syllabus focuses on mastering core Java fundamentals—Object-Oriented Design, Java Collections Framework, manual algorithm implementations, concurrency, and File I/O.
2. **Zero External Dependencies**: Using `com.sun.net.httpserver.HttpServer` eliminates dependency conflicts and classpath issues, ensuring $100\%$ turnkey portability on any standard Java 17+ or Java 26 JDK.
3. **Architecture Decoupling**: All business logic, graph data structures, PriorityQueue heaps, and routing algorithms reside exclusively in pure Java. React serves purely as an interface.

---

## 2. Java Object-Oriented Programming (OOP) Concepts

### Q3: How did you implement Encapsulation in your project?
**Answer**:
All entity fields in `backend/src/org/rescuedrone/model/` (e.g., [`Drone.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/Drone.java)) are declared `private`. Access and mutation are controlled exclusively via `public` getters and setters with strict defensive validation:
- **Location Coordinates**: In `Location.java`, `latitude` is clamped between $[-90.0, 90.0]$ and `longitude` between $[-180.0, 180.0]$.
- **Drone Battery**: In `Drone.java`, `batteryPercentage` is clamped to $[0, 100]$.
- **Resource Inventory**: In `Resource.java`, negative stock allocations are strictly prevented.

### Q4: How is Inheritance used in your project?
**Answer**:
1. **Abstract Base Entity**: [`BaseEntity.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/BaseEntity.java) defines common metadata (`id`, `createdAt`, `updatedAt`) and abstract contracts (`toCsvRecord()`, `validate()`). Seven concrete entity classes (`Disaster`, `Location`, `Drone`, `Resource`, `ReliefRequest`, `Route`, `Delivery`) extend `BaseEntity`.
2. **Generic Base Manager**: [`BaseManager<T extends BaseEntity>`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/BaseManager.java) provides shared CRUD mechanics, synchronization, and CSV persistence for all domain managers.

### Q5: Give an example of Polymorphism in your codebase.
**Answer**:
1. **Dynamic Method Overriding**: Every domain entity overrides `toCsvRecord()`. When `FileManager` serializes entities to disk, calling `entity.toCsvRecord()` polymorphically invokes the specific subclass's CSV generator.
2. **Comparable Interface**: `ReliefRequest` implements `Comparable<ReliefRequest>` by overriding `compareTo()`, allowing the standard Java `PriorityQueue` to order heterogeneous requests polymorphically based on domain rules.

### Q6: What is the difference between an Abstract Class and an Interface, and where did you use each?
**Answer**:
- **Abstract Class**: Can have state (instance variables) and concrete method implementations along with abstract methods.
  - *Usage*: `BaseEntity` (stores `id`, `createdAt`) and `BaseManager<T>` (implements collection management and file loading).
- **Interface**: Defines a pure functional or behavioral contract with no instance state.
  - *Usage*: `java.lang.Comparable<T>` implemented by `ReliefRequest` and `com.sun.net.httpserver.HttpHandler` implemented by all REST handlers.

---

## 3. Data Structures & Collections Framework

### Q7: Why did you use a `PriorityQueue` for relief requests instead of `Collections.sort()`?
**Answer**:
- A `PriorityQueue` maintains a **Binary Min/Max-Heap** data structure.
- **Insertion**: Takes $O(\log n)$ time.
- **Extraction (Poll)**: Takes $O(\log n)$ time.
- **Inspection (Peek)**: Takes $O(1)$ time.
- If we used `Collections.sort()`, every time a new emergency request arrived, we would have to re-sort the entire list taking $O(n \log n)$ time. The heap structure is significantly more efficient for dynamic streaming environments.

### Q8: What is your exact priority comparison logic in `ReliefRequest.compareTo()`?
**Answer**:
In [`ReliefRequest.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/ReliefRequest.java#L160-L185):
1. **Primary**: **Urgency Level** in descending order (`CRITICAL (4)` > `HIGH (3)` > `MEDIUM (2)` > `LOW (1)`).
2. **Secondary**: For equal urgency, **Affected Population Count** in descending order (areas with more endangered citizens get served first).
3. **Tertiary**: For identical urgency and population, **Request Timestamp** in ascending order (FIFO / longest waiting time).

### Q9: Why does `BaseManager<T>` maintain BOTH an `ArrayList` and a `HashMap`?
**Answer**:
In [`BaseManager.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/BaseManager.java):
- `HashMap<String, T> map`: Provides **$O(1)$ constant-time lookup** when fetching an entity by its unique ID (`getById(id)`).
- `ArrayList<T> list`: Preserves insertion order and allows fast sequential iteration when saving records to CSV or displaying tables in the UI.
- Together, they provide the best trade-off between lookup speed and iteration order.

### Q10: How is the transportation network modeled as a Graph?
**Answer**:
In [`Graph.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/ds/Graph.java), we use an **Adjacency List** represented as `Map<String, List<Edge>>`:
- **Vertices**: Represent disaster locations (e.g. `LOC-001`, `LOC-002`).
- **Edges**: Represent aerial/ground corridors connecting pairs of locations with positive weights representing simulated distances in kilometers.
- **Adjacency List vs Adjacency Matrix**: An adjacency list takes $O(V + E)$ space, which is optimal for sparse transportation networks. An adjacency matrix would consume $O(V^2)$ space, wasting memory on empty cells.

---

## 4. Algorithms Deep-Dive

### Q11: Explain your manual implementation of Dijkstra's Shortest Path Algorithm.
**Answer**:
Implemented in [`DijkstraOptimizer.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DijkstraOptimizer.java):
1. **Data Structures**:
   - `Map<String, Double> distances`: Tracks tentative shortest distance from source to each vertex. Initialized to $+\infty$.
   - `Map<String, String> predecessors`: Tracks the previous node along the optimal path for backtracking.
   - `PriorityQueue<PathNode> minHeap`: Min-heap prioritized by smallest tentative distance.
   - `Set<String> visited`: Tracks settled vertices to prevent redundant reprocessing.
2. **Algorithm Steps**:
   - Set $\text{distances}[\text{source}] = 0$ and insert `PathNode(source, 0.0)` into `minHeap`.
   - While `minHeap` is not empty:
     - Pop vertex $u$ with minimum distance.
     - If $u$ is the destination, break early.
     - For each outgoing edge $(u, v)$ with weight $w$:
       - Calculate $\text{tentative} = \text{dist}[u] + w$.
       - If $\text{tentative} < \text{distances}[v]$ (**Edge Relaxation**):
         - Update $\text{distances}[v] = \text{tentative}$.
         - Record $\text{predecessors}[v] = u$.
         - Push `PathNode(v, tentative)` into `minHeap`.
   - If destination distance is infinite, return `found = false` (unreachable).
   - Otherwise, trace backwards from destination to source using `predecessors` and reverse the sequence.
3. **Time Complexity**: $O((V + E) \log V)$ where $V$ is number of locations and $E$ is number of routes.

### Q12: Can Dijkstra's algorithm work with negative edge weights? Why or why not?
**Answer**:
No. Dijkstra relies on a greedy assumption: once a vertex is marked settled/visited, its shortest distance cannot be improved. A negative edge could later reduce the distance to an already-settled vertex, producing incorrect results. However, in physical transportation and drone flight networks, distance cannot be negative ($\text{distance} \ge 0$), making Dijkstra mathematically sound and optimal.

### Q13: How does your Drone Allocation Algorithm work?
**Answer**:
Implemented in [`DroneAllocator.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DroneAllocator.java):
1. **Constraint Evaluation**: Filters all fleet aircraft through 4 hard checks:
   - Status must be `AVAILABLE`.
   - Payload weight $\le$ Drone's `maxCapacityKg`.
   - Round-trip flight $(2 \times \text{distance}) \le$ Drone's `maxFlightRangeKm`.
   - Battery $\ge$ Required buffer $(\text{round-trip} \times 1.5\% + 10\% \text{ safety buffer})$.
2. **Greedy Least-Excess-Capacity (Best Fit)**:
   - Among eligible drones, selects the one that minimizes excess capacity:
     $$\text{Excess Capacity} = \text{drone.maxCapacityKg} - \text{payloadWeightKg}$$
   - *Rationale*: Prevents assigning a $25\text{ kg}$ heavy-lift drone to a $2\text{ kg}$ first-aid kit, conserving high-capacity drones for severe mass-casualty requests.
3. **Deterministic Tie-Breaking**: If excess capacity is identical, selects the drone with the higher battery percentage, then breaks ties alphanumerically by Drone ID.

### Q14: What is the delivery cost calculation formula?
**Answer**:
Implemented in [`CostCalculator.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/CostCalculator.java):
$$\text{Cost (₹)} = 100 \text{ (Base Charge)} + (\text{Distance in km} \times 20) + (50 \text{ if Urgency is CRITICAL or HIGH})$$

---

## 5. File Handling & Concurrency

### Q15: How did you ensure data persistence without a database?
**Answer**:
Implemented in [`FileManager.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/persistence/FileManager.java):
- Reads and writes RFC-4180 compliant CSV flat files located in `backend/data/*.csv`.
- Uses `BufferedReader` and `BufferedWriter` with `StandardCharsets.UTF_8` for fast, platform-independent streaming.
- Implements custom tokenizer supporting quoted fields with embedded commas (e.g., descriptions like `"Flash flood, road blocked"`).

### Q16: How do you prevent data corruption if the system crashes during file writes?
**Answer**:
Through **Atomic File Swapping**:
- Instead of directly writing to the destination CSV file, `FileManager` writes all records to a temporary file (`filename.tmp`).
- Once the write completes successfully, `Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)` atomically swaps the temporary file into place.
- If a crash occurs mid-write, the original `.csv` remains completely intact and uncorrupted.

### Q17: How is concurrency handled in the Java REST Server?
**Answer**:
- `HttpServer` uses a `ThreadPoolExecutor` with a fixed pool of 10 worker threads.
- All mutating operations across managers (e.g. `add`, `update`, `delete`, `allocate`, `completeDelivery`) are marked with the `synchronized` keyword or execute inside synchronized monitor blocks on the central facade [`ReliefSystem.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/ReliefSystem.java), preventing race conditions.

---

## 6. Key Code Locations to Show the Examiner

| Requirement / Topic | File & Location |
| :--- | :--- |
| **Manual Dijkstra Implementation** | [`DijkstraOptimizer.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DijkstraOptimizer.java#L103-L184) |
| **Graph Adjacency List** | [`Graph.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/ds/Graph.java#L15-L55) |
| **PriorityQueue Scheduling** | [`ReliefRequest.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/ReliefRequest.java#L160-L185) |
| **Least-Excess Drone Allocation** | [`DroneAllocator.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DroneAllocator.java#L112-L215) |
| **Atomic File Persistence** | [`FileManager.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/persistence/FileManager.java#L40-L80) |
| **Generic Collection Manager** | [`BaseManager.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/BaseManager.java#L18-L95) |
| **Delivery Mission Simulation** | [`DeliveryService.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/DeliveryService.java#L45-L160) |
| **Automated Master Test Suite** | [`Phase10IntegrationTest.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/test/Phase10IntegrationTest.java) |
