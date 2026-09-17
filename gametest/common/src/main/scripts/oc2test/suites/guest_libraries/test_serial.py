from harness import expect, report

from oc2 import serial

ME = 3
PEER = 7

hello = serial.encode(ME, PEER, b"hello")

expect("a frame is header plus payload plus checksum", len(hello), 4 + 5 + 1)
expect("a frame starts with the marker", hello[0], 0x01)

frame, rest = serial.decode(hello, ME)
expect("the sender comes back", frame[0], PEER)
expect("the payload comes back", frame[1], b"hello")
expect("the whole frame is consumed", rest, b"")

frame, _ = serial.decode(serial.encode(serial.BROADCAST, PEER, b"all"), ME)
expect("a broadcast is for everyone", frame[1], b"all")

frame, _ = serial.decode(serial.encode(42, PEER, b"not yours"), ME)
expect("another station's frame is skipped", frame, None)

damaged = hello[:-1] + bytes(((hello[-1] + 1) % 256,))
frame, _ = serial.decode(damaged, ME)
expect("a frame that does not add up is dropped", frame, None)

frame, _ = serial.decode(b"garbage" + hello, ME)
expect("a receiver finds the marker again", frame[1], b"hello")

frame, _ = serial.decode(damaged + hello, ME)
expect("and keeps hunting past a damaged one", frame[1], b"hello")

frame, rest = serial.decode(hello[:4], ME)
expect("an incomplete frame is not a frame yet", frame, None)
expect("and its bytes are kept", rest, hello[:4])

frame, rest = serial.decode(hello + hello, ME)
expect("two frames in one read: the first comes out", frame[1], b"hello")
expect("and the second is left for the next call", len(rest), len(hello))

frame, _ = serial.decode(bytes((1, 0, 0, 150)) + hello, ME)
expect("a stray marker in noise does not hold back the frame behind it", frame and frame[1], b"hello")

frame, _ = serial.decode(bytes((1, 0, 0, 250)) + hello, ME)
expect("nor does one claiming more than a frame holds", frame and frame[1], b"hello")

frame, rest = serial.decode(bytes((1, 0, 0, 150)) + hello[:6], ME)
expect("with nothing complete behind it, the marker waits", frame, None)
expect("keeping everything from the marker on", rest, bytes((1, 0, 0, 150)) + hello[:6])

expect("the payload limit is what the manual says", serial.MAX_PAYLOAD, 200)

report()
