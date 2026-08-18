import virtio
from harness import expect, report

from oc2.channel import Channel

channel = Channel.open(virtio.rpc.path)

NUL = virtio.NUL
BODY = virtio.encode({"value": "hello"})
FRAME = NUL + BODY + NUL


def feed(parts):
    channel.reset()
    virtio.feed(virtio.rpc, parts)


def read():
    message = channel.read(1000)
    return message.get("value") if message else None


def check(name, parts):
    feed(parts)
    expect(name, read(), "hello")


check("single chunk, NUL-sandwiched", [FRAME])

check("no leading delimiter", [BODY + NUL])

check("terminator alone in next chunk", [NUL + BODY, NUL])

check("double leading delimiter", [NUL + NUL + BODY + NUL])

check("chunk of delimiters only, then body", [NUL * 3, BODY + NUL])

check("leading delimiter alone in its own chunk", [NUL, BODY + NUL])

check("body split across chunks", [NUL + BODY[:5], BODY[5:11], BODY[11:] + NUL])

# Joining a stream mid-frame can leave a tail that still decodes. The reader has to drop
# anything that is not an object, because every consumer above assumes it got one.
check("a frame that is not an object is discarded", [NUL + b"42" + NUL + FRAME])

check("a frame that does not decode is discarded", [NUL + b"bad json" + NUL + FRAME])

# Exhaustive: split the frame at every offset.
bad = 0
for i in range(1, len(FRAME)):
    feed([FRAME[:i], FRAME[i:]])
    if read() != "hello":
        bad += 1
expect("every 2-way split of the frame", bad, 0)

# Byte-at-a-time delivery.
feed([FRAME[i:i + 1] for i in range(len(FRAME))])
expect("one byte per read", read(), "hello")

# Two messages in one read: the second must come out of the buffer that is still held.
feed([NUL + virtio.encode({"value": "first"}) + NUL +
      NUL + virtio.encode({"value": "second"}) + NUL])
expect("first message in the chunk", read(), "first")
expect("second message from the held buffer", read(), "second")

# A read that times out returns nothing, and keeps the partial frame for the next one.
feed([NUL + BODY[:6]])
expect("a read with nothing queued times out", read(), None)
virtio.add(virtio.rpc, BODY[6:] + NUL)
expect("the partial frame resumes after the timeout", read(), "hello")

# Requests go out framed the same way.
channel.write({"type": "list"})
request = b"".join(virtio.rpc.writes)
expect("request starts with a delimiter", request[0], 0)
expect("request ends with a delimiter", request[-1], 0)
expect("request body is the message", virtio.decode(request[1:-1])["type"], "list")

report()
