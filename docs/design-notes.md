# Design notes

This note records why the rail network is split the way it is, and how one tick is ordered. The class diagram that matches this layout is [architecture.pdf](architecture.pdf).

## Why the types are split

Stations, tracks, trains, and loads change for different reasons, so they are separate types.

- A station is a place. It has a position, a train cap, and a list of loads waiting there. It does not move.
- A track is an undirected edge between two stations. Ordinary tracks never wear. Breakable tracks have durability and a status.
- A train is the only thing that moves. It owns its route, its position, and the loads it is carrying.
- A load is cargo with a destination. Passengers and freight differ in who may carry them and whether they affect speed or durability.

`TrainsController` is the composition root. It creates the world, answers queries, and runs ticks. Entity classes do not reach into each other's collections. The controller looks up a station, track, or train by id and passes the object into the method that needs it.

Info types under `railnet.response.models` are immutable snapshots built inside query methods. Domain objects do not keep them as fields. That keeps the simulation model free of response shapes, and it keeps JSON (and the tests) on a stable value object.

## Inheritance, and what stays open

Shared behaviour sits on abstract bases. Concrete types only declare what is actually different.

| Type | Capacity | Speed | Passengers | Freight | Loop route | Broken track |
| --- | --- | --- | --- | --- | --- | --- |
| `CommuterTrain` | 3500 | 2.0 | yes | no | no | stopped |
| `FreightTrain` | 7000 | 4.0 | no | yes | no | stopped |
| `MaglevTrain` | 6000 | 6.0 | yes | yes | yes | stopped |
| `RepairTrain` | 3500 | 2.0 | yes | no | no | may enter |

`RepairTrain` extends `CommuterTrain` rather than `Train`. It is a passenger train with a linear route, the same speed, and the same load cap. The extra behaviour is damage, permission to enter a broken track, and a repair hook. Extending the commuter type reuses that behaviour instead of copying it.

Station types differ only by how many trains may dock:

- `PassengerStation` — 4
- `FreightStation` — 3
- `CentralStation` — 8
- `SupplyStation` — 6

Loads use the same idea. `Passenger` weighs 70 and may board a train whose `carryPassenger()` is true. `Technician` is a passenger that counts as one technician. `Freight` reports its weight as slowdown weight and may board a train whose `carryFreight()` is true. `RefrigeratedFreight` adds a spoil timer on top of freight.

Each of those differences is a method override. `Train.move` does not switch on train type, and `Load` does not switch on load type. A new train or load can be added by subclassing and by one branch in the controller's create method.

## Routes

A route is cyclical when it has at least three stations and a track joins the first station to the last. `createTrain` asks the train `cyclicalRoute()`. A train that returns false, given a loop, raises `InvalidRouteException`. Maglev is the only type that returns true. Everyone else shuttles: at either end of the route the stored direction flips, and the train goes back the way it came.

`getTargetStationId` is a query. It computes the next index in a local variable and does not write `direction`. Direction changes only when the train actually arrives at an endpoint. That keeps a lookup from changing the train as a side effect.

## One tick

`simulate(n)` calls `simulate()` once per tick. Inside a tick the order is fixed:

1. Sort trains by id. Lexicographic order decides who moves first when two trains want the same dock.
2. For each train, find the target station and whether that station is already full.
3. If the train is docked, the current track is the segment between its station and the target (`getTrackBetween`). If it is already on a segment, the current track is the one named by its location. Location is a station id while docked, so a lookup by that id is not a track.
4. If the train is docked and `canMove` is true, the station embarks loads. Embark is sorted by load id.
5. `move`. If the train arrives, the track takes damage and loads whose destination is this station disembark.
6. Every track receives `repair(1)`.
7. Every track updates its status from its durability.
8. Loads waiting at stations, then loads on trains, deteriorate. A spoiled load is removed.

Deterioration is last on purpose. If it ran first, refrigerated freight that could still depart this tick would spoil before the train left.

`canMove` is also the gate for embark. A train that is blocked (broken track, or the next station is full and the train is close enough to arrive) does not pick up loads. Otherwise cargo would leave the platform while the train stayed put.

While a train is docked, the track must be resolved with `getTrackBetween`. `getTrack` looks up by id, and a docked train's location is a station id, so that lookup is null.

## Embark rules

A station embarks a load onto a train only when all of the following hold:

- The load's destination is on the train's route.
- The train's current weight plus the load's weight is within the train's cap.
- `load.canBeCarriedBy(train)` is true (passengers and freight are not interchangeable).
- `load.canSurviveTrip(estimatedMinutes)` is true.

The estimate walks the route from the docked station to the destination at the train's current speed, using the same loop-or-shuttle rule the train will actually follow. Perishable freight returns false when that time is greater than the minutes it has left. Other loads always survive.

Embark only considers loads on the station the train is docked at. A passenger waiting somewhere else stays there.

## Speed, damage, and spoilage

Speed is `baseSpeed * (1 - freightWeight * 0.0001)`. Only freight weight counts. A passenger does not slow the train. 1000 kg of freight is a 10% reduction, so a freight train at base speed 4.0 moves at 3.6.

On arrival, damage is `1 + ceil(totalLoadWeight / 1000)`. `RepairTrain` overrides this to 0, so a repair train never wears the segment it arrives on. Ordinary tracks ignore durability changes.

`RefrigeratedFreight.deteriorate` subtracts one minute. At 0 or below, `isSpoiled` is true and the load is dropped, whether it is still on a platform or already on a train. After it spoils on a train, the train's speed is computed again from the freight that remains.

## Breakable tracks and the repair bonus

A breakable track is created as `UNBROKEN` with durability 10 (`Track.MAX_DURABILITY`). `NORMAL` tracks stay at 10 and never change type.

`decreaseDur` applies only to breakable tracks and clamps at 0. `repair` applies only while the type is `BROKEN`, and clamps at 10. `updateStatus` runs after damage and repair:

- `UNBROKEN` with durability 0 becomes `BROKEN`.
- `BROKEN` with durability 10 becomes `UNBROKEN`.

So a track that hits 0 during this tick is not broken until the status pass. Repair in the same tick does not stick to it yet, because `repair` returns immediately unless the type is already `BROKEN`.

The flat repair and the technician bonus are separate so they are not applied twice:

- `TrainsController` calls `track.repair(1)` once per track per tick. That is the whole network's baseline, and it does nothing unless the track is broken.
- `Train.onMoved` is an empty hook. `RepairTrain` overrides it and calls `track.repair(2 * technicianCount)`.
- The hook runs from `move` both when the train reaches a station and when it is still travelling the segment. A repair train therefore bonuses a broken track on the ticks it spends crossing it, not only on the tick it arrives.
- The 2 is only the extra per technician. The baseline 1 stays in the controller. A repair train with no technicians still benefits from that baseline, and it can enter a broken track (`canMoveOnBrokenTrack`). It does not add a second +1 inside `repairRate`.

`Track.connects(a, b)` is the single definition of an undirected edge. `hasTrack` delegates to `getTrackBetween`, and both ends of a segment match. `createTrack` without a breakable flag delegates to the full method with `false`. `createPassenger` without a technician flag delegates to the full method with `false`.

## What is hidden

Lookup helpers on the controller (`getTrain`, `getStation`, `getTrack`, `getTrackBetween`) are private. Callers use the public create, query, and simulate methods. `Track.setType` is private so status changes go through `updateStatus`. Ids, type names, caps, and speeds that do not change are `final` or named constants (`MAX_LOAD`, `SPEED`, `MAX_TRAINS`, `MAX_DURABILITY`, `REPAIR_BONUS_PER_TECH`, `FREIGHT_SPEED_REDUCTION_FACTOR`, `PASSENGER_WEIGHT`).
