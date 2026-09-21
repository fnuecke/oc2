local virtio = require("virtio")
virtio.gated = true

local harness = require("harness")
local expect, report = harness.expect, harness.report

local bus = require("devices")

local function event(gen, eventType)
  virtio.add(virtio.event, virtio.frame({ type = eventType or "devicesChanged", gen = gen }))
end

local function listReply(gen)
  virtio.reply({ type = "list", gen = gen, data = {} })
end

expect("event port was discovered", bus.events ~= nil, true)

-- A list populates the cache at gen 1.
listReply(1)
bus:list()
expect("cache populated", bus.deviceList ~= nil, true)
expect("generation recorded", bus.generation, 1)

-- An event announcing a new generation drops the cache, with no request made.
event(2)
local before = virtio.requests
expect("event was applied", bus:pumpEvents(), 1)
expect("cache dropped by event", bus.deviceList, nil)
expect("generation advanced by event", bus.generation, 2)
expect("no RPC traffic was needed", virtio.requests, before)

-- An event repeating the generation we already have leaves the cache alone.
listReply(2)
bus:list()
expect("cache repopulated", bus.deviceList ~= nil, true)
event(2)
bus:pumpEvents()
expect("same generation keeps cache", bus.deviceList ~= nil, true)

-- flush() must not eat events.
event(3)
bus:flush()
expect("flush left the event queued", #virtio.event.chunks, 1)
bus:pumpEvents()
expect("event still applied after flush", bus.generation, 3)

-- Several events collapse to the newest, and list() picks them up on its own.
listReply(3)
bus:list()
event(4)
event(5)
listReply(5)
bus:list()
expect("list pumped events itself", bus.generation, 5)

-- waitEvent hands the frame back to the caller.
event(6)
local got = bus:waitEvent(0)
expect("waitEvent returned the event", got and got.type, "devicesChanged")
expect("waitEvent applied it", bus.generation, 6)
expect("waitEvent on an empty channel times out", bus:waitEvent(0), nil)

-- Asking for a type skips everything else -- but still applies it.
event(7)
expect("a non-matching event does not satisfy the wait",
       bus:waitEvent(0, "robotActionCompleted"), nil)
expect("the skipped event was still applied", bus.generation, 7)
expect("the skipped event was consumed", #virtio.event.chunks, 0)

event(8)
event(8, "robotActionCompleted")
local wanted = bus:waitEvent(0, "robotActionCompleted")
expect("the wait returned the type it asked for",
       wanted and wanted.type, "robotActionCompleted")
expect("it applied the events it passed over", bus.generation, 8)

-- Without a type, the next event of any kind is still what comes back.
event(9)
expect("no type means any event", (bus:waitEvent(0) or {}).type, "devicesChanged")

-- A device event pumped by list() is kept for the next waitEvent.
local function deviceEvent(eventType, data)
  virtio.add(virtio.event, virtio.frame({ type = eventType, gen = 9, data = data }))
end

deviceEvent("redstoneChanged", 1)
listReply(9)
bus:list()
expect("list consumed the frame", #virtio.event.chunks, 0)
local kept = bus:waitEvent(0, "redstoneChanged")
expect("waitEvent returns the event list pumped", kept and kept.data, 1)
expect("a pumped event is handed out once", bus:waitEvent(0), nil)

-- Pumped events come out ahead of the channel, in order, and a filter discards them too.
deviceEvent("redstoneChanged", 2)
deviceEvent("inventoryChanged", 3)
bus:pumpEvents()
deviceEvent("redstoneChanged", 4)
expect("pumped events are ordered", (bus:waitEvent(0) or {}).data, 2)
expect("the filter discards pumped events too", (bus:waitEvent(0, "redstoneChanged") or {}).data, 4)
expect("nothing left over", bus:waitEvent(0), nil)

-- A device wrapper only hands out its own events, and discards the rest.
virtio.reply({ type = "list", gen = 9, data = {
  { deviceId = "aaa", typeNames = { "redstone" } },
  { deviceId = "bbb", typeNames = { "redstone" } },
} })
local aaa = assert(bus:get("aaa"))
virtio.add(virtio.event, virtio.frame({ type = "redstoneChanged", gen = 9, deviceId = "bbb", data = 1 }))
virtio.add(virtio.event, virtio.frame({ type = "redstoneChanged", gen = 9, deviceId = "aaa", data = 2 }))
expect("wrapper skips other devices' events", (aaa:waitEvent(0, "redstoneChanged") or {}).data, 2)
expect("wrapper discarded what it skipped", bus:waitEvent(0), nil)
expect("wrapper times out like the bus", aaa:waitEvent(0), nil)

-- Pumping without waiting keeps only the newest events.
require("oc2.bus").maxPendingEvents = 2
deviceEvent("redstoneChanged", 5)
deviceEvent("redstoneChanged", 6)
deviceEvent("redstoneChanged", 7)
bus:pumpEvents()
expect("oldest pending event dropped", (bus:waitEvent(0) or {}).data, 6)
expect("newest pending event kept", (bus:waitEvent(0) or {}).data, 7)

report()
