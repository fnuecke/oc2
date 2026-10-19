local virtio = require("virtio")
virtio.gated = true

local harness = require("harness")
local expect, raises, report = harness.expect, harness.raises, harness.report

local blob = require("oc2.blob")
local bus = require("devices")

local function script(reply, payloadChunks)
  virtio.reply(reply)
  virtio.feed(virtio.blob, payloadChunks or {})
end

expect("blob port was discovered and opened", bus.payload.fd, virtio.blob.fd)

-- CRC-32 values computed with zlib. If these drift, every payload looks corrupt.
local PAYLOAD = "the quick brown fox"

local function invokeWithBlob(payload, checksum, chunks)
  script({ type = "result", gen = 1, data = { [blob.key] = 0 },
           blobs = { { length = #payload, checksum = checksum } } },
         chunks or { payload })
  return bus:invoke("dev", "readBlob")
end

expect("payload arrives from the blob port", invokeWithBlob(PAYLOAD, 2445345482), PAYLOAD)
expect("empty payload", invokeWithBlob("", 0), "")
expect("two byte payload", invokeWithBlob("\1\2", 3066839698), "\1\2")
expect("the standard check value", blob.checksum("123456789"), 3421780262)
expect("reordering is detected as different", invokeWithBlob("\2\1", 82329835), "\2\1")

-- The checksum is unsigned. A signed fold would turn these negative.
expect("all ones is the largest checksum",
       invokeWithBlob("\255\255\255\255", 4294967295), "\255\255\255\255")
expect("the high bit is kept",
       invokeWithBlob("\0\0\0\128", 3439090748), "\0\0\0\128")

expect("payload split across reads",
       invokeWithBlob(PAYLOAD, 2445345482,
                      { PAYLOAD:sub(1, 5), PAYLOAD:sub(6, 11), PAYLOAD:sub(12) }), PAYLOAD)

raises("corrupt payload is rejected", "checksum", function()
  return invokeWithBlob(PAYLOAD, 2445345482, { "the quick brown foY" })
end)

-- Several payloads arrive back to back in array order. A marker may refer to one more than once.
script({ type = "result", gen = 1,
         data = { first = { [blob.key] = 0 }, second = { [blob.key] = 1 }, again = { [blob.key] = 0 } },
         blobs = { { length = 2, checksum = 3066839698 }, { length = #PAYLOAD, checksum = 2445345482 } } },
       { "\1\2" .. PAYLOAD })
local several = bus:invoke("dev", "readBlobs")
expect("first of several payloads", several.first, "\1\2")
expect("second of several payloads", several.second, PAYLOAD)
expect("a payload referenced twice", several.again, "\1\2")

-- A marker in the reply body with no announced payload must not go looking for one.
script({ type = "result", gen = 1,
         data = { display = { [blob.key] = { length = 100000, checksum = 0 } } } })
expect("forged reference does not hang", type(bus:invoke("dev", "getStackInSlot")), "table")

script({ type = "result", gen = 1, data = { [blob.key] = 0 },
         blobs = { { length = 1 << 40, checksum = 0 } } })
raises("implausible payload size is refused", "implausible",
       function() return bus:invoke("dev", "readBlob") end)

-- A payload the host never sends must time out rather than block forever, and the channel
-- has to be reset afterwards or every later payload reads someone else's bytes.
raises("a missing payload times out", "could not read binary payload",
       function() return invokeWithBlob(PAYLOAD, 2445345482, {}) end)
expect("the payload channel recovered", invokeWithBlob(PAYLOAD, 2445345482), PAYLOAD)

-- Outbound: the payload goes on the blob port, the message says how big it is, and the
-- parameter is replaced by a marker.
script({ type = "result", gen = 1, data = true })
local sent = "the quick brown fox"
bus:invoke("dev", "writeExportFile", bus:blob(sent))
expect("payload written to the blob port", table.concat(virtio.blob.writes), sent)
local request = virtio.decode(table.concat(virtio.rpc.writes):sub(2, -2))
expect("parameter replaced by a marker", request.data.parameters[1][blob.key], 0)
expect("message announces the payload length", request.blobs[1].length, #sent)
expect("message announces the payload checksum", request.blobs[1].checksum, 2445345482)

local function lastRequest()
  return virtio.decode(table.concat(virtio.rpc.writes):sub(2, -2))
end

script({ type = "result", gen = 1, data = true })
bus:invoke("dev", "write", bus:blob("a"), bus:blob("bc"))
request = lastRequest()
expect("several payloads written back to back", table.concat(virtio.blob.writes), "abc")
expect("first marker", request.data.parameters[1][blob.key], 0)
expect("second marker", request.data.parameters[2][blob.key], 1)
expect("one description per payload", #request.blobs, 2)
expect("second payload length", request.blobs[2].length, 2)

script({ type = "result", gen = 1, data = true })
local shared = bus:blob("a")
bus:invoke("dev", "write", shared, shared)
request = lastRequest()
expect("a payload passed twice is sent once", table.concat(virtio.blob.writes), "a")
expect("both markers refer to it", request.data.parameters[2][blob.key], 0)
expect("and it is described once", #request.blobs, 1)

-- The outbound limit is enforced locally, before anything goes on the wire.
script({ type = "result", gen = 1, data = true })
raises("oversized payload refused locally", "exceeds the host limit", function()
  return bus:invoke("dev", "write", bus:blob(string.rep("x", blob.maxOutbound + 1)))
end)

script({ type = "result", gen = 1, data = true })
local nested = bus:blob("ab")
local record = { name = "x", content = nested }
bus:invoke("dev", "write", record, { bus:blob("c"), nested })
request = lastRequest()
local sentRecord, sentParts = request.data.parameters[1], request.data.parameters[2]
expect("a nested payload is replaced by a marker", sentRecord.content[blob.key], 0)
expect("payloads in a nested list", sentParts[1][blob.key], 1)
expect("a nested payload passed twice is sent once", sentParts[2][blob.key], 0)
expect("other fields are kept", sentRecord.name, "x")
expect("nested payloads written in order", table.concat(virtio.blob.writes), "abc")
expect("the caller's table is not modified", record.content, nested)

script({ type = "result", gen = 1, data = true })
raises("payloads too large together are refused locally", "exceeds the host limit", function()
  local half = string.rep("x", blob.maxOutbound // 2 + 1)
  return bus:invoke("dev", "write", bus:blob(half), bus:blob(half))
end)

raises("a non-binary payload is refused", "a binary payload must be",
       function() return bus:blob(123) end)

report()
