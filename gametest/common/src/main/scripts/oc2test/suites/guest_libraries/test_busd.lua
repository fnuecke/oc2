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
local seen = hostAnswers({ type = "list", gen = 3, data = { { deviceId = "aaa" } } })
expect("the request reached the host", seen and seen.type, "list")
local answer = first.rpc:read(500)
expect("and the reply came back", answer and answer.type, "list")
expect("carrying the host's data", answer and answer.data[1].deviceId, "aaa")

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

hostEvents:write({ type = "devicesChanged", gen = 4 })
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
       (hostAnswers({ type = "list", gen = 4, data = {} }) or {}).type, "list")
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
host:write({ type = "list", gen = 4, data = {} })
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
local function stillWorks(name)
  first.rpc:write({ type = "list" })
  pump()
  local request = hostAnswers({ type = "list", gen = 5, data = {} })
  expect(name, request and request.type, "list")
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
pushy.rpc:write({ type = "list" })
pushy.rpc:write({ type = "list" })
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
