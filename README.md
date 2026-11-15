# RescueDrone – Smart Disaster Relief and Drone Route Optimizer

[![Java](https://img.shields.io/badge/Java-17%2B%20%7C%2026-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://www.oracle.com/java/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=for-the-badge&logo=react&logoColor=black)](https://react.dev/)
[![Vite](https://img.shields.io/badge/Vite-5-646CFF?style=for-the-badge&logo=vite&logoColor=white)](https://vitejs.dev/)
[![Tests](https://img.shields.io/badge/Tests-282%20Passing%20(100%25)-success?style=for-the-badge)](./run-tests.bat)

---

## 1. Project Overview

**RescueDrone** is a smart disaster management simulation and autonomous drone route optimization application designed for humanitarian emergency response operations. Developed as an academic laboratory project for **Third-Semester B.Tech Computer Science & Engineering (Java Programming Lab)**, it demonstrates rigorous, practical implementations of **Object-Oriented Programming (OOP)**, **Data Structures (Graphs, Priority Queues, HashMaps, ArrayLists)**, **Algorithms (Dijkstra's Shortest Path, Multi-Tier Heap Scheduling, Greedy Least-Excess Drone Allocation)**, **File Handling (RFC-4180 CSV Persistence with Atomic Writes)**, and a **Modern Menu-Driven React Web Dashboard**.

### Core Operational Workflow
$$\text{Disaster Event} \rightarrow \text{Affected Locations} \rightarrow \text{Relief Request} \xrightarrow{\text{PriorityQueue}} \text{Optimal Drone Allocation} \xrightarrow{\text{Dijkstra Path}} \text{Cost Computation} \rightarrow \text{Delivery Simulation} \rightarrow \text{Asset Updates} \rightarrow \text{Audited History}$$

---

## 2. Technology Stack & Architectural Principles

| Layer | Technologies & Principles | Purpose |
| :--- | :--- | :--- |
| **Backend** | Pure Java SE 17+ (Java 26 compatible)<br>`com.sun.net.httpserver.HttpServer`<br>`ThreadPoolExecutor` (10 worker threads) | Zero external JAR dependencies; eliminates framework bytecode incompatibilities and demonstrates core Java SE mastery. |
| **Data Structures** | `Graph` (Adjacency List `Map<String, List<Edge>>`)<br>`PriorityQueue` (Binary Min-Heap)<br>`HashMap` ($O(1)$ ID lookups)<br>`ArrayList` (Sequential indexing) | Graph modeling, shortest path relaxation, urgent triage ordering, and in-memory indexing. |
| **Algorithms** | **Manual Dijkstra's Algorithm** with min-heap relaxation<br>**Multi-tier PriorityQueue** comparator<br>**Least-Excess-Capacity (Best Fit)** drone allocation | Core route optimization, triage scheduling, and payload/battery constrained assignment. |
| **Persistence** | Java File I/O (`BufferedReader`, `BufferedWriter`)<br>Atomic swap writes (`.tmp` $\rightarrow$ `.csv`)<br>RFC-4180 double-quote escaping | Thread-safe, corruption-tolerant CSV flat-file persistence in `backend/data/*.csv`. |
| **Frontend** | React 18, Vite 5, Lucide React, HTML5, CSS3<br>Interactive SVG Graph Canvas & Charts | Visual dashboard, dynamic network graph visualizer, mission dispatch board, telemetry gauges. |
| **Communication** | RESTful HTTP/JSON APIs on port `8080` (Vite dev proxy on port `3000`) | Clean decoupling: All business logic and algorithms stay in pure Java; React serves exclusively as UI. |

---

## 3. The 11 Functional Modules

The application features a structured navigation sidebar with 11 functional modules:

1. **Dashboard**: Central command hub with 9 live counters, PriorityQueue dispatch order table (`Heap Rank #1`), fleet readiness snapshot with battery meters, recent operational activity feed, and quick action buttons.
2. **Disaster Management**: Register, view, filter, update, and manage disasters across types (`Flood`, `Earthquake`, `Cyclone`, `Landslide`, `Fire`, `Other`) and severity tiers (`Low`, `Medium`, `High`, `Critical`).
3. **Location Management**: Register relief camps, field hospitals, and drop zones as graph vertices with simulated latitude/longitude coordinates, affected populations, and disaster links.
4. **Relief Request Management**: Multi-urgency relief demands triage system backed by a Java `PriorityQueue` heap (`Critical` $\rightarrow$ `High` $\rightarrow$ `Medium` $\rightarrow$ `Low`, with affected population and timestamp tie-breakers).
5. **Drone Management**: Fleet telemetry station tracking aircraft types, maximum payload capacities ($5\text{--}25\text{ kg}$), battery charge meters, flight ranges, stationed locations, and recharge controls.
6. **Drone Assignment Engine**: Automated allocation engine that checks payload capacity, battery sufficiency ($3\%\text{/km}$ round-trip), and flight range, selecting the drone with **Least Excess Capacity** to conserve heavy aircraft.
7. **Route Management**: Disaster transportation network represented as a weighted undirected graph with locations as vertices and road/air corridors as weighted edges.
8. **Dijkstra Route Optimization**: Step-by-step shortest path solver with an interactive SVG network canvas, intermediate hop reconstruction, distance calculation, and unreachable node diagnosis.
9. **Delivery Management**: Active mission simulation board (`Pending` $\rightarrow$ `In Transit` $\rightarrow$ `Completed` / `Cancelled`) with live status toggles, battery drainage calculation, and destination relocation.
10. **Resource Inventory Management**: Humanitarian stockpiles tracking (`First Aid Kits`, `Drinking Water`, `Food Rations`, `Medicines`, `Blankets`) with atomic reservation and consumption safeguards.
11. **Reports, Analytics & Settings**: Executive reporting dashboard with interactive SVG Donut and Bar charts, JVM runtime telemetry, CSV file inspection, and one-click demo data re-seeding.

---

## 4. Java Object-Oriented Programming (OOP) Implementation

### A. Inheritance & Abstraction
- **[`BaseEntity.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/BaseEntity.java)**: Abstract base class providing common properties (`id`, `createdAt`, `updatedAt`) and abstract methods (`toCsvRecord()`, `validate()`).
- All domain entities (`Disaster`, `Location`, `Drone`, `Resource`, `ReliefRequest`, `Route`, `Delivery`) extend `BaseEntity`, inheriting validation contracts and timestamp tracking.
- **[`BaseManager<T extends BaseEntity>`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/service/BaseManager.java)**: Generic abstract manager maintaining dual-collection architecture (`ArrayList<T>` for ordered iteration and `HashMap<String, T>` for $O(1)$ lookups) with automated CSV file synchronization.

### B. Polymorphism
- **Method Overriding**: Every entity overrides `toCsvRecord()` to produce RFC-4180 compliant CSV strings and `validate()` to enforce invariant business constraints.
- **Comparable Interface**: `ReliefRequest` implements `Comparable<ReliefRequest>`, overriding `compareTo()` to govern its ordering within the Java `PriorityQueue`.

### C. Encapsulation
- All class fields are declared `private`. Access and mutations occur strictly through getters and setters containing defensive boundary validations (e.g., latitude restricted to $[-90.0, 90.0]$, battery percentage clamped to $[0, 100]$, and negative stock allocation prevention).

---

## 5. Core Data Structures & Algorithms Deep-Dive

### 1. Manual Dijkstra Shortest Path Algorithm
- **Class**: [`DijkstraOptimizer.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DijkstraOptimizer.java)
- **Time Complexity**: $O((V + E) \log V)$ using a binary min-heap (`PriorityQueue<PathNode>`).
- **Graph Representation**: Weighted adjacency list [`Graph.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/ds/Graph.java) storing vertices and bidirectional weighted edges.
- **Algorithm Mechanics**:
  1. Initialize distance to source $s$ as $0$ and all other vertices as $+\infty$.
  2. Insert `PathNode(s, 0.0)` into min-heap.
  3. While min-heap is not empty:
     - Extract vertex $u$ with minimum tentative distance.
     - If $u$ is destination $d$, terminate early.
     - For each unvisited neighbor $v$ of $u$:
       - Calculate tentative distance: $\text{dist}[v] = \text{dist}[u] + \text{weight}(u, v)$.
       - If $\text{dist}[v] < \text{distances}[v]$, relax the edge, record $u$ as predecessor of $v$, and push `PathNode(v, dist[v])` into heap.
  4. Backtrack from destination to source using predecessors map to reconstruct exact hop sequence.
  5. If destination distance remains $+\infty$, report unreachable destination without crashing.

### 2. Multi-Tier PriorityQueue Request Scheduling
- **Class**: [`ReliefRequest.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/model/ReliefRequest.java)
- **Priority Logic**:
  1. **Primary Criterion**: Urgency Level (`CRITICAL (4)` > `HIGH (3)` > `MEDIUM (2)` > `LOW (1)`).
  2. **Secondary Criterion**: Affected Population count (descending order).
  3. **Tertiary Criterion**: Request timestamp (FIFO / waiting time).

### 3. Drone Allocation (Least Excess Capacity / Best Fit)
- **Class**: [`DroneAllocator.java`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/backend/src/org/rescuedrone/algorithm/DroneAllocator.java)
- **Eligibility Constraints**:
  - Aircraft status must be `AVAILABLE`.
  - Required payload $\le \text{Max Capacity (kg)}$.
  - Round-trip flight $(2 \times \text{distance}) \le \text{Max Range (km)}$.
  - Current battery $\ge \text{Required Battery } (\text{round trip} \times 1.5\% + 10\% \text{ safety buffer})$.
- **Selection Strategy**:
  - From all eligible drones, select the aircraft with **minimum excess capacity** ($\text{capacity} - \text{payload}$) to preserve heavy-lift aircraft for larger missions.
  - Deterministic tie-breaking by higher battery percentage, then alphanumeric Drone ID.

### 4. Delivery Cost Formula
$$\text{Delivery Cost (₹)} = 100 \text{ (Base Fee)} + (\text{Distance in km} \times 20) + (50 \text{ if Urgency is CRITICAL or HIGH})$$

---

## 6. Project Directory Structure

```
java2/
├── backend/
│   ├── src/
│   │   └── org/rescuedrone/
│   │       ├── Main.java                          # REST Server entry point
│   │       ├── algorithm/                         # Core Algorithms
│   │       │   ├── DijkstraOptimizer.java         # Manual Dijkstra with PriorityQueue
│   │       │   └── DroneAllocator.java            # Best-fit drone assignment engine
│   │       ├── config/                            # App configuration & file paths
│   │       ├── ds/                                # Data Structures
│   │       │   └── Graph.java                     # Adjacency list weighted graph
│   │       ├── model/                             # Entities (BaseEntity, Disaster, Drone, ...)
│   │       │   └── enums/                         # 7 Domain Enums
│   │       ├── persistence/                       # File I/O & Data Seeder
│   │       │   ├── FileManager.java               # Atomic swap CSV reader/writer
│   │       │   └── DataSeeder.java                # Baseline demo records
│   │       ├── server/                            # Embedded HttpServer & Handlers
│   │       │   ├── HttpServerManager.java         # HTTP endpoint registration
│   │       │   ├── CorsHandler.java               # Cross-Origin headers & JSON serializer
│   │       │   └── handler/                       # 10 Domain REST Handlers
│   │       ├── service/                           # Business Domain Managers & Services
│   │       │   ├── ReliefSystem.java              # Central coordinator facade
│   │       │   ├── CostCalculator.java            # Logistics formula engine
│   │       │   ├── DeliveryService.java           # End-to-end mission simulation
│   │       │   └── AnalyticsService.java          # Aggregation & JVM telemetry engine
│   │       └── test/                              # Automated Test Suites (Phases 2-10)
│   ├── data/                                      # 7 Persistent CSV storage files
│   └── bin/                                       # Compiled Java bytecode (.class)
├── frontend/
│   ├── src/
│   │   ├── pages/                                 # 11 Menu-Driven Dashboard Views
│   │   │   ├── Dashboard.jsx
│   │   │   ├── DisasterManagement.jsx
│   │   │   ├── LocationManagement.jsx
│   │   │   ├── ReliefRequests.jsx
│   │   │   ├── DroneManagement.jsx
│   │   │   ├── RouteOptimization.jsx
│   │   │   ├── DeliveryManagement.jsx
│   │   │   ├── ResourceManagement.jsx
│   │   │   ├── ReportsAnalytics.jsx
│   │   │   ├── DeliveryHistory.jsx
│   │   │   └── SettingsSystem.jsx
│   │   ├── components/                            # Modular UI components (Sidebar, Header, ...)
│   │   └── services/api.js                        # Frontend REST API client
├── run-backend.bat                                # Double-click to launch Java Backend
├── run-frontend.bat                               # Double-click to launch React UI
├── run-tests.bat                                  # Double-click to run all 9 test suites
├── VIVA_GUIDE.md                                  # Academic Java Lab Examination Guide
└── README.md                                      # Master Project Documentation
```

---

## 7. REST API Reference

All endpoints return JSON responses with standard CORS headers (`*`).

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/health` | Backend status, uptime, and Java runtime version |
| `GET` | `/api/dashboard/stats` | 9 core counters, live PriorityQueue preview, fleet snapshot, activities |
| `GET` / `POST` | `/api/disasters` | List all disasters / Register new disaster |
| `PUT` / `DELETE`| `/api/disasters/{id}` | Update disaster / Delete disaster |
| `GET` / `POST` | `/api/locations` | List all locations / Register new graph vertex |
| `PUT` / `DELETE`| `/api/locations/{id}` | Update location / Delete location with cascade route cleanup |
| `GET` / `POST` | `/api/requests` | List all relief requests / Register new request |
| `GET` | `/api/requests/queue` | Retrieve pending requests ordered by Java `PriorityQueue` heap |
| `GET` / `POST` | `/api/drones` | List all drones / Register new aircraft |
| `PUT` | `/api/drones/{id}/recharge` | Instantly recharge drone battery to $100\%$ |
| `POST`| `/api/drones/assign` | Evaluate fleet and auto-assign best-fit drone for request |
| `GET` / `POST` | `/api/routes` | List all graph routes / Add route edge |
| `POST`| `/api/routes/optimize` | Compute shortest route using manual Dijkstra algorithm |
| `GET` / `POST` | `/api/deliveries` | List active missions / Create new delivery mission |
| `PUT` | `/api/deliveries/{id}/start` | Transition delivery to `IN_TRANSIT` (drone `IN_FLIGHT`) |
| `PUT` | `/api/deliveries/{id}/complete`| Complete delivery, consume inventory, drain battery, relocate drone |
| `PUT` | `/api/deliveries/{id}/cancel` | Abort mission and roll back all reserved assets |
| `GET` | `/api/history` | Audit log of completed and cancelled delivery missions |
| `GET` | `/api/resources` | List all inventory stocks, reservations, and unit weights |
| `GET` | `/api/reports` | Comprehensive operational analytics, fleet metrics, and JVM diagnostics |
| `POST`| `/api/system/reseed` | Reset all CSV storage files to clean demo baseline data |

---

## 8. Setup & Execution Instructions

### Prerequisites
- **Java Development Kit (JDK)**: Java 17 or higher (tested on Java 26 SE).
- **Node.js**: v18+ with `npm`.
- **Operating System**: Windows (tested with PowerShell / Command Prompt).

### Quick Turnkey Startup (Windows)
1. **Launch Backend**:
   - Double-click [`run-backend.bat`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/run-backend.bat).
   - Starts the pure Java REST HTTP server on `http://localhost:8080`.
2. **Launch Frontend**:
   - Double-click [`run-frontend.bat`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/run-frontend.bat).
   - Opens the React Vite dashboard on `http://localhost:3000`.
3. **Run Automated Test Suite**:
   - Double-click [`run-tests.bat`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/run-tests.bat).
   - Executes all 9 test suites (282 automated tests).

---

## 9. Verification & Automated Test Results

Every phase of the project has been rigorously tested using dedicated automated test classes:

| Test Suite | Focus Area | Tests Passed | Status |
| :--- | :--- | :---: | :---: |
| **`Phase2Test`** | OOP Entities, Inheritance, Polymorphism, Encapsulation, Cost Formula | 36 / 36 | 100% |
| **`Phase3Test`** | CSV Persistence, Quote Parsing, Data Structures (`ArrayList`, `HashMap`) | 33 / 33 | 100% |
| **`Phase4Test`** | Disaster, Location & Resource Management, Cascade Cleanup | 24 / 24 | 100% |
| **`Phase5Test`** | Multi-Tier `PriorityQueue` Heap Scheduling & Triage | 19 / 19 | 100% |
| **`Phase6Test`** | Drone Management, Battery Sufficiency, Least-Excess Allocation | 16 / 16 | 100% |
| **`Phase7Test`** | Graph Adjacency List, Dijkstra Shortest Path, Unreachable Vertices | 28 / 28 | 100% |
| **`Phase8Test`** | Delivery Lifecycle, Inventory Reservation, Battery Drain, Rollback | 26 / 26 | 100% |
| **`Phase9Test`** | Operational Analytics, Reporting Engine, JVM Telemetry, CSV Audit | 49 / 49 | 100% |
| **`Phase10IntegrationTest`** | Master End-to-End Mission Pipeline & Edge-Case Integrity | 51 / 51 | 100% |
| **Total** | **All 9 Verification Test Suites** | **282 / 282** | **100% Success** |

---

## 10. Academic Examination & Viva Preparation

For detailed questions, answers, and conceptual breakdowns specifically tailored for academic faculty evaluation and viva examinations, refer to [`VIVA_GUIDE.md`](file:///c:/Users/aryan/OneDrive/Desktop/projects/java2/VIVA_GUIDE.md).
