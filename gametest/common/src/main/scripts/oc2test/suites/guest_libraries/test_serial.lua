local harness = require("harness")
local expect, report = harness.expect, harness.report

local serial = require("oc2.serial")

local ME = 3
local PEER = 7

local function decode(buffer)
  local frame, rest = serial.decode(buffer, ME)
  return frame, rest
end

local hello = serial.encode(ME, PEER, "hello")

expect("a frame is header plus payload plus checksum", #hello, 4 + 5 + 1)
expect("a frame starts with the marker", hello:byte(1), 0x01)

local frame, rest = decode(hello)
expect("the sender comes back", frame and frame.from, PEER)
expect("the payload comes back", frame and frame.data, "hello")
expect("the whole frame is consumed", rest, "")

frame = decode(serial.encode(serial.BROADCAST, PEER, "all"))
expect("a broadcast is for everyone", frame and frame.data, "all")

frame = decode(serial.encode(42, PEER, "not yours"))
expect("another endpoint's frame is skipped", frame, nil)

local damaged = serial.encode(ME, PEER, "hello")
damaged = damaged:sub(1, #damaged - 1) .. string.char((damaged:byte(#damaged) + 1) % 256)
frame = decode(damaged)
expect("a frame that does not add up is dropped", frame, nil)

frame = decode("garbage" .. hello)
expect("a receiver finds the marker again", frame and frame.data, "hello")

frame = decode(damaged .. hello)
expect("and keeps hunting past a damaged one", frame and frame.data, "hello")

frame, rest = decode(hello:sub(1, 4))
expect("an incomplete frame is not a frame yet", frame, nil)
expect("and its bytes are kept", rest, hello:sub(1, 4))

frame, rest = decode(hello .. hello)
expect("two frames in one read: the first comes out", frame and frame.data, "hello")
expect("and the second is left for the next call", #rest, #hello)

frame = decode("\1\0\0\150" .. hello)
expect("a stray marker in noise does not hold back the frame behind it", frame and frame.data, "hello")

frame = decode("\1\0\0\250" .. hello)
expect("nor does one claiming more than a frame holds", frame and frame.data, "hello")

frame, rest = decode("\1\0\0\150" .. hello:sub(1, 6))
expect("with nothing complete behind it, the marker waits", frame, nil)
expect("keeping everything from the marker on", rest, "\1\0\0\150" .. hello:sub(1, 6))

expect("the payload limit is what the manual says", serial.MAX_PAYLOAD, 200)

report()
