-- The real client against a real daemon, over real sockets.

local harness = require("harness")
local expect, raises, report = harness.expect, harness.raises, harness.report

local bus = require("oc2.bus")
local busd = require("oc2.busd")
local blob = require("oc2.blob")
local Channel = require("oc2.channel")
local socket = require("oc2.socket")
local sys = require("posix.sys.socket")
local signal = require("posix.signal")
local unistd = require("posix.unistd")
local wait = require("posix.sys.wait")

local PATH = "/tmp/oc2test-bus"

local function socketPair()
  local a, b = sys.socketpair(sys.AF_UNIX, sys.SOCK_STREAM, 0)
  assert(a and b, "socketpair failed")
  assert(socket.setNonBlocking(a))
  assert(socket.setNonBlocking(b))
  return a, b
end

local rpcHost, rpcGuest = socketPair()
local blobHost, blobGuest = socketPair()
local eventHost, eventGuest = socketPair()
local listenFd = assert(socket.listen(PATH))

local DEVICES = {
  { deviceId = "redstone-1", typeNames = { "redstone" } },
  { deviceId = "robot-1", typeNames = { "robot" } },
}
local METHODS = {
  { name = "getRedstoneOutput", parameters = {} },
  { name = "setRedstoneOutput", parameters = { { name = "side" }, { name = "value" } } },
  { name = "documented", returnType = "number",
    description = "Does a documented thing.",
    returnValueDescription = "the documented result.",
    parameters = { { name = "count", type = "number", description = "how many." } } },
  { name = "echoBlob", parameters = {} },
}

local child = assert(unistd.fork())
if child == 0 then
  -- Daemon and host in one loop: the daemon gets a step, then anything it forwarded is
  -- answered as the host would answer it.
  local daemon = busd.new({
    rpcFd = rpcGuest, blobFd = blobGuest, eventFd = eventGuest, listenFd = listenFd,
  })
  local host = Channel.fromFd(rpcHost)
  local hostPayload = blob.fromFd(blobHost)
  local hostEvents = Channel.fromFd(eventHost)
  local generation = 1
  -- Stands in for primeGeneration, which cannot run here: it blocks on a host reply, and the
  -- host is this same loop.
  daemon.generation = generation
  local deadline = os.time() + 30

  while os.time() < deadline do
    daemon:step(5)

    local message = host:read(0)
    if message then
      if message.type == "list" then
        host:write({ type = "list", gen = generation, data = DEVICES })
      elseif message.type == "methods" then
        host:write({ type = "methods", gen = generation, data = METHODS })
      elseif message.type == "invoke" then
        local name = message.data and message.data.name
        if name == "echoBlob" then
          local sent = hostPayload:read(message.blob.length, 2000) or ""
          hostPayload:write(sent)
          host:write({ type = "result", gen = generation, data = { [blob.key] = true },
                       blob = { length = #sent, checksum = blob.checksum(sent) } })
        elseif name == "explode" then
          host:write({ type = "error", gen = generation, data = "the host said no" })
        elseif name == "bumpGeneration" then
          generation = generation + 1
          hostEvents:write({ type = "devicesChanged", gen = generation })
          host:write({ type = "result", gen = generation, data = true })
        else
          host:write({ type = "result", gen = generation, data = 15 })
        end
      else
        host:write({ type = "error", gen = generation, data = "unsupported" })
      end
    end
  end
  os.exit(0)
end

-- Parent: an ordinary client. It must drop every descriptor the child now owns -- keeping a
-- dup of the listening socket in particular would make a connect after the child is gone
-- succeed into a backlog nobody serves, quietly disabling the fallback this file checks.
for _, fd in ipairs({ rpcHost, rpcGuest, blobHost, blobGuest, eventHost, eventGuest, listenFd }) do
  socket.close(fd)
end

local function stop()
  signal.kill(child, signal.SIGKILL)
  wait.wait(child)
end

local first, reason = bus.connect({ socketPath = PATH })
expect("the client reached the daemon", first ~= nil, true)
if not first then
  print("connect failed: " .. tostring(reason))
  stop()
  report()
end

expect("it chose the socket transport", first.transport, "socket")

-- The documentation the host sends must all survive the formatter. The return value
-- description in particular is easy to drop silently, since nothing else reads that key.
local doc = tostring(first:get("redstone-1"))
expect("the docs name the method", doc:find("documented(", 1, true) ~= nil, true)
expect("and carry its description", doc:find("Does a documented thing.", 1, true) ~= nil, true)
expect("and its parameter description", doc:find("count  how many.", 1, true) ~= nil, true)
expect("and its return value description",
       doc:find("returns  the documented result.", 1, true) ~= nil, true)
expect("and learned the generation from the handshake", first.generation, 1)

local devices = first:list()
expect("list came back through the daemon", #devices, 2)
expect("with the host's devices", devices[1].deviceId, "redstone-1")

-- Over a socket the daemon holds the one list for the whole machine, so a session that keeps a
-- copy of its own would be the only thing able to go stale.
expect("and the session kept no list of its own", first.deviceList, nil)
expect("asking again still answers", #first:list(), 2)

local redstone = first:find("redstone")
expect("find located a device by type", redstone and redstone.deviceId, "redstone-1")
expect("methods resolve through the daemon", type(redstone.getRedstoneOutput), "function")
expect("and invoking one returns the host's answer", redstone:getRedstoneOutput(), 15)

-- Device.__index resolves through the cached method list, so repeated lookups on one device
-- must not go back to the host. Counting the fetches is the only way to see the cache work.
local realMethods = first.methods
local methodListFetches = 0
first.methods = function(self, deviceId)
  methodListFetches = methodListFetches + 1
  return realMethods(self, deviceId)
end

local cached = first:find("redstone")
local _ = cached.getRedstoneOutput
local _ = cached.setRedstoneOutput
local _ = cached.getRedstoneOutput
expect("the method list is fetched once per device", methodListFetches, 1)

-- ...but a freshly resolved device starts with a cold cache of its own.
local _ = first:find("redstone").getRedstoneOutput
expect("and again for a device resolved anew", methodListFetches, 2)

first.methods = nil

-- Errors from the host arrive as errors, not as some transport failure.
raises("a host error surfaces as an error", "the host said no",
       function() return first:invoke("redstone-1", "explode") end)

-- Payloads, both directions, over the session's own blob socket.
local payload = "the quick brown fox"
expect("a payload round-trips through the daemon",
       first:invoke("redstone-1", "echoBlob", first:blob(payload)), payload)

-- A second, independent session from the same process.
local second = assert(bus.connect({ socketPath = PATH }))
expect("a second session opens alongside the first", second.transport, "socket")
expect("and works independently", second:find("robot") ~= nil, true)
expect("the first session is unaffected", first:find("redstone") ~= nil, true)

-- Events are broadcast: both sessions see the same devicesChanged.
first:invoke("redstone-1", "bumpGeneration")
local firstEvent = first:waitEvent(2000, "devicesChanged")
local secondEvent = second:waitEvent(2000, "devicesChanged")
expect("the invoking session sees the event", firstEvent and firstEvent.type, "devicesChanged")
expect("so does the other one", secondEvent and secondEvent.type, "devicesChanged")
expect("the list still resolves after the change", #first:list(), 2)

-- A session with no event socket still works; it simply never hears about changes.
local quiet = assert(bus.connect({ socketPath = PATH, roles = { "rpc" } }))
expect("an rpc-only session works", #quiet:list(), 2)

-- A session that declined a role must say so rather than indexing a channel it never opened.
raises("a payload needs a payload channel", "no payload channel",
       function() return quiet:invoke("redstone-1", "echoBlob", quiet:blob("x")) end)
local waited, waitReason = quiet:waitEvent(10)
expect("waiting on a session with no event channel returns nothing", waited, nil)
expect("and says why", waitReason, "this session has no event channel")
expect("pumping one is simply a no-op", quiet:pumpEvents(), 0)

-- flush() is deliberately inert on a socket: the session is nobody else's, and resetting would
-- throw away a reply the daemon has already written.
first:flush()
expect("flush does not disturb a socket session", first:find("redstone") ~= nil, true)

-- Every failed connect has to give its descriptors back, or a script that retries in a loop
-- runs the machine out of them.
local function openDescriptors()
  local total = 0
  for _ in pairs(require("posix.dirent").dir("/proc/self/fd")) do
    total = total + 1
  end
  return total
end

local savedAttempts = bus.connectAttempts
bus.connectAttempts = 1 -- no point sitting through the startup-race retries here

local beforeFailures = openDescriptors()
for _ = 1, 5 do
  bus.connect({ socketPath = "/tmp/oc2test-bus-nothing-here" })
end
expect("a failed connect leaks no descriptors", openDescriptors(), beforeFailures)
bus.connectAttempts = savedAttempts

first:close()
second:close()
quiet:close()

-- With no daemon reachable and no ports either, the failure names both attempts rather than
-- silently reporting only the last one. The ports are stubbed out rather than left to whether
-- something else happens to be holding them -- and so that a failure here cannot take the
-- machine-wide ports for itself midway through a suite.
package.loaded["oc2.ports"] = { find = function() return nil end }
bus.connectAttempts = 1

local absent, absentReason = bus.connect({ socketPath = "/tmp/oc2test-bus-absent" })
expect("connecting with nothing available fails", absent, nil)
expect("and explains both transports",
       (absentReason or ""):find("nor the ports", 1, true) ~= nil, true)

stop()
os.remove(PATH)
report()
