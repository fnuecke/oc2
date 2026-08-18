local harness = require("harness")
local expect, report = harness.expect, harness.report

local busd = require("oc2.busd")
local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local socket = require("oc2.socket")
local sys = require("posix.sys.socket")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local time = require("posix.time")
local signal = require("posix.signal")
local wait = require("posix.sys.wait")
local stat = require("posix.sys.stat")

local PATH = "/tmp/oc2test-busd"

local function socketPair()
  local a, b = sys.socketpair(sys.AF_UNIX, sys.SOCK_STREAM, 0)
  assert(a and b, "socketpair failed")
  assert(socket.setNonBlocking(a))
  assert(socket.setNonBlocking(b))
  return a, b
end

local function sleep(milliseconds)
  time.nanosleep({ tv_sec = 0, tv_nsec = milliseconds * 1000 * 1000 })
end

-- Host side of each port, from the daemon's point of view.
local rpcHost, rpcGuest = socketPair()
local blobHost, blobGuest = socketPair()
local eventHost, eventGuest = socketPair()

local listenFd = assert(socket.listen(PATH))
local daemon = busd.new({
  rpcFd = rpcGuest, blobFd = blobGuest, eventFd = eventGuest, listenFd = listenFd,
})

local host = Channel.fromFd(rpcHost)
local hostEvents = Channel.fromFd(eventHost)

--- The daemon has no thread of its own here, so anything that should make it act is followed
--- by a few non-blocking passes.
local function pump(times)
  for _ = 1, times or 6 do
    daemon:step(5)
  end
end

--- Parse a reply without letting a missing field crash the file and hide every other test.
local function field(message, ...)
  local value = message
  for _, key in ipairs({ ... }) do
    if type(value) ~= "table" then
      return nil
    end
    value = value[key]
  end
  return value
end

--- Reports whether the daemon closed a descriptor, without letting a timing miss abort the
--- whole file the way a bare assert on a non-blocking read would.
local function expectClosed(name, fd)
  local closed = false
  for _ = 1, 20 do
    pump(2)
    if poll.rpoll(fd, 5) == 1 then
      local chunk = unistd.read(fd, 64)
      if chunk and #chunk == 0 then
        closed = true
        break
      end
    end
  end
  expect(name, closed, true)
end

-- Client, in miniature

local Client = {}
Client.__index = Client

local function connect(roles)
  local self = setmetatable({ roles = roles or { "rpc", "blob", "event" } }, Client)

  self.rpcFd = assert(socket.connect(PATH))
  Channel.writeAll(self.rpcFd, "R")
  self.rpc = Channel.fromFd(self.rpcFd)
  self.rpc:write({ type = "hello", version = 1, roles = self.roles })
  pump()

  self.hello = self.rpc:read(500)
  if not self.hello or not self.hello.token then
    return self
  end

  for _, role in ipairs(self.roles) do
    if role == "blob" then
      self.blobFd = assert(socket.connect(PATH))
      Channel.writeAll(self.blobFd, "B" .. self.hello.token)
    elseif role == "event" then
      self.eventFd = assert(socket.connect(PATH))
      Channel.writeAll(self.eventFd, "E" .. self.hello.token)
      self.events = Channel.fromFd(self.eventFd)
    end
  end
  pump()
  return self
end

function Client:close()
  -- Explicitly, not with ipairs over a table with holes: a session that declined a role would
  -- otherwise leak the descriptors after the first nil.
  socket.close(self.rpcFd)
  socket.close(self.blobFd)
  socket.close(self.eventFd)
end

-- Handshaking

local first = connect()
expect("hello is answered", first.hello and first.hello.type, "hello")
expect("with a session token of the agreed length", #(first.hello.token or ""), 32)

local second = connect()
expect("a second session gets its own token",
       second.hello.token ~= first.hello.token, true)

-- A token is spent once every declared role has attached, so a replay finds nothing.
local replay = assert(socket.connect(PATH))
Channel.writeAll(replay, "E" .. first.hello.token)
expectClosed("a spent token cannot attach a fourth socket", replay)
socket.close(replay)

local bogus = assert(socket.connect(PATH))
Channel.writeAll(bogus, "X")
expectClosed("an unknown role code is refused", bogus)
socket.close(bogus)

local oldVersion = assert(socket.connect(PATH))
Channel.writeAll(oldVersion, "R")
Channel.fromFd(oldVersion):write({ type = "hello", version = 99 })
pump()
local refusal = Channel.fromFd(oldVersion):read(200)
expect("an unsupported version is refused", refusal and refusal.type, "error")
socket.close(oldVersion)

local badRole = assert(socket.connect(PATH))
Channel.writeAll(badRole, "R")
Channel.fromFd(badRole):write({ type = "hello", version = 1, roles = { "rpc", "telepathy" } })
pump()
local roleRefusal = Channel.fromFd(badRole):read(200)
expect("an unknown role is refused", roleRefusal and roleRefusal.type, "error")
socket.close(badRole)

-- Requests

--- Answers whatever the daemon forwarded, as the host would.
local function hostAnswers(reply)
  local request = host:read(500)
  if reply then
    host:write(reply)
  end
  pump()
  return request
end

first.rpc:write({ type = "list" })
pump()
local seen = hostAnswers({ type = "list", gen = 3,
                          data = { { deviceId = "aaa" }, { deviceId = "first" },
                                   { deviceId = "second" } } })
expect("the request reached the host", seen and seen.type, "list")
local answer = first.rpc:read(500)
expect("and the reply came back", answer and answer.type, "list")
expect("carrying the host's data", field(answer, "data", 1, "deviceId"), "aaa")

-- Two clients, one host. The daemon must serialise them and keep the replies apart.
first.rpc:write({ type = "methods", data = "first" })
second.rpc:write({ type = "methods", data = "second" })
pump()

local firstSeen = host:read(500)
expect("only one request is in flight at a time", firstSeen and firstSeen.data, "first")
expect("the second is still queued", host:read(50), nil)

host:write({ type = "methods", gen = 3, data = { "for-first" } })
pump()
local secondSeen = host:read(500)
expect("the queued request goes out once the first is answered",
       secondSeen and secondSeen.data, "second")
host:write({ type = "methods", gen = 3, data = { "for-second" } })
pump()

local firstReply = first.rpc:read(500)
local secondReply = second.rpc:read(500)
expect("the first client got its own reply", firstReply and firstReply.data[1], "for-first")
expect("the second client got its own reply", secondReply and secondReply.data[1], "for-second")

-- Device cache

-- The point of the whole thing: the host answered one list, and every session after that is
-- served without it hearing about them again.
first.rpc:write({ type = "list" })
pump()
expect("a repeated list never reaches the host", host:read(50), nil)
local cached = first.rpc:read(500)
expect("it is answered anyway", cached and cached.type, "list")
expect("with what the host gave", field(cached, "data", 1, "deviceId"), "aaa")
expect("stamped with the current generation", field(cached, "gen"), 3)

second.rpc:write({ type = "list" })
pump()
expect("a different session is served too",
       field(second.rpc:read(500), "data", 1, "deviceId"), "aaa")
expect("still without the host", host:read(50), nil)

first.rpc:write({ type = "methods", data = "first" })
pump()
expect("methods are cached per device", field(first.rpc:read(500), "data", 1), "for-first")
expect("also without the host", host:read(50), nil)

first.rpc:write({ type = "methods", data = "third" })
pump()
expect("a device nobody has asked about still goes out",
       field(hostAnswers({ type = "methods", gen = 3, data = { "for-third" } }), "data"), "third")
first.rpc:read(500)

-- A cached answer must not overtake a reply the client is still waiting for, so it is only
-- served to a session with nothing outstanding.
first.rpc:write({ type = "invoke", data = { deviceId = "aaa", name = "slow" } })
pump()
expect("a slow request goes to the host", field(host:read(500), "type"), "invoke")
first.rpc:write({ type = "list" })
pump()
expect("a list queued behind it does not jump ahead", first.rpc:read(50), nil)
host:write({ type = "result", gen = 3, data = true })
pump()
expect("the earlier reply comes first", field(first.rpc:read(500), "type"), "result")
expect("and only then does the list go out",
       field(hostAnswers({ type = "list", gen = 3, data = { { deviceId = "aaa" } } }), "type"), "list")
first.rpc:read(500)

-- An id the host never listed is never filed. The host resolves ids leniently, so a client that
-- spells one device many ways would otherwise be able to mint cache entries at will.
first.rpc:write({ type = "methods", data = "third" })
pump()
expect("a device the host never listed is asked about every time",
       field(hostAnswers({ type = "methods", gen = 3, data = { "for-third" } }), "data"), "third")
first.rpc:read(500)

-- A new generation means the bus changed, so everything learned about the old one is wrong.
hostEvents:write({ type = "devicesChanged", gen = 7 })
pump()
first.events:read(500)
second.events:read(500)

first.rpc:write({ type = "list" })
pump()
expect("a changed bus sends the next list to the host again",
       field(hostAnswers({ type = "list", gen = 7, data = { { deviceId = "bbb" } } }), "type"), "list")
expect("and the new answer is the one served",
       field(first.rpc:read(500), "data", 1, "deviceId"), "bbb")

first.rpc:write({ type = "methods", data = "first" })
pump()
expect("methods learned before the change went with it",
       field(hostAnswers({ type = "methods", gen = 7, data = { "fresh" } }), "data"), "first")
first.rpc:read(500)

-- A reply that was already on its way when the bus changed carries the older generation. Taking
-- it would pin the cache to a bus that is gone, with the event that would have corrected it
-- already spent.
hostEvents:write({ type = "devicesChanged", gen = 8 })
pump()
first.events:read(500)
second.events:read(500)

first.rpc:write({ type = "list" })
pump()
expect("a list after the change goes to the host",
       field(hostAnswers({ type = "list", gen = 5, data = { { deviceId = "stale" } } }), "type"),
       "list")
expect("the stale answer still reaches the client that asked",
       field(first.rpc:read(500), "data", 1, "deviceId"), "stale")
expect("but the generation did not go backwards", daemon.generation, 8)

first.rpc:write({ type = "list" })
pump()
expect("and nothing was kept from it",
       field(hostAnswers({ type = "list", gen = 8, data = { { deviceId = "ccc" } } }), "type"), "list")
expect("so the current bus is what gets served",
       field(first.rpc:read(500), "data", 1, "deviceId"), "ccc")

-- A request that announces a payload has to go the long way whatever the cache says: its bytes
-- are already on the wire, and would otherwise be charged to whatever request came next.
Channel.writeAll(first.blobFd, "junk")
pump()
first.rpc:write({ type = "list", blob = { length = 4, checksum = 0 } })
pump()
expect("a request carrying a payload is never answered from the cache",
       field(hostAnswers({ type = "list", gen = 8, data = { { deviceId = "ccc" } } }), "type"), "list")
expect("and its bytes went to the host rather than onto the next request",
       Channel.readExactly(blobHost, 4, 500), "junk")
first.rpc:read(500)

-- Refill, so there is something to invalidate below.
hostEvents:write({ type = "devicesChanged", gen = 9 })
pump()
first.events:read(500)
second.events:read(500)
first.rpc:write({ type = "list" })
pump()
hostAnswers({ type = "list", gen = 9, data = { { deviceId = "ddd" } } })
first.rpc:read(500)

first.rpc:write({ type = "list" })
pump()
expect("a cache the events agree with answers on its own", host:read(50), nil)
first.rpc:read(500)

-- Every event carries the generation that was current when the host queued it, not only
-- `devicesChanged`, so any of them can be the one that reveals a change was missed.
hostEvents:write({ type = "robotActionCompleted", gen = 10, data = { actionId = 1 } })
pump()
first.events:read(500)
second.events:read(500)

first.rpc:write({ type = "list" })
pump()
expect("a generation seen on any event invalidates the cache",
       field(hostAnswers({ type = "list", gen = 10, data = { { deviceId = "eee" } } }), "type"),
       "list")
first.rpc:read(500)

-- And when the host has to drop events, it says so, which is what makes a missed
-- `devicesChanged` recoverable without the daemon distrusting its cache on a timer.
hostEvents:write({ type = "eventsDropped", gen = 11, data = 3 })
pump()
expect("the notice reaches subscribers as well",
       field(first.events:read(500), "type"), "eventsDropped")
second.events:read(500)

first.rpc:write({ type = "list" })
pump()
expect("and the daemon goes back to the host after one",
       field(hostAnswers({ type = "list", gen = 11, data = { { deviceId = "fff" } } }), "type"),
       "list")
first.rpc:read(500)

-- Blobs

-- A client writes its payload before the message that claims it, exactly as the bus does, so
-- the daemon has to hold bytes that arrive ahead of their request.
local payload = "the quick brown fox"
Channel.writeAll(first.blobFd, payload)
pump()
first.rpc:write({ type = "invoke", data = { deviceId = "aaa", name = "write" },
                  blob = { length = #payload, checksum = 0 } })
pump()

local invoked = host:read(500)
expect("the invoke reached the host", invoked and invoked.type, "invoke")
expect("and its payload went out on the blob port",
       Channel.readExactly(blobHost, #payload, 500), payload)

local reply = "reply payload"
Channel.writeAll(blobHost, reply)
host:write({ type = "result", gen = 3, data = true, blob = { length = #reply, checksum = 0 } })
pump()
local blobReply = first.rpc:read(500)
expect("the reply frame announces its payload", blobReply and blobReply.blob.length, #reply)
expect("a reply payload reaches the client",
       Channel.readExactly(first.blobFd, #reply, 500), reply)

-- Events

hostEvents:write({ type = "devicesChanged", gen = 12 })
pump()
expect("an event reaches the first subscriber",
       (first.events:read(500) or {}).type, "devicesChanged")
expect("and the second: events are broadcast, not consumed",
       (second.events:read(500) or {}).type, "devicesChanged")

local quiet = connect({ "rpc" })
expect("a rpc-only session establishes", quiet.hello and quiet.hello.type, "hello")
quiet.rpc:write({ type = "list" })
pump()
expect("and can still make requests",
       (hostAnswers({ type = "list", gen = 12, data = {} }) or {}).type, "list")
expect("its reply arrives", (quiet.rpc:read(500) or {}).type, "list")

-- Lifecycle and limits

-- A request may arrive before the blob and event sockets have attached: the client gets no
-- attach acknowledgement, so it is entitled to send one immediately. It must wait, not fail.
local eager = setmetatable({ roles = { "rpc", "blob", "event" } }, Client)
eager.rpcFd = assert(socket.connect(PATH))
Channel.writeAll(eager.rpcFd, "R")
eager.rpc = Channel.fromFd(eager.rpcFd)
eager.rpc:write({ type = "hello", version = 1, roles = eager.roles })
pump()
eager.hello = eager.rpc:read(500)
eager.rpc:write({ type = "list" })
pump()
expect("a request before the session is complete is not rejected", host:read(50), nil)
expect("and no error was sent back", eager.rpc:read(50), nil)

eager.blobFd = assert(socket.connect(PATH))
Channel.writeAll(eager.blobFd, "B" .. eager.hello.token)
eager.eventFd = assert(socket.connect(PATH))
Channel.writeAll(eager.eventFd, "E" .. eager.hello.token)
pump()
expect("it goes out as soon as the session completes", (host:read(500) or {}).type, "list")
host:write({ type = "list", gen = 12, data = {} })
pump()
expect("and is answered", (eager.rpc:read(500) or {}).type, "list")
eager:close()

-- Closing any one socket of a session ends the whole session.
local doomed = connect()
socket.close(doomed.eventFd)
doomed.eventFd = nil
expectClosed("closing one socket tears down the session", doomed.rpcFd)
doomed:close()

-- Half-closing only the rpc socket must reap the session too. This is the shape that used to
-- leave an fd polling readable forever, spinning the daemon at full tilt.
local function countConnections()
  local total = 0
  for _ in pairs(daemon.connections) do
    total = total + 1
  end
  return total
end

local before = countConnections()
local halfClosed = connect({ "rpc" })
expect("the half-closed session started out attached", countConnections(), before + 1)
socket.close(halfClosed.rpcFd)
halfClosed.rpcFd = nil
pump(10)
expect("a half-closed session is reaped, not spun on", countConnections(), before)

-- A connection that says nothing is reaped rather than held forever.
busd.handshakeTimeout = 50
local silent = assert(socket.connect(PATH))
pump()
sleep(80)
expectClosed("a silent connection is reaped", silent)
socket.close(silent)
busd.handshakeTimeout = 5000

-- Misbehaving clients

-- The point of each of these is not that the offender is refused. It is that everyone else
-- keeps working afterwards.
--- `list` and `methods` are answered from the daemon's cache, so a test that wants to watch a
--- request make the round trip has to ask for something the daemon cannot know.
local function uncached()
  return { type = "invoke", data = { deviceId = "aaa", name = "ping" } }
end

local function stillWorks(name)
  first.rpc:write(uncached())
  pump()
  local request = hostAnswers({ type = "result", gen = 13, data = true })
  expect(name, request and request.type, "invoke")
  first.rpc:read(500)
end

-- A stream with no delimiter in it would, uncapped, buffer without limit and keep the read
-- loop fed for as long as the client cared to write.
local flood = assert(socket.connect(PATH))
Channel.writeAll(flood, "R")
for _ = 1, 40 do
  Channel.writeAll(flood, string.rep("x", 1024))
end
pump(10)
expectClosed("a frame with no end is cut off", flood)
socket.close(flood)
stillWorks("the bus survives a client that never delimits")

-- Two requests on one session, when only one may be outstanding.
local pushy = connect()
pushy.rpc:write(uncached())
pushy.rpc:write(uncached())
pump()
local pushyReply = pushy.rpc:read(500)
expect("a second concurrent request is an error", pushyReply and pushyReply.type, "error")
pushy:close()
host:read(50)
stillWorks("the bus survives a client that pipelines")

-- A payload that is announced and never sent. The timeout is shortened only after the session
-- is up: connecting takes three round trips through a poll loop, which under emulation is
-- comfortably longer than the deadline being tested here.
local truant = connect()
busd.sessionTimeout = 50
truant.rpc:write({ type = "invoke", data = { deviceId = "aaa", name = "write" },
                   blob = { length = 4096, checksum = 0 } })
pump()
sleep(80)
expectClosed("a payload that never arrives reaps the session", truant.rpcFd)
truant:close()
busd.sessionTimeout = 10000
stillWorks("the bus survives an abandoned payload")

-- More payload bytes than were announced would otherwise be prepended to the next request.
local liar = connect()
Channel.writeAll(liar.blobFd, string.rep("z", 64))
liar.rpc:write({ type = "invoke", data = { deviceId = "aaa", name = "write" },
                 blob = { length = 8, checksum = 0 } })
pump()
local liarReply = liar.rpc:read(500)
expect("over-sending its payload is an error", liarReply and liarReply.type, "error")
liar:close()
stillWorks("the bus survives an over-sent payload")

-- Host misbehaviour

busd.requestTimeout = 50
first.rpc:write({ type = "list" })
pump()
host:read(500) -- taken, never answered
sleep(80)
pump()
local timedOut = first.rpc:read(500)
expect("an unanswered request becomes an error reply", timedOut and timedOut.type, "error")
expect("and the error carries a generation, as clients require",
       type(timedOut and timedOut.gen), "number")
busd.requestTimeout = 30000

-- The host was never told that request was abandoned, so its answer still turns up. Handing it
-- to whoever is waiting is what has always happened here; filing it is what must not, since it
-- may be about an entirely different question.
busd.requestTimeout = 50
first.rpc:write({ type = "methods", data = "eee" })
pump()
host:read(500) -- taken, never answered
sleep(80)
pump()
expect("the abandoned request is answered with an error",
       field(first.rpc:read(500), "type"), "error")
busd.requestTimeout = 30000

first.rpc:write({ type = "methods", data = "eee" })
pump()
host:read(500)
host:write({ type = "methods", gen = daemon.generation, data = { "late" } })
pump()
expect("a late answer still reaches the client that is waiting now",
       field(first.rpc:read(500), "data", 1), "late")

first.rpc:write({ type = "methods", data = "eee" })
pump()
expect("but it was not filed under the question it landed on",
       field(hostAnswers({ type = "methods", gen = daemon.generation, data = { "real" } }), "data"),
       "eee")
first.rpc:read(500)

stillWorks("the bus recovers after a host timeout")

first:close()
second:close()
quiet:close()

-- Shutting down

-- run() blocks in poll with no timeout once nothing is pending, so stopping it depends on the
-- signal interrupting that poll. The entry point wires SIGTERM to stop() and relies on exactly
-- this; without it the init script's stop would have to resort to SIGKILL.
local SHUTDOWN_PATH = "/tmp/oc2test-busd-shutdown"
os.remove(SHUTDOWN_PATH)

local shutdownRpc, shutdownRpcGuest = socketPair()
local shutdownBlob, shutdownBlobGuest = socketPair()
local shutdownEvent, shutdownEventGuest = socketPair()
local shutdownListen = assert(socket.listen(SHUTDOWN_PATH))

local shutdownChild = assert(unistd.fork())
if shutdownChild == 0 then
  local other = busd.new({ rpcFd = shutdownRpcGuest, blobFd = shutdownBlobGuest,
                           eventFd = shutdownEventGuest, listenFd = shutdownListen })
  signal.signal(signal.SIGTERM, function() other:stop() end)
  local finished = pcall(other.run, other)
  other:close()
  os.remove(SHUTDOWN_PATH)
  os.exit(finished and 0 or 1)
end

for _, fd in ipairs({ shutdownRpc, shutdownRpcGuest, shutdownBlob, shutdownBlobGuest,
                      shutdownEvent, shutdownEventGuest, shutdownListen }) do
  socket.close(fd)
end

sleep(500)
expect("the daemon is listening while it runs", stat.stat(SHUTDOWN_PATH) ~= nil, true)

signal.kill(shutdownChild, signal.SIGTERM)
local _, how, code = wait.wait(shutdownChild)
expect("SIGTERM ends the run loop", how, "exited")
expect("and it exits cleanly", code, 0)
expect("giving up its socket on the way out", stat.stat(SHUTDOWN_PATH), nil)

daemon:close()
daemon:close() -- idempotent: a second close must not raise
expect("closing twice is safe", true, true)
expect("stepping a closed daemon reports rather than raising", daemon:step(0), false)

os.remove(PATH)
report()
