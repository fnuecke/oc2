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

# The checksums are the same literals pinned on the Java side, computed independently of
# both implementations. If these drift, every payload looks corrupt.
PAYLOAD = b"the quick brown fox"


def invoke_with_blob(payload, checksum, chunks=None):
    script({"type": "result", "gen": 1, "data": {oc2_blob.KEY: True},
            "blob": {"length": len(payload), "checksum": checksum}},
           [payload] if chunks is None else chunks)
    return bus.invoke("dev", "readBlob")


expect("payload arrives from the blob port", invoke_with_blob(PAYLOAD, 746590911), PAYLOAD)
expect("empty payload", invoke_with_blob(b"", 0), b"")
expect("two byte payload", invoke_with_blob(b"\1\2", 513), b"\1\2")
expect("reordering is detected as different", invoke_with_blob(b"\2\1", 258), b"\2\1")

# The host folds the checksum to signed 32 bits, so the guest has to as well. Without the
# fold these two come back as 4294967295 and never match what the host announced.
expect("all ones folds to a negative checksum",
       invoke_with_blob(b"\xff\xff\xff\xff", -1), b"\xff\xff\xff\xff")
expect("and so does a zero-padded tail",
       invoke_with_blob(b"\xff\xff\xff\xff\x00", -1), b"\xff\xff\xff\xff\x00")
expect("the high bit alone is the most negative checksum",
       invoke_with_blob(b"\x00\x00\x00\x80", -2147483648), b"\x00\x00\x00\x80")

expect("payload split across reads",
       invoke_with_blob(PAYLOAD, 746590911,
                        [PAYLOAD[:5], PAYLOAD[5:11], PAYLOAD[11:]]), PAYLOAD)

raises("corrupt payload is rejected", "checksum",
       lambda: invoke_with_blob(PAYLOAD, 746590911, [b"the quick brown foY"]))

# A marker in the reply body with no announced payload must not go looking for one.
script({"type": "result", "gen": 1,
        "data": {"display": {oc2_blob.KEY: {"length": 100000, "checksum": 0}}}})
expect("forged reference does not hang",
       isinstance(bus.invoke("dev", "getStackInSlot"), dict), True)

script({"type": "result", "gen": 1, "data": {oc2_blob.KEY: True},
        "blob": {"length": 1 << 40, "checksum": 0}})
raises("implausible payload size is refused", "implausible",
       lambda: bus.invoke("dev", "readBlob"))

# A payload the host never sends must time out rather than block forever, and the channel
# has to be reset afterwards or every later payload reads someone else's bytes.
raises("a missing payload times out", "could not read binary payload",
       lambda: invoke_with_blob(PAYLOAD, 746590911, []))
expect("the payload channel recovered", invoke_with_blob(PAYLOAD, 746590911), PAYLOAD)

# Outbound: the payload goes on the blob port, the message says how big it is, and the
# parameter is replaced by a marker.
script({"type": "result", "gen": 1, "data": True})
sent = b"the quick brown fox"
bus.invoke("dev", "writeExportFile", bus.blob(sent))
expect("payload written to the blob port", b"".join(virtio.blob.writes), sent)
request = virtio.decode(b"".join(virtio.rpc.writes)[1:-1])
expect("parameter replaced by a marker",
       request["data"]["parameters"][0][oc2_blob.KEY], True)
expect("message announces the payload length", request["blob"]["length"], len(sent))
expect("message announces the payload checksum", request["blob"]["checksum"], 746590911)

script({"type": "result", "gen": 1, "data": True})
raises("two payloads in one call refused", "at most one",
       lambda: bus.invoke("dev", "write", bus.blob(b"a"), bus.blob(b"b")))

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
