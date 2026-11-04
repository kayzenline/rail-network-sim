# Rail Network Simulator

A tick-based Java simulator of a rail network, with perishable freight, breakable tracks, and repair trains.

按时间步推进的 Java 铁路网络模拟器，涵盖易腐货物、可损坏轨道和维修列车。

Personal project by Eddie Lin.

## What it is

`rail-network-sim` models a small railway as stations, tracks, trains, and loads. Time moves in discrete ticks. Each tick, trains move, pick up and drop off loads, wear or repair tracks, and perishable freight ages.

A browser UI, served by [Spark Java](https://sparkjava.com/), shows the same world the simulator updates. [Gson](https://github.com/google/gson) serializes the JSON API. The static UI is hosted by a small in-tree helper, `scintilla`.

## Features

- Stations: passenger, freight, central, and supply. Each type has its own train capacity.
- Tracks: ordinary segments, and breakable segments that lose durability and can snap.
- Trains: commuter, freight, maglev, and repair. Maglev trains may run a loop. The others shuttle back and forth.
- Loads: passengers, freight, refrigerated freight, and technicians.
- Freight weight slows a train. Refrigerated freight spoils if it stays out too long, and it will not board a train that cannot reach its destination in time.
- Broken tracks stop ordinary trains. A repair train can still cross them. Technicians on that train add a repair bonus on top of a flat per-tick repair.
- A loop route is rejected for any train that is not a maglev.

## Architecture

```
app/src/main/java/railnet
  App                  Spark JSON API
  trains               Train hierarchy and TrainsController (one tick)
  stations             TrainStation and the four station types
  track                Track durability and break status
  load                 Load hierarchy
  response/models      Immutable query snapshots
  utils                Position and track/station enums
  exceptions           Route and controller errors
app/src/main/java/scintilla
                       In-tree helper that hosts the static UI
```

`TrainsController` is the only type that owns the collections of stations, tracks, and trains. Trains know how to move and what they can carry. Stations know how many trains they can hold and which loads are waiting. Tracks know whether they are intact. Query methods build `*InfoResponse` objects on the way out and do not store those objects on the domain types.

The class diagram is in [docs/architecture.pdf](docs/architecture.pdf). The reasons for the split are in [docs/design-notes.md](docs/design-notes.md).

## Requirements

- Java 21. The Gradle toolchain requests language version 21.
- No global Gradle install. Use the wrapper in this repository.

## Run

```bash
./gradlew test
./gradlew run
```

`test` runs the JUnit 5 suite. `run` starts the API and the static UI on port 4567 and tries to open a browser. Set the environment variable `scintilla:HEADLESS` to any value to skip the browser. The process listens on `0.0.0.0`.

Checkstyle lives in `config/checkstyle.xml`:

```bash
./gradlew lint
```

## HTTP API

- `GET /api/stations` and `GET /api/stations/all`
- `POST /api/station/create` with `stationId`, `type`, `x`, `y`
- `GET /api/trains` and `GET /api/trains/all`
- `POST /api/train/create` with `trainId`, `type`, `stationId`, `route`
- `GET /api/tracks` and `GET /api/tracks/all`
- `POST /api/track/create` with `trackId`, `fromStationId`, `toStationId`, `type` (`NORMAL` or `UNBROKEN`)
- `POST /api/passenger/create` with `startStationId`, `destStationId`, `passengerId`, `type` (`Passenger` or `Technician`)
- `POST /api/freight/create` with `startStationId`, `destStationId`, `freightId`, `weight`, and optional `minsTillSpoil`
- `POST /api/simulate` with `length` (number of ticks)
- `POST /api/reset`

Station types: `PassengerStation`, `FreightStation`, `CentralStation`, `SupplyStation`.

Train types: `CommuterTrain`, `FreightTrain`, `MaglevTrain`, `RepairTrain`.

## Layout

- `app/` — Java sources, tests, and the built UI assets
- `config/checkstyle.xml` — Checkstyle rules
- `docs/design-notes.md` — why the model is shaped this way
- `docs/architecture.pdf` — class diagram
