local virtio = require("virtio")
virtio.gated = true

local harness = require("harness")
local expect, raises, report = harness.expect, harness.raises, harness.report

local bus = require("devices")

local function listReply(gen, ...)
  local devices = {}
  for _, deviceId in ipairs({ ... }) do
    devices[#devices + 1] = { deviceId = deviceId, typeNames = { "redstone" } }
  end
  return { type = "list", gen = gen, data = devices }
end

-- First list hits the wire; the second is served from cache; the third re-confirms.
virtio.reply(listReply(1, "aaa"))
virtio.requests = 0
local a = bus:list()
expect("first list hits the wire", virtio.requests, 1)
local b = bus:list()
expect("second list served from cache", virtio.requests, 1)
expect("cached list content", b[1].deviceId, "aaa")

virtio.reply(listReply(1, "aaa"))
bus:list()
expect("third list re-confirms with the host", virtio.requests, 2)

-- Returned lists must be private: lsdev sorts typeNames in place.
expect("list returns a fresh list each call", a ~= b, true)
expect("entries are copies too", a[1] ~= b[1], true)
expect("nested typeNames are copies", a[1].typeNames ~= b[1].typeNames, true)
a[1].deviceId = "clobbered"
table.remove(a)
expect("mutating a returned list leaves the cache intact", bus:list()[1].deviceId, "aaa")

-- A reply carrying a new generation invalidates the cache.
virtio.reply({ type = "result", gen = 2, data = true })
virtio.requests = 0
bus:invoke("aaa", "setRedstoneOutput", "up", 15)
expect("invoke hits the wire", virtio.requests, 1)

virtio.reply(listReply(2, "aaa", "bbb"))
virtio.requests = 0
local c = bus:list()
expect("list refetched after generation bump", virtio.requests, 1)
expect("refetched list sees the new device", #c, 2)

-- The same generation leaves the cache alone.
virtio.reply({ type = "result", gen = 2, data = true })
virtio.requests = 0
bus:invoke("aaa", "setRedstoneOutput", "up", 0)
bus:list()
expect("unchanged generation keeps cache", virtio.requests, 1)

-- An error reply still carries the generation, and still invalidates.
virtio.reply({ type = "error", gen = 3, data = "unknown device" }, listReply(3, "aaa"))
pcall(function() return bus:invoke("zzz", "nope") end)
expect("error reply invalidates cache too", #bus:list(), 1)

-- find() and get() both resolve, and neither hands out the cached entry.
virtio.reply(listReply(4, "aaa"))
bus.deviceList = nil
local device = bus:find("redstone")
expect("find returns the device", device.deviceId, "aaa")
local byId = bus:get("aaa")
expect("get returns the device", byId.deviceId, "aaa")
expect("get returned a fresh object", byId ~= device, true)

-- A miss must refetch once, or a poll loop waiting for a device never talks to the host.
virtio.reply(listReply(5, "aaa"))
bus.deviceList = nil
bus:list()
virtio.reply(listReply(5, "aaa"))
virtio.requests = 0
expect("miss still reports not found", bus:find("nope"), nil)
expect("miss refetched once", virtio.requests, 1)

-- Now the device appears; the very next find must see it, from the same bus.
virtio.reply({ type = "list", gen = 6, data = {
  { deviceId = "rrr", typeNames = { "redstone" } },
} })
bus.deviceList = { { deviceId = "aaa", typeNames = { "other" } } } -- stale cache
local found = bus:find("redstone")
expect("miss on stale cache then finds new device", found ~= nil, true)
expect("found the newly attached device", found and found.deviceId, "rrr")
expect("device carries its type names", found and found.typeNames[1], "redstone")

-- A reply without a generation is a broken host, not a supported configuration.
virtio.reply({ type = "list", data = { { deviceId = "old" } } })
bus.generation = nil
bus.deviceList = nil
bus.generationConfirmed = false
raises("missing generation is rejected", "bus generation",
       function() return bus:list() end)

-- find() goes through a different path than list(); it used to hand out the cached entry.
virtio.reply(listReply(7, "aaa"))
bus.deviceList = nil
local mine = bus:find("redstone")
mine.typeNames[1] = "clobbered"
expect("mutating a found device leaves the cache intact",
       bus.deviceList[1].typeNames[1], "redstone")

report()
