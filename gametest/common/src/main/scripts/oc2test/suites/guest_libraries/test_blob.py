import virtio
virtio.gated = True

from harness import expect, raises, report  # noqa: E402

from oc2 import blob as oc2_blob  # noqa: E402

import devices  # noqa: E402

bus = devices.bus


def script(reply, payload_chunks=()):
    virtio.reply(reply)
    virtio.feed(virtio.blob, payload_chunks)


expect("blob port was discovered and opened", bus.payload.file is virtio.blob, True)

# CRC-32 values computed with zlib. If these drift, every payload looks corrupt.
PAYLOAD = b"the quick brown fox"


def invoke_with_blob(payload, checksum, chunks=None):
    script({"type": "result", "gen": 1, "data": {oc2_blob.KEY: 0},
            "blobs": [{"length": len(payload), "checksum": checksum}]},
           [payload] if chunks is None else chunks)
    return bus.invoke("dev", "readBlob")


expect("payload arrives from the blob port", invoke_with_blob(PAYLOAD, 2445345482), PAYLOAD)
expect("empty payload", invoke_with_blob(b"", 0), b"")
expect("two byte payload", invoke_with_blob(b"\1\2", 3066839698), b"\1\2")
expect("the standard check value", oc2_blob.checksum(b"123456789"), 3421780262)
expect("reordering is detected as different", invoke_with_blob(b"\2\1", 82329835), b"\2\1")

# The checksum is unsigned. A signed fold would turn these negative.
expect("all ones is the largest checksum",
       invoke_with_blob(b"\xff\xff\xff\xff", 4294967295), b"\xff\xff\xff\xff")
expect("the high bit is kept",
       invoke_with_blob(b"\x00\x00\x00\x80", 3439090748), b"\x00\x00\x00\x80")

expect("payload split across reads",
       invoke_with_blob(PAYLOAD, 2445345482,
                        [PAYLOAD[:5], PAYLOAD[5:11], PAYLOAD[11:]]), PAYLOAD)

raises("corrupt payload is rejected", "checksum",
       lambda: invoke_with_blob(PAYLOAD, 2445345482, [b"the quick brown foY"]))

# Several payloads arrive back to back in array order. A marker may refer to one more than once.
script({"type": "result", "gen": 1,
        "data": {"first": {oc2_blob.KEY: 0}, "second": {oc2_blob.KEY: 1},
                 "again": {oc2_blob.KEY: 0}},
        "blobs": [{"length": 2, "checksum": 3066839698}, {"length": len(PAYLOAD), "checksum": 2445345482}]},
       [b"\1\2" + PAYLOAD])
several = bus.invoke("dev", "readBlobs")
expect("first of several payloads", several["first"], b"\1\2")
expect("second of several payloads", several["second"], PAYLOAD)
expect("a payload referenced twice", several["again"], b"\1\2")

# A marker in the reply body with no announced payload must not go looking for one.
script({"type": "result", "gen": 1,
        "data": {"display": {oc2_blob.KEY: {"length": 100000, "checksum": 0}}}})
expect("forged reference does not hang",
       isinstance(bus.invoke("dev", "getStackInSlot"), dict), True)

script({"type": "result", "gen": 1, "data": {oc2_blob.KEY: 0},
        "blobs": [{"length": 1 << 40, "checksum": 0}]})
raises("implausible payload size is refused", "implausible",
       lambda: bus.invoke("dev", "readBlob"))

# A payload the host never sends must time out rather than block forever, and the channel
# has to be reset afterwards or every later payload reads someone else's bytes.
raises("a missing payload times out", "could not read binary payload",
       lambda: invoke_with_blob(PAYLOAD, 2445345482, []))
expect("the payload channel recovered", invoke_with_blob(PAYLOAD, 2445345482), PAYLOAD)

# Outbound: the payload goes on the blob port, the message says how big it is, and the
# parameter is replaced by a marker.
script({"type": "result", "gen": 1, "data": True})
sent = b"the quick brown fox"
bus.invoke("dev", "writeExportFile", bus.blob(sent))
expect("payload written to the blob port", b"".join(virtio.blob.writes), sent)
request = virtio.decode(b"".join(virtio.rpc.writes)[1:-1])
expect("parameter replaced by a marker",
       request["data"]["parameters"][0][oc2_blob.KEY], 0)
expect("message announces the payload length", request["blobs"][0]["length"], len(sent))
expect("message announces the payload checksum", request["blobs"][0]["checksum"], 2445345482)


def last_request():
    return virtio.decode(b"".join(virtio.rpc.writes)[1:-1])


script({"type": "result", "gen": 1, "data": True})
bus.invoke("dev", "write", bus.blob(b"a"), bus.blob(b"bc"))
request = last_request()
expect("several payloads written back to back", b"".join(virtio.blob.writes), b"abc")
expect("first marker", request["data"]["parameters"][0][oc2_blob.KEY], 0)
expect("second marker", request["data"]["parameters"][1][oc2_blob.KEY], 1)
expect("one description per payload", len(request["blobs"]), 2)
expect("second payload length", request["blobs"][1]["length"], 2)

script({"type": "result", "gen": 1, "data": True})
shared = bus.blob(b"a")
bus.invoke("dev", "write", shared, shared)
request = last_request()
expect("a payload passed twice is sent once", b"".join(virtio.blob.writes), b"a")
expect("both markers refer to it", request["data"]["parameters"][1][oc2_blob.KEY], 0)
expect("and it is described once", len(request["blobs"]), 1)

script({"type": "result", "gen": 1, "data": True})
nested = bus.blob(b"ab")
record = {"name": "x", "content": nested}
bus.invoke("dev", "write", record, [bus.blob(b"c"), nested])
request = last_request()
sent_record, sent_parts = request["data"]["parameters"]
expect("a nested payload is replaced by a marker", sent_record["content"][oc2_blob.KEY], 0)
expect("payloads in a nested list", sent_parts[0][oc2_blob.KEY], 1)
expect("a nested payload passed twice is sent once", sent_parts[1][oc2_blob.KEY], 0)
expect("other fields are kept", sent_record["name"], "x")
expect("nested payloads written in order", b"".join(virtio.blob.writes), b"abc")
expect("the caller's dict is not modified", record["content"] is nested, True)

script({"type": "result", "gen": 1, "data": True})
raises("nested raw bytes must be declared as a payload", "bus.blob",
       lambda: bus.invoke("dev", "write", {"data": b"raw"}))

# The outbound limit is enforced locally, before anything goes on the wire.
script({"type": "result", "gen": 1, "data": True})
raises("oversized payload refused locally", "exceeds the host limit",
       lambda: bus.invoke("dev", "write", bus.blob(b"x" * (oc2_blob.MAX_OUTBOUND + 1))))

raises("a non-binary payload is refused", "a binary payload must be",
       lambda: bus.blob(123))

# Lua has no separate binary type, so this one has no counterpart there: json.dumps would
# quietly encode raw bytes as a string instead of sending them on the payload channel.
script({"type": "result", "gen": 1, "data": True})
raises("raw bytes must be declared as a payload", "bus.blob",
       lambda: bus.invoke("dev", "write", b"raw"))

report()
