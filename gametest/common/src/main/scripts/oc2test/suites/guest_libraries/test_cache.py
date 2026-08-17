import virtio
virtio.gated = True

from harness import expect, raises, report  # noqa: E402

import devices  # noqa: E402

bus = devices.bus


def list_reply(gen, *device_ids):
    return {"type": "list", "gen": gen,
            "data": [{"deviceId": i, "typeNames": ["redstone"]} for i in device_ids]}


# First list hits the wire; the second is served from cache; the third re-confirms.
virtio.reply(list_reply(1, "aaa"))
virtio.requests = 0
a = bus.list()
expect("first list hits the wire", virtio.requests, 1)
b = bus.list()
expect("second list served from cache", virtio.requests, 1)
expect("cached list content", b[0]["deviceId"], "aaa")

virtio.reply(list_reply(1, "aaa"))
bus.list()
expect("third list re-confirms with the host", virtio.requests, 2)

# Returned lists must be private: lsdev sorts typeNames in place.
expect("list returns a fresh list each call", a is not b, True)
expect("entries are copies too", a[0] is not b[0], True)
expect("nested typeNames are copies", a[0]["typeNames"] is not b[0]["typeNames"], True)
a[0]["deviceId"] = "clobbered"
a.pop()
expect("mutating a returned list leaves the cache intact", bus.list()[0]["deviceId"], "aaa")

# A reply carrying a new generation invalidates the cache.
virtio.reply({"type": "result", "gen": 2, "data": True})
virtio.requests = 0
bus.invoke("aaa", "setRedstoneOutput", "up", 15)
expect("invoke hits the wire", virtio.requests, 1)

virtio.reply(list_reply(2, "aaa", "bbb"))
virtio.requests = 0
c = bus.list()
expect("list refetched after generation bump", virtio.requests, 1)
expect("refetched list sees the new device", len(c), 2)

# The same generation leaves the cache alone.
virtio.reply({"type": "result", "gen": 2, "data": True})
virtio.requests = 0
bus.invoke("aaa", "setRedstoneOutput", "up", 0)
bus.list()
expect("unchanged generation keeps cache", virtio.requests, 1)

# An error reply still carries the generation, and still invalidates.
virtio.reply({"type": "error", "gen": 3, "data": "unknown device"}, list_reply(3, "aaa"))
try:
    bus.invoke("zzz", "nope")
except Exception:
    pass
expect("error reply invalidates cache too", len(bus.list()), 1)

# find() and get() both resolve, and neither hands out the cached entry.
virtio.reply(list_reply(4, "aaa"))
bus.device_list = None
device = bus.find("redstone")
expect("find returns the device", device.device_id, "aaa")
by_id = bus.get("aaa")
expect("get returns the device", by_id.device_id, "aaa")
expect("get returned a fresh object", by_id is not device, True)

# A miss must refetch once, or a poll loop waiting for a device never talks to the host.
virtio.reply(list_reply(5, "aaa"))
bus.device_list = None
bus.list()
virtio.reply(list_reply(5, "aaa"))
virtio.requests = 0
expect("miss still reports not found", bus.find("nope"), None)
expect("miss refetched once", virtio.requests, 1)

# Now the device appears; the very next find must see it, from the same bus.
virtio.reply({"type": "list", "gen": 6, "data": [
    {"deviceId": "rrr", "typeNames": ["redstone"]},
]})
bus.device_list = [{"deviceId": "aaa", "typeNames": ["other"]}]  # stale cache
found = bus.find("redstone")
expect("miss on stale cache then finds new device", found is not None, True)
expect("found the newly attached device", found and found.device_id, "rrr")
expect("device carries its type names", found and found.type_names[0], "redstone")

# A reply without a generation is a broken host, not a supported configuration.
virtio.reply({"type": "list", "data": [{"deviceId": "old"}]})
bus.generation = None
bus.device_list = None
bus.generation_confirmed = False
raises("missing generation is rejected", "bus generation", lambda: bus.list())

# find() goes through a different path than list(); it used to hand out the cached entry.
virtio.reply(list_reply(7, "aaa"))
bus.device_list = None
mine = bus.find("redstone")
mine.type_names[0] = "clobbered"
expect("mutating a found device leaves the cache intact",
       bus.device_list[0]["typeNames"][0], "redstone")

report()
