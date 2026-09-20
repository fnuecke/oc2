# Robotics
[Robots](item/robot.md) are [computers](block/computer.md) that can move. Everything in the [basics](basics.md) and [HLAPI](hlapi.md)/[MLAPI](mlapi.md) entries applies to them as well. For the full method reference, see the [robot](item/robot.md) entry.

## Building a Robot
A robot is configured much like a computer, with two differences.

It cannot connect to [bus cables](block/bus_cable.md), so all its devices must be installed in the robot itself. In place of cards, robots take modules: the [block operations module](item/block_operations_module.md), the [inventory operations module](item/inventory_operations_module.md) and the [network tunnel module](item/network_tunnel_module.md).

It has two inventories. The component inventory holds the processor, memory, storage and modules, and must be filled by hand. The regular inventory is the robot's storage, and the only one a hopper or similar machine can use.

## Energy
A robot draws energy from its internal storage for as long as its computer runs. Consumption depends on the installed components; moving and turning cost nothing on top of that. When it runs out, the computer stops with an out-of-energy error, wherever the robot is.

Recharge using a [charger](block/charger.md). A robot can move onto one by itself, so an unattended recharge is easy to script. Breaking a robot and picking it up preserves its energy and its inventory, including the installed components.

## Action Queue
`move` and `turn` do not move the robot synchronously. They append an action to a queue and return whether there was room for it. The robot then runs the queue one action at a time. Moving one block takes a second, turning ninety degrees takes a second.

Every enqueued action gets an id, available from `getLastActionId()`. Once the robot has finished it, `getActionResult(id)` reports `SUCCESS` or `FAILURE`, and the computer receives a `robotActionCompleted` event with the same information. Until then the result is `INCOMPLETE`. Only the last sixteen results are kept.

A move into an obstacle is not rejected up front. The robot starts moving, hits the obstacle, returns to its original position and reports `FAILURE`. Shutting the computer down clears both the queue and the stored results.

## Directions and Sides
Movement and module operations use different terminology.

Movement takes `forward`, `backward`, `upward` and `downward`; rotation takes `left` and `right`. Module operations take a side, which is one of `front`, `up` and `down`. There is no `forward` side and no `front` direction.

Both accept short forms. `up`, `down`, `back` and `ahead` work for movement, and the single letters `f`, `b`, `u`, `d`, `l` and `r` work wherever the corresponding long form does.

## The robot Library
Driving the action queue by hand is tedious, so the default Linux distribution ships a `robot` library that waits for completion:

`local r = require("robot")`  
`r.move("forward")`  
`r.turn("left")`

`move` and `turn` block until the action has completed and return whether it succeeded. `moveAsync` and `turnAsync` block only until the action is in the queue. All four take an optional timeout in milliseconds, defaulting to thirty seconds, and return `false` if it runs out. For the asynchronous pair, a timeout means the queue stayed full for that long.

An equivalent library is available for micropython.

## Example
This walks the robot around a three by three square:

`local r = require("robot")`  
`for _ = 1, 4 do`  
`  for _ = 1, 3 do r.move("forward") end`  
`  r.turn("left")`  
`end`

This digs a straight tunnel, stopping as soon as it cannot get any further. It needs a block operations module and a pickaxe in the selected slot:

`local d = require("devices")`  
`local r = require("robot")`  
`local m = assert(d:find("block_operations"),`
`                 "no block operations module")`  
`for _ = 1, 16 do`  
`  m:excavate("front")`  
`  if not r.move("forward") then break end`  
`end`

A move takes a second, usually enough for the module's cooldown to expire. On a block that takes longer to break, the next `excavate` returns `false` without doing anything, the move that follows fails, and the loop stops early. Retry the excavation to dig through those as well.
