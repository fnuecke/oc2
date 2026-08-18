local harness = require("harness")
local expect, raises, report = harness.expect, harness.raises, harness.report

local socket = require("oc2.socket")
local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local sys_stat = require("posix.sys.stat")
local unistd = require("posix.unistd")
local poll = require("posix.poll")
local fcntl = require("posix.fcntl")
local wait = require("posix.sys.wait")

local PATH = "/tmp/oc2test-socket"

local function pair()
  local server = assert(socket.listen(PATH))
  local client = assert(socket.connect(PATH))
  local accepted = assert(socket.accept(server))
  return server, client, accepted
end

local function closeAll(...)
  for _, fd in ipairs({ ... }) do
    socket.close(fd)
  end
end

-- Listening

local server = assert(socket.listen(PATH))
expect("listening socket exists", sys_stat.stat(PATH) ~= nil, true)
expect("socket is world accessible", sys_stat.stat(PATH).st_mode & tonumber("777", 8),
       tonumber("666", 8))
socket.close(server)

-- Leftover dead socket must be replaced.
local replacement = socket.listen(PATH)
expect("stale socket file is replaced", replacement ~= nil, true)
socket.close(replacement)

server = assert(socket.listen(PATH, tonumber("600", 8)))
expect("explicit mode is honoured", sys_stat.stat(PATH).st_mode & tonumber("777", 8),
       tonumber("600", 8))
socket.close(server)

expect("connecting to nothing fails rather than raises",
       socket.connect("/tmp/oc2test-socket-absent"), nil)

local _, missingReason = socket.listen("/tmp/oc2test-absent-dir/bus")
expect("listening under a missing directory explains itself",
       (missingReason or ""):find("oc2test-absent-dir", 1, true) ~= nil, true)

-- Live socket must not be replaced.
local live = assert(socket.listen(PATH))
local refused, refusedReason = socket.listen(PATH)
expect("a live listener is not replaced", refused, nil)
expect("and says why", (refusedReason or ""):find("already bound", 1, true) ~= nil, true)
socket.close(live)

-- Framing over a socket

local s, c, a = pair()

local channel = Channel.fromFd(a)
Channel.writeAll(c, "\0" .. require("cjson").encode({ type = "list" }) .. "\0")
local message = channel:read(1000)
expect("a frame written to a socket reads back", message and message.type, "list")

-- Channel:write on a socket-backed channel, read from the other end.
local reply = Channel.fromFd(c)
channel:write({ type = "result", gen = 7 })
local back = reply:read(1000)
expect("Channel:write goes out over the socket", back and back.type, "result")
expect("and carries its payload", back and back.gen, 7)

closeAll(s, c, a)

-- Fixed-length reads, which is how the session token gets across

s, c, a = pair()
Channel.writeAll(c, string.rep("a", 32) .. "trailing")
expect("readExactly returns exactly what was asked for",
       Channel.readExactly(a, 32, 1000), string.rep("a", 32))
expect("and leaves the rest on the descriptor",
       Channel.readExactly(a, 8, 1000), "trailing")

-- The token may be split across writes, as any stream may be.
Channel.writeAll(c, "abc")
Channel.writeAll(c, "def")
expect("readExactly reassembles across writes", Channel.readExactly(a, 6, 1000), "abcdef")

local timedOut, timeoutReason = Channel.readExactly(a, 4, 100)
expect("readExactly times out rather than blocking", timedOut, nil)
expect("and says it was a timeout", timeoutReason, "timeout")

-- A timeout discards whatever had arrived, so retrying would resume mid-value. Pinned here
-- because the surrounding Channel:read deliberately does the opposite.
Channel.writeAll(c, "ab")
expect("a partial read times out", Channel.readExactly(a, 4, 100), nil)
Channel.writeAll(c, "cd")
expect("and its bytes are gone, not resumed", Channel.readExactly(a, 2, 500), "cd")

raises("readExactly refuses an unbounded timeout", "bounded",
       function() return Channel.readExactly(a, 1) end)
raises("readExactly refuses a channel in place of a descriptor", "descriptor",
       function() return Channel.readExactly(Channel.fromFd(a), 1, 100) end)
closeAll(s, c, a)

s, c, a = pair()
socket.close(c)
local ended, endReason = Channel.readExactly(a, 4, 1000)
expect("readExactly reports the peer going away", ended, nil)
expect("and distinguishes it from a timeout", endReason, "end of file")
closeAll(s, a)

-- Descriptor state the daemon's liveness rests on. accept(2) does not inherit O_NONBLOCK from
-- the listening socket on Linux, so this is load-bearing rather than incidental.
s, c, a = pair()
expect("an accepted socket is non-blocking", unistd.read(a, 16), nil)
expect("a listening socket is non-blocking", socket.accept(s), nil)
expect("an accepted socket is close-on-exec",
       fcntl.fcntl(a, fcntl.F_GETFD) & fcntl.FD_CLOEXEC ~= 0, true)
closeAll(s, c, a)

-- Writing to a departed peer must fail rather than kill the process with SIGPIPE, and must not
-- fall into a write/poll spin: poll reports ERR and HUP whatever events were asked for, so a
-- retry loop that trusts it would never end. Reaching this line at all proves SIGPIPE is
-- ignored, since the default disposition would have killed the process instead.
s, c, a = pair()
socket.close(c)
raises("writing to a departed peer fails cleanly", "could not write to the channel",
       function() return Channel.writeAll(a, string.rep("x", 64)) end)
closeAll(s, a)

-- More than a socket buffer holds, so writeAll actually goes round its poll-for-OUT path
-- instead of completing in one write.
s, c, a = pair()
local BIG = 1024 * 1024
local child = assert(unistd.fork())
if child == 0 then
  socket.close(a)
  local seen = 0
  while seen < BIG do
    if poll.rpoll(c, 5000) ~= 1 then break end
    local chunk = unistd.read(c, 64 * 1024)
    if not chunk or #chunk == 0 then break end
    seen = seen + #chunk
  end
  os.exit(seen == BIG and 0 or 1)
end
socket.close(c)
local sent = pcall(Channel.writeAll, a, string.rep("x", BIG))
local _, _, reaped = wait.wait(child)
expect("a write larger than the socket buffer completes", sent, true)
expect("and the reader saw all of it", reaped, 0)
closeAll(s, a)

-- reset() drains a channel. A descriptor at end of file stays readable and returns an empty
-- string forever, so this is the shape that used to loop without end.
s, c, a = pair()
Channel.writeAll(c, "leftovers")
socket.close(c)
local drained = Channel.fromFd(a)
drained:reset()
expect("reset returns on a hung up peer", true, true)
blob.fromFd(a):reset()
expect("payload reset returns on a hung up peer", true, true)
closeAll(s, a)

-- Payloads over a socket, the blob channel's shape

s, c, a = pair()
local sender = blob.fromFd(c)
local receiver = blob.fromFd(a)
sender:write("the quick brown fox")
expect("a payload crosses a socket intact", receiver:read(19), "the quick brown fox")

sender:write(string.rep("x", 70000))
expect("a payload larger than one read", #assert(receiver:read(70000)), 70000)
closeAll(s, c, a)

-- What the daemon's poll loop depends on

s, c, a = pair()
local fds = { [a] = { events = { IN = true } } }
expect("nothing readable yet", poll.poll(fds, 0), 0)
Channel.writeAll(c, "x")
expect("poll reports readable", poll.poll(fds, 100), 1)
expect("revents carries IN", fds[a].revents.IN, true)

assert(unistd.read(a, 16))
socket.close(c)
expect("poll reports the hangup", poll.poll(fds, 100) >= 1, true)
expect("a closed peer reads as end of file", #assert(unistd.read(a, 16)), 0)
closeAll(s, a)

-- Peer credentials, which are diagnostics only

s, c, a = pair()
expect("peerPid reports this process", socket.peerPid(a), unistd.getpid())
closeAll(s, c, a)

os.remove(PATH)
report()
