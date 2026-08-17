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

-- The checksums are the same literals pinned on the Java side, computed independently of
-- both implementations. If these drift, every payload looks corrupt.
local PAYLOAD = "the quick brown fox"

local function invokeWithBlob(payload, checksum, chunks)
  script({ type = "result", gen = 1, data = { [blob.key] = true },
           blob = { length = #payload, checksum = checksum } },
         chunks or { payload })
  return bus:invoke("dev", "readBlob")
end

expect("payload arrives from the blob port", invokeWithBlob(PAYLOAD, 746590911), PAYLOAD)
expect("empty payload", invokeWithBlob("", 0), "")
expect("two byte payload", invokeWithBlob("\1\2", 513), "\1\2")
expect("reordering is detected as different", invokeWithBlob("\2\1", 258), "\2\1")

expect("payload split across reads",
       invokeWithBlob(PAYLOAD, 746590911,
                      { PAYLOAD:sub(1, 5), PAYLOAD:sub(6, 11), PAYLOAD:sub(12) }), PAYLOAD)

raises("corrupt payload is rejected", "checksum", function()
  return invokeWithBlob(PAYLOAD, 746590911, { "the quick brown foY" })
end)

-- A marker in the reply body with no announced payload must not go looking for one.
script({ type = "result", gen = 1,
         data = { display = { [blob.key] = { length = 100000, checksum = 0 } } } })
expect("forged reference does not hang", type(bus:invoke("dev", "getStackInSlot")), "table")

script({ type = "result", gen = 1, data = { [blob.key] = true },
         blob = { length = 1 << 40, checksum = 0 } })
raises("implausible payload size is refused", "implausible",
       function() return bus:invoke("dev", "readBlob") end)

-- A payload the host never sends must time out rather than block forever, and the channel
-- has to be reset afterwards or every later payload reads someone else's bytes.
raises("a missing payload times out", "could not read binary payload",
       function() return invokeWithBlob(PAYLOAD, 746590911, {}) end)
expect("the payload channel recovered", invokeWithBlob(PAYLOAD, 746590911), PAYLOAD)

-- Outbound: the payload goes on the blob port, the message says how big it is, and the
-- parameter is replaced by a marker.
script({ type = "result", gen = 1, data = true })
local sent = "the quick brown fox"
bus:invoke("dev", "writeExportFile", bus:blob(sent))
expect("payload written to the blob port", table.concat(virtio.blob.writes), sent)
local request = virtio.decode(table.concat(virtio.rpc.writes):sub(2, -2))
expect("parameter replaced by a marker", request.data.parameters[1][blob.key], true)
expect("message announces the payload length", request.blob.length, #sent)
expect("message announces the payload checksum", request.blob.checksum, 746590911)

script({ type = "result", gen = 1, data = true })
raises("two payloads in one call refused", "at most one",
       function() return bus:invoke("dev", "write", bus:blob("a"), bus:blob("b")) end)

-- The outbound limit is enforced locally, before anything goes on the wire.
script({ type = "result", gen = 1, data = true })
raises("oversized payload refused locally", "exceeds the host limit", function()
  return bus:invoke("dev", "write", bus:blob(string.rep("x", blob.maxOutbound + 1)))
end)

raises("a non-binary payload is refused", "a binary payload must be",
       function() return bus:blob(123) end)

report()
