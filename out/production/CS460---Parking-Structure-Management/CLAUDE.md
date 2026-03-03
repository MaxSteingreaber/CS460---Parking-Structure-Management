# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

CS460 university course project: a **Multistory Parking Structure Management System (MPSMS)** administrative dashboard written in Java (Swing GUI). Administrators can monitor occupancy, trigger the emergency state, restrict spaces, and simulate vehicle entry/exit — all without physical hardware.

## Build & Run

Plain Java project (no Maven/Gradle). IntelliJ IDEA is the primary IDE, configured with **OpenJDK 20**. Compiled output goes to `out/`.

**Compile all sources from project root:**
```bash
javac -d out $(find src -name "*.java")
```

**Run the application:**
```bash
java -cp out Main
```

**Compile a single file (example):**
```bash
javac -cp out -d out src/controller/MainController.java
```

Persistent data is serialized to `data/*.dat` files in the working directory on shutdown.

## Package Structure

`src/` is the source root. All packages live beneath it.

| Package | Responsibility |
|---|---|
| `controller` | `MainController` (central orchestrator), `UserInputController`, `FacilitiesInputController`, `EmergencyOutputController`, `FacilitiesOutputController` |
| `input` | Simulated input devices: `EntryKiosk`, `ExitKiosk`, `AdminCommands`, `InductionLoopSensor`, `WeightSensor`, `EmergencySignal`, `PowerFaultDetector` |
| `output` | Simulated output devices: `Siren`, `LEDController`, `GateActuator`, `FloorLevelDisplay`, `EntranceDisplay`, `TicketDispenser` |
| `datastore` | `DataStoreDriver` (serialized file I/O), `SessionLogger`, `CapacityMonitor`, `TransactionArchive` |
| `model` | Core data model: `ParkingStructure` → `Floor` → `ParkingSpace`, plus `Session`, `Transaction`, `Ticket` |
| `gui` | Swing panels: `DashboardFrame`, `BannerPanel`, `ControlConsolePanel`, `StructureViewPanel`, `EventLogPanel` |
| `enums` | `SpaceState`, `LEDState`, `AllocationStrategy`, `SystemState` |
| `observer` | `SystemObserver` (interface), `SystemEvent`, `EventType` |

`Main.java` (default package, in `src/`) is the entry point.

## Architecture

### Data Flow (vehicle entry)
`DashboardFrame` → `AdminCommands.simulateEntry()` → `UserInputController` → `MainController.handleVehicleEntry()` → queries `CapacityMonitor` for a free space → commands `FacilitiesOutputController` (gate, ticket, floor display) → writes to `SessionLogger` / `CapacityMonitor` via `DataStoreDriver` → fires `SystemEvent` → all registered `SystemObserver` panels repaint.

### Observer Pattern
`MainController` holds a `List<SystemObserver>`. GUI panels (`DashboardFrame`, `ControlConsolePanel`, `StructureViewPanel`, `EventLogPanel`) register at startup. Every state change fires `notifyObservers(SystemEvent)` — no polling needed.

### Persistence
`DataStoreDriver` manages three `Serializable` collections (`SessionLogger`, `CapacityMonitor`, `TransactionArchive`). It deserializes them from `data/*.dat` on `initialize()` and serializes them back on `close()` (triggered by the window-closing hook in `DashboardFrame`).

### Wiring
All subsystems are instantiated and wired together in `Main.main()` via setters on `MainController`, then `mainController.initialize()` is called before the GUI is shown.
