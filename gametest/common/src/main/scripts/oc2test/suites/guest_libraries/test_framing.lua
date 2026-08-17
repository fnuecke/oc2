local virtio = require("virtio")
local harness = require("harness")
local expect, report = harness.expect, harness.report

local Channel = require("oc2.channel")
local channel = assert(Channel.open(virtio.rpc.path))

local NUL = virtio.NUL
local BODY = virtio.encode({ value = "hello" })
local FRAME = NUL .. BODY .. NUL

local function feed(parts)
  channel:reset()
  virtio.feed(virtio.rpc, parts)
end

local function read()
  local message = channel:read(1000)
  return message and message.value
end

local function check(name, parts)
  feed(parts)
  expect(name, read(), "hello")
end

check("single chunk, NUL-sandwiched", { FRAME })

check("no leading delimiter", { BODY .. NUL })

check("terminator alone in next chunk", { NUL .. BODY, NUL })

check("double leading delimiter", { NUL .. NUL .. BODY .. NUL })

check("chunk of delimiters only, then body", { NUL .. NUL .. NUL, BODY .. NUL })

check("leading delimiter alone in its own chunk", { NUL, BODY .. NUL })

check("body split across chunks",
      { NUL .. BODY:sub(1, 5), BODY:sub(6, 11), BODY:sub(12) .. NUL })

-- Joining a stream mid-frame can leave a tail that still decodes. The reader has to drop
-- anything that is not an object, because every consumer above assumes it got one.
check("a frame that is not an object is discarded", { NUL .. "42" .. NUL .. FRAME })

check("a frame that does not decode is discarded", { NUL .. "bad json" .. NUL .. FRAME })

-- Exhaustive: split the frame at every offset.
local bad = 0
for i = 1, #FRAME - 1 do
  feed({ FRAME:sub(1, i), FRAME:sub(i + 1) })
  if read() ~= "hello" then bad = bad + 1 end
end
expect("every 2-way split of the frame", bad, 0)

-- Byte-at-a-time delivery.
local bytes = {}
for i = 1, #FRAME do bytes[i] = FRAME:sub(i, i) end
feed(bytes)
expect("one byte per read", read(), "hello")

-- Two messages in one read: the second must come out of the buffer that is still held.
feed({ NUL .. virtio.encode({ value = "first" }) .. NUL ..
       NUL .. virtio.encode({ value = "second" }) .. NUL })
expect("first message in the chunk", read(), "first")
expect("second message from the held buffer", read(), "second")

-- A read that times out returns nothing, and keeps the partial frame for the next one.
feed({ NUL .. BODY:sub(1, 6) })
expect("a read with nothing queued times out", read(), nil)
virtio.add(virtio.rpc, BODY:sub(7) .. NUL)
expect("the partial frame resumes after the timeout", read(), "hello")

-- Requests go out framed the same way.
channel:write({ type = "list" })
local request = table.concat(virtio.rpc.writes)
expect("request starts with a delimiter", request:byte(1), 0)
expect("request ends with a delimiter", request:byte(-1), 0)
expect("request body is the message", virtio.decode(request:sub(2, -2)).type, "list")

report()
