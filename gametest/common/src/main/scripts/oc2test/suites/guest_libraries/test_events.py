import virtio
virtio.gated = True

from harness import expect, report  # noqa: E402

import devices  # noqa: E402

bus = devices.bus


def event(gen, event_type="devicesChanged"):
    virtio.add(virtio.event, virtio.frame({"type": event_type, "gen": gen}))


def list_reply(gen):
    virtio.reply({"type": "list", "gen": gen, "data": []})


expect("event port was discovered", bus.events is not None, True)

# A list populates the cache at gen 1.
list_reply(1)
bus.list()
expect("cache populated", bus.device_list is not None, True)
expect("generation recorded", bus.generation, 1)

# An event announcing a new generation drops the cache, with no request made.
event(2)
before = virtio.requests
expect("event was applied", bus.pump_events(), 1)
expect("cache dropped by event", bus.device_list, None)
expect("generation advanced by event", bus.generation, 2)
expect("no RPC traffic was needed", virtio.requests, before)

# An event repeating the generation we already have leaves the cache alone.
list_reply(2)
bus.list()
expect("cache repopulated", bus.device_list is not None, True)
event(2)
bus.pump_events()
expect("same generation keeps cache", bus.device_list is not None, True)

# flush() must not eat events.
event(3)
bus.flush()
expect("flush left the event queued", len(virtio.event.chunks), 1)
bus.pump_events()
expect("event still applied after flush", bus.generation, 3)

# Several events collapse to the newest, and list() picks them up on its own.
list_reply(3)
bus.list()
event(4)
event(5)
list_reply(5)
bus.list()
expect("list pumped events itself", bus.generation, 5)

# wait_event hands the frame back to the caller.
event(6)
got = bus.wait_event(0)
expect("wait_event returned the event", got and got.get("type"), "devicesChanged")
expect("wait_event applied it", bus.generation, 6)
expect("wait_event on an empty channel times out", bus.wait_event(0), None)

# Asking for a type skips everything else -- but still applies it.
event(7)
expect("a non-matching event does not satisfy the wait",
       bus.wait_event(0, "robotActionCompleted"), None)
expect("the skipped event was still applied", bus.generation, 7)
expect("the skipped event was consumed", len(virtio.event.chunks), 0)

event(8)
event(8, "robotActionCompleted")
wanted = bus.wait_event(0, "robotActionCompleted")
expect("the wait returned the type it asked for",
       wanted and wanted.get("type"), "robotActionCompleted")
expect("it applied the events it passed over", bus.generation, 8)

# Without a type, the next event of any kind is still what comes back.
event(9)
expect("no type means any event", (bus.wait_event(0) or {}).get("type"), "devicesChanged")


# A device event pumped by list() is kept for the next wait_event.
def device_event(event_type, data):
    virtio.add(virtio.event, virtio.frame({"type": event_type, "gen": 9, "data": data}))


device_event("redstoneChanged", 1)
list_reply(9)
bus.list()
expect("list consumed the frame", len(virtio.event.chunks), 0)
kept = bus.wait_event(0, "redstoneChanged")
expect("wait_event returns the event list pumped", kept and kept.get("data"), 1)
expect("a pumped event is handed out once", bus.wait_event(0), None)

# Pumped events come out ahead of the channel, in order, and a filter discards them too.
device_event("redstoneChanged", 2)
device_event("inventoryChanged", 3)
bus.pump_events()
device_event("redstoneChanged", 4)
expect("pumped events are ordered", (bus.wait_event(0) or {}).get("data"), 2)
expect("the filter discards pumped events too",
       (bus.wait_event(0, "redstoneChanged") or {}).get("data"), 4)
expect("nothing left over", bus.wait_event(0), None)

# A device wrapper only hands out its own events, and discards the rest.
virtio.reply({"type": "list", "gen": 9, "data": [
    {"deviceId": "aaa", "typeNames": ["redstone"]},
    {"deviceId": "bbb", "typeNames": ["redstone"]},
]})
aaa = bus.get("aaa")
expect("wrapper found", aaa is not None, True)
virtio.add(virtio.event, virtio.frame({"type": "redstoneChanged", "gen": 9, "deviceId": "bbb", "data": 1}))
virtio.add(virtio.event, virtio.frame({"type": "redstoneChanged", "gen": 9, "deviceId": "aaa", "data": 2}))
expect("wrapper skips other devices' events",
       (aaa.wait_event(0, "redstoneChanged") or {}).get("data"), 2)
expect("wrapper discarded what it skipped", bus.wait_event(0), None)
expect("wrapper times out like the bus", aaa.wait_event(0), None)

# Pumping without waiting keeps only the newest events.
from oc2 import bus as oc2_bus  # noqa: E402
oc2_bus.MAX_PENDING_EVENTS = 2
device_event("redstoneChanged", 5)
device_event("redstoneChanged", 6)
device_event("redstoneChanged", 7)
bus.pump_events()
expect("oldest pending event dropped", (bus.wait_event(0) or {}).get("data"), 6)
expect("newest pending event kept", (bus.wait_event(0) or {}).get("data"), 7)

report()
