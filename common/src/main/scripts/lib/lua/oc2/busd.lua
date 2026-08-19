--- The OpenComputers II bus daemon.
---
--- Takes ownership of the three bus ports -- rpc, blob and event -- while it runs and
--- multiplexes them to many clients over an AF_UNIX socket. The kernel lets only one process
--- open a virtio port at a time, so without this exactly one script can talk to the host at
--- all, and any event queued while nobody holds the event port is discarded by the driver.
---
--- Between daemon and client the wire format is the same as when talking to the host, with an
--- additional handshake at the start: every connection opens with a one byte role code,
--- "R", "B" or "E". The rpc connection then exchanges a JSON `hello` and is handed a session
--- token; the blob and event connections present that token as their first 32 bytes, which
--- binds them to the session for as long as they live. The token is no longer used after that.

local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local clock = require("oc2.clock")
local socket = require("oc2.socket")

local poll = require("posix.poll")
local unistd = require("posix.unistd")

local busd = {}

busd.socketPath = "/run/oc2/bus"

busd.protocolVersion = 1
busd.tokenLength = 32
busd.handshakeTimeout = 5000
busd.requestTimeout = 30000
busd.hostBlobTimeout = 2000
busd.primeTimeout = 2000
busd.eventBufferLimit = 64 * 1024
busd.replyBufferLimit = 256 * 1024
busd.maxEventsPerPump = 32
busd.maxFramesPerPump = 64

--- The host refuses anything larger than 4 KB, so a client frame past this is going nowhere
--- useful anyway. Well under Channel's own default, which is sized for host replies.
busd.maxRequestSize = 16 * 1024

--- How long a session may sit unfinished -- still attaching, or waiting for payload bytes that
--- were announced and never arrived -- before it is dropped.
busd.sessionTimeout = 10000

--- Handlers keyed by message type.
busd.handlers = {}

local roleOf = { R = "rpc", B = "blob", E = "event" }
local validRole = { rpc = true, blob = true, event = true }

local isRetryable = Channel.isRetryable

local function log(format, ...)
  io.stderr:write("oc2busd: " .. string.format(format, ...) .. "\n")
end

busd.log = log

-- Buffered output
--
-- The daemon must never block on a client. A 512 KB payload will not fit in a socket buffer,
-- and a client that stops reading must cost us nothing but memory we chose to spend.

local Writer = {}
Writer.__index = Writer

function Writer.new(fd, limit)
  return setmetatable({ fd = fd, chunks = {}, size = 0, limit = limit }, Writer)
end

--- Returns false when the queue is already at its limit, which means the peer has stopped
--- reading. A reply stream cannot meaningfully be shortened, so callers drop the session.
function Writer:push(data)
  if #data == 0 then
    return true
  end
  if self.limit and self.size + #data > self.limit then
    return false
  end
  self.chunks[#self.chunks + 1] = data
  self.size = self.size + #data
  return true
end

function Writer:isEmpty()
  return self.size == 0
end

--- Writes whatever the socket will take right now. Returns false only if the peer is gone.
function Writer:flush()
  while self.chunks[1] do
    local chunk = self.chunks[1]
    local written, reason, number = unistd.write(self.fd, chunk)
    if not written then
      if isRetryable(number) then
        return true -- come back when poll says it drained
      end
      return false, reason
    end
    if written == 0 then
      return true
    end

    self.size = self.size - written
    if written < #chunk then
      self.chunks[1] = chunk:sub(written + 1)
      return true
    end
    table.remove(self.chunks, 1)
  end
  return true
end

-- Sessions

local Session = {}
Session.__index = Session

function Session.new(token)
  return setmetatable({
    token = token,
    roles = { rpc = true },
    connections = {},   -- role -> connection
    blobInbox = {},     -- payload bytes that arrived before the request that claims them
    blobInboxSize = 0,
    established = false,
    dead = false,
    eventsDropped = 0,
    -- Armed until the session finishes attaching, and again while a request waits for a
    -- payload it announced. Both are states a client can enter and then simply abandon.
    deadline = clock.deadline(busd.sessionTimeout),
  }, Session)
end

function Session:has(role)
  return self.connections[role] ~= nil
end

function Session:isComplete()
  for role in pairs(self.roles) do
    if not self:has(role) then
      return false
    end
  end
  return true
end

-- The daemon

local Daemon = {}
Daemon.__index = Daemon

function busd.new(options)
  local daemon = setmetatable({
    rpc = Channel.fromFd(options.rpcFd),
    payload = blob.fromFd(options.blobFd),
    events = Channel.fromFd(options.eventFd),
    listenFd = options.listenFd,
    generation = nil,
    sessions = {},      -- session -> true, for broadcasting
    tokens = {},        -- token -> session, only while attaches are still outstanding
    connections = {},   -- fd -> connection
    queue = {},         -- sessions with a dispatchable request, oldest first
    inFlight = nil,     -- { session, request, id, clientId, deadline }
    nextRequestId = 0,
    running = false,
  }, Daemon)

  daemon:dropCache() -- init cache
  return daemon
end

local function randomToken()
  local source = io.open("/dev/urandom", "rb")
  local bytes = source and source:read(16)
  if source then
    source:close()
  end
  if not bytes or #bytes < 16 then
    -- We don't need a secure id here, just make sure we avoid conflicting ids.
    bytes = string.pack("<I8I8", clock.ms(), unistd.getpid())
  end
  return (bytes:gsub(".", function(c) return string.format("%02x", c:byte()) end))
end

function Daemon:takeRequestId()
  self.nextRequestId = self.nextRequestId % 0x7FFFFFFF + 1
  return self.nextRequestId
end

function Daemon:generateToken()
  local token = randomToken()
  while self.tokens[token] do
    token = randomToken()
  end
  return token
end

-- Connections

function Daemon:accept()
  local fd, reason, number = socket.accept(self.listenFd)
  if not fd then
    -- A connection aborted between the poll that announced it and this accept is routine.
    if not isRetryable(number) then
      log("accept failed: %s", tostring(reason))
    end
    return
  end

  self.connections[fd] = {
    fd = fd,
    state = "role",
    buffer = "",
    writer = Writer.new(fd, busd.replyBufferLimit),
    deadline = clock.deadline(busd.handshakeTimeout),
    pid = socket.peerPid(fd),
  }
end

function Daemon:dropConnection(connection, reason)
  if not self.connections[connection.fd] then
    return
  end
  self.connections[connection.fd] = nil
  socket.close(connection.fd)

  local session = connection.session
  if session and not session.dead then
    -- Any one socket going away means we consider the whole session closed for simplicity.
    self:dropSession(session, reason or "a session socket closed")
  end
end

function Daemon:dropSession(session, reason)
  if session.dead then
    return
  end
  session.dead = true
  self.sessions[session] = nil
  self.tokens[session.token] = nil

  local doomed = {}
  for _, connection in pairs(session.connections) do
    doomed[#doomed + 1] = connection
  end
  session.connections = {}

  for _, connection in ipairs(doomed) do
    connection.session = nil
    if self.connections[connection.fd] then
      self.connections[connection.fd] = nil
      socket.close(connection.fd)
    end
  end

  session.blobInbox = {}
  session.blobInboxSize = 0

  for i = #self.queue, 1, -1 do
    if self.queue[i] == session then
      table.remove(self.queue, i)
    end
  end

  -- Any in-flight request stays so we know to throw away the pending host reply.

  if reason then
    log("session %s (pid %s) ended: %s",
        session.token:sub(1, 8), tostring(session.pid or "?"), reason)
  end
end

--- Reports a failure to a session and then ends it.
function Daemon:fail(session, reason, id)
  self:sendError(session, reason, id)
  local connection = session.connections.rpc
  if connection then
    connection.writer:flush()
  end
  self:dropSession(session, reason)
end

-- Handshakes

--- Appends up to `wanted` bytes to the connection's scratch buffer. Returns nil once the
--- connection is gone or has nothing to give.
function Daemon:readInto(connection, wanted)
  local chunk, reason, number = unistd.read(connection.fd, wanted)
  if not chunk then
    if isRetryable(number) then
      return nil
    end
    self:dropConnection(connection, reason)
    return nil
  end
  if #chunk == 0 then
    self:dropConnection(connection, "peer closed")
    return nil
  end
  connection.buffer = connection.buffer .. chunk
  return #chunk
end

function Daemon:onRoleByte(connection)
  if not self:readInto(connection, 1) then
    return
  end

  local role = roleOf[connection.buffer:sub(1, 1)]
  connection.buffer = ""
  if not role then
    self:dropConnection(connection, "unknown role code")
    return
  end

  connection.role = role
  if role == "rpc" then
    connection.channel = Channel.fromFd(connection.fd, busd.maxRequestSize)
    connection.state = "hello"
  else
    connection.state = "token"
    if role == "event" then
      connection.writer.limit = busd.eventBufferLimit
    else
      connection.writer.limit = blob.maxInbound + busd.replyBufferLimit
    end
  end
end

function Daemon:onToken(connection)
  if not self:readInto(connection, busd.tokenLength - #connection.buffer) then
    return
  end
  if #connection.buffer < busd.tokenLength then
    return
  end

  local token = connection.buffer
  connection.buffer = ""

  local session = self.tokens[token]
  if not session or session.dead or not session.roles[connection.role]
      or session:has(connection.role) then
    self:dropConnection(connection, "unusable session token")
    return
  end

  self:attach(session, connection)
end

--- Reads one framed message from a client connection.
function Daemon:readFrame(connection)
  local message, reason = connection.channel:read(0)
  if message then
    return message
  end
  if reason and reason ~= "timeout" then
    self:dropConnection(connection, reason)
  end
  return nil
end

function Daemon:onHello(connection)
  local message = self:readFrame(connection)
  if not message then
    return
  end

  if message.type ~= "hello" then
    self:refuse(connection, "expected a hello, got " .. tostring(message.type))
    return
  end
  if message.version ~= busd.protocolVersion then
    self:refuse(connection, "unsupported protocol version " .. tostring(message.version))
    return
  end

  local session = Session.new(self:generateToken())
  if type(message.roles) == "table" then
    for _, role in ipairs(message.roles) do
      if not validRole[role] then
        self:refuse(connection, "unknown role " .. tostring(role))
        return
      end
      session.roles[role] = true
    end
  end

  session.pid = connection.pid
  self.sessions[session] = true
  self.tokens[session.token] = session
  self:attach(session, connection)

  connection.writer:push(Channel.frame({
    type = "hello",
    version = busd.protocolVersion,
    token = session.token,
    gen = self.generation,
  }))
end

--- Reports a handshake failure on a connection that has no session yet, then closes it.
function Daemon:refuse(connection, reason)
  connection.writer:push(Channel.frame({ type = "error", gen = self.generation, data = reason }))
  connection.writer:flush()
  self:dropConnection(connection, reason)
end

function Daemon:attach(session, connection)
  connection.session = session
  connection.state = "ready"
  connection.deadline = nil
  session.connections[connection.role] = connection

  if connection.role == "blob" then
    session.blobWriter = connection.writer
  elseif connection.role == "event" then
    session.eventWriter = connection.writer
  end

  if session:isComplete() then
    session.established = true
    session.deadline = nil
    self.tokens[session.token] = nil -- spent; see onToken
    self:tryEnqueue(session)
  end
end

-- Device cache

function Daemon:dropCache()
  self.cache = { methods = {}, ids = {}, bodies = {} }
end

local function clientIdOf(message)
  local id = message and message.id
  return type(id) == "number" and id or nil
end

function Daemon:noteGeneration(gen)
  if type(gen) ~= "number" or (self.generation and gen <= self.generation) then
    return
  end
  self.generation = gen
  self:dropCache()
end

function Daemon:cacheDeviceList(devices)
  local cache = self.cache
  cache.list = devices
  cache.ids = {}
  if type(devices) == "table" then
    for _, device in ipairs(devices) do
      if type(device) == "table" and type(device.deviceId) == "string" then
        cache.ids[device.deviceId] = true
      end
    end
  end
end

function Daemon:cacheReply(request, reply)
  if reply.type ~= request.type or reply.gen ~= self.generation then
    return
  end

  if reply.type == "list" then
    self:cacheDeviceList(reply.data)
  elseif reply.type == "methods" and self.cache.ids[request.data] then
    self.cache.methods[request.data] = reply.data
  end
end

local function cacheBody(message)
  local frame = Channel.frame(message)
  assert(frame:sub(2, 2) == "{", "a cached reply must encode as a JSON object")
  return frame:sub(3, -2)
end

local function framedWithId(body, id)
  if id then
    return Channel.delimiter .. '{"id":' .. id .. ',' .. body .. Channel.delimiter
  end
  return Channel.delimiter .. "{" .. body .. Channel.delimiter
end

local function serve(daemon, session, frame)
  local connection = session.connections.rpc
  if connection and not connection.writer:push(frame) then
    daemon:dropSession(session, "the client stopped reading its replies")
  end
  return true
end

busd.handlers.list = function(daemon, session, message)
  local cache = daemon.cache
  if not cache.list then
    return false
  end
  cache.bodies.list = cache.bodies.list
      or cacheBody({ type = "list", gen = daemon.generation, data = cache.list })
  return serve(daemon, session, framedWithId(cache.bodies.list, clientIdOf(message)))
end

busd.handlers.methods = function(daemon, session, message)
  local cache = daemon.cache
  if not cache.list or type(message.data) ~= "string" then
    return false
  end

  local methods = cache.methods[message.data]
  if not methods then
    return false
  end

  local bodies = cache.bodies.methods
  if not bodies then
    bodies = {}
    cache.bodies.methods = bodies
  end
  bodies[message.data] = bodies[message.data]
      or cacheBody({ type = "methods", gen = daemon.generation, data = methods })
  return serve(daemon, session, framedWithId(bodies[message.data], clientIdOf(message)))
end

-- Requests

function Daemon:sendError(session, reason, id)
  self:reply(session, { type = "error", gen = self.generation, id = id, data = reason })
end

--- Queues a reply, and its payload if there is one.
function Daemon:reply(session, message, payload)
  local connection = session.connections.rpc
  if not connection then
    return true
  end
  if not connection.writer:push(Channel.frame(message)) then
    return false
  end
  if payload and session.blobWriter and not session.blobWriter:push(payload) then
    return false
  end
  return true
end

function Daemon:onRequest(session, message)
  if type(message.type) ~= "string" then
    self:fail(session, "a request needs a type", clientIdOf(message))
    return
  end
  if session.request then
    self:fail(session, "a request is already in flight on this session", clientIdOf(message))
    return
  end

  local handler = busd.handlers[message.type]
  if handler and message.blob == nil and session.established
      and not (self.inFlight and self.inFlight.session == session)
      and handler(self, session, message) then
    return
  end

  local reference = message.blob
  if type(reference) == "table" then
    local length = reference.length
    if type(length) ~= "number" or length < 0 or length > blob.maxOutbound then
      self:sendError(session, "announced payload size is out of range", clientIdOf(message))
      return
    end
    session.request = message
    session.requestBlobLength = length
    session.deadline = clock.deadline(busd.sessionTimeout)
  else
    session.request = message
    session.requestBlobLength = nil
  end

  self:tryEnqueue(session)
end

function Daemon:tryEnqueue(session)
  if not session.established or not session.request or session.queued then
    return
  end
  if session.requestBlobLength and session.blobInboxSize < session.requestBlobLength then
    return
  end

  session.deadline = nil
  session.queued = true
  self.queue[#self.queue + 1] = session
end

function Daemon:takeInboundPayload(session, length, id)
  local joined = table.concat(session.blobInbox)
  if #joined > length then
    self:fail(session, "more payload bytes arrived than were announced", id)
    return nil
  end

  session.blobInbox = {}
  session.blobInboxSize = 0
  return joined
end

function Daemon:dispatch()
  while not self.inFlight and self.queue[1] do
    local session = table.remove(self.queue, 1)
    session.queued = false

    if not session.dead and session.request then
      local message = session.request
      local length = session.requestBlobLength
      session.request = nil
      session.requestBlobLength = nil

      local payload
      if length then
        payload = self:takeInboundPayload(session, length, message.id)
      end

      if not session.dead and (payload or not length) then
        local clientId = message.id
        local id = self:takeRequestId()
        message.id = id
        local sent, reason = pcall(self.writeToHost, self, message, payload)
        if sent then
          self.inFlight = {
            session = session,
            request = message,
            id = id,
            clientId = clientId,
            deadline = clock.deadline(busd.requestTimeout),
          }
        elseif payload then
          log("FATAL: the host blob stream may be desynchronised: %s", tostring(reason))
          self:sendError(session, "the host connection failed mid-request", clientId)
        else
          self:sendError(session, "could not reach the host: " .. tostring(reason), clientId)
        end
      end
    end
  end
end

function Daemon:writeToHost(message, payload)
  if payload then
    self.payload:write(payload)
  end
  self.rpc:write(message)
end

function Daemon:resync()
  self.rpc:reset()
  self.payload:reset()
end

function Daemon:onHostReply()
  local message = self.rpc:read(0)
  if not message then
    return false
  end

  self:noteGeneration(message.gen)

  local payload
  local reference = message.blob
  if type(reference) == "table" and type(reference.length) == "number" then
    if reference.length < 0 or reference.length > blob.maxInbound then
      log("host announced an implausible payload size: %s", tostring(reference.length))
      self:resync()
    else
      -- Blocking, briefly: the bytes are already on their way, and the host is the one peer
      -- that cannot be a hostile slow writer.
      local data, reason = self.payload:read(reference.length, busd.hostBlobTimeout)
      if not data then
        log("could not read the host payload: %s", tostring(reason))
        self:resync()
      else
        payload = data
      end
    end
  end

  local inFlight = self.inFlight
  if type(message.id) == "number" and message.id ~= 0
      and message.id ~= (inFlight and inFlight.id) then
    return true
  end

  self.inFlight = nil
  local session = inFlight and inFlight.session
  local request = inFlight and inFlight.request

  if request and not payload then
    self:cacheReply(request, message)
  end

  message.id = inFlight and inFlight.clientId or nil
  if session and not session.dead and not self:reply(session, message, payload) then
    self:dropSession(session, "the client stopped reading its replies")
  end
  return true
end

function Daemon:onHostEvent()
  for _ = 1, busd.maxEventsPerPump do
    local event = self.events:read(0)
    if not event then
      return
    end
    self:noteGeneration(event.gen)
    self:broadcast(event)
  end
end

function Daemon:broadcast(event)
  local frame = Channel.frame(event)
  for session in pairs(self.sessions) do
    local writer = session.eventWriter
    if writer and not session.dead then
      if writer.size + #frame > busd.eventBufferLimit then
        session.eventsDropped = session.eventsDropped + 1
      else
        if session.eventsDropped > 0 then
          writer:push(Channel.frame({
            type = "eventsDropped",
            gen = self.generation,
            data = session.eventsDropped,
          }))
          session.eventsDropped = 0
        end
        writer:push(frame)
      end
    end
  end
end

function Daemon:expire()
  local now = clock.ms()

  if self.inFlight and now >= self.inFlight.deadline then
    local session = self.inFlight.session
    local clientId = self.inFlight.clientId
    self.inFlight = nil
    -- The host was never told to stop, so it answers eventually. That answer carries the id of the
    -- request that gave up, which is how onHostReply knows to drop it.
    self:resync()
    if session and not session.dead then
      self:sendError(session, "the host did not answer in time", clientId)
    end
  end

  local staleSessions
  for session in pairs(self.sessions) do
    if session.deadline and now >= session.deadline then
      staleSessions = staleSessions or {}
      staleSessions[#staleSessions + 1] = session
    end
  end
  for _, session in ipairs(staleSessions or {}) do
    self:dropSession(session, session.established
        and "announced a payload that never arrived" or "session was never completed")
  end

  local staleConnections
  for _, connection in pairs(self.connections) do
    if connection.deadline and now >= connection.deadline then
      staleConnections = staleConnections or {}
      staleConnections[#staleConnections + 1] = connection
    end
  end
  for _, connection in ipairs(staleConnections or {}) do
    self:dropConnection(connection, "handshake did not complete")
  end
end

-- Event loop

function Daemon:onClientRequests(connection)
  local session = connection.session
  for _ = 1, busd.maxFramesPerPump do
    if session.dead then
      return -- a previous request in this same batch ended the session, and closed its socket
    end
    local message = self:readFrame(connection)
    if not message then
      return
    end
    self:onRequest(session, message)
  end
end

function Daemon:onClientPayload(connection)
  local session = connection.session
  local chunk, reason, number = unistd.read(connection.fd, 32 * 1024)
  if not chunk then
    if not isRetryable(number) then
      self:dropConnection(connection, reason)
    end
    return
  end
  if #chunk == 0 then
    self:dropConnection(connection, "peer closed")
    return
  end

  session.blobInbox[#session.blobInbox + 1] = chunk
  session.blobInboxSize = session.blobInboxSize + #chunk
  if session.blobInboxSize > blob.maxOutbound then
    self:fail(session, "sent more payload than the host accepts")
    return
  end

  self:tryEnqueue(session)
end

function Daemon:readable(connection)
  if connection.state == "role" then
    self:onRoleByte(connection)
  elseif connection.state == "token" then
    self:onToken(connection)
  elseif connection.state == "hello" then
    self:onHello(connection)
  elseif connection.role == "rpc" then
    self:onClientRequests(connection)
  elseif connection.role == "blob" then
    self:onClientPayload(connection)
  elseif connection.role == "event" then
    local chunk, reason, number = unistd.read(connection.fd, 256)
    if not chunk then
      if not isRetryable(number) then
        self:dropConnection(connection, reason)
      end
    elseif #chunk == 0 then
      self:dropConnection(connection, "peer closed")
    else
      self:dropConnection(connection, "wrote to its event socket")
    end
  end
end

function Daemon:pollSet()
  local fds = { [self.listenFd] = { events = { IN = true } },
                [self.rpc.fd] = { events = { IN = true } },
                [self.events.fd] = { events = { IN = true } } }
  for fd, connection in pairs(self.connections) do
    fds[fd] = { events = { IN = true, OUT = not connection.writer:isEmpty() or nil } }
  end
  return fds
end

function Daemon:nextTimeout()
  local deadline = self.inFlight and self.inFlight.deadline
  local function consider(candidate)
    if candidate and (not deadline or candidate < deadline) then
      deadline = candidate
    end
  end

  for _, connection in pairs(self.connections) do
    consider(connection.deadline)
  end
  for session in pairs(self.sessions) do
    consider(session.deadline)
  end

  if not deadline then
    return -1
  end
  return clock.remaining(deadline)
end

function Daemon:flushAll()
  local blocked
  for _, connection in pairs(self.connections) do
    if not connection.writer:isEmpty() then
      local ok, reason = connection.writer:flush()
      if not ok then
        blocked = blocked or {}
        blocked[#blocked + 1] = { connection = connection, reason = reason }
      end
    end
  end
  for _, entry in ipairs(blocked or {}) do
    self:dropConnection(entry.connection, entry.reason)
  end
end

--- `budget` caps how long this pass may wait, which is what lets a test drive the daemon by
--- hand instead of handing it a thread. The daemon itself never passes one: with nothing
--- pending there is nothing to wake up for.
function Daemon:step(budget)
  if not self.listenFd or not self.rpc.fd or not self.events.fd then
    return false
  end

  local timeout = self:nextTimeout()
  if budget and (timeout < 0 or timeout > budget) then
    timeout = budget
  end

  local fds = self:pollSet()
  local ready = poll.poll(fds, timeout)

  if ready and ready > 0 then
    if fds[self.listenFd].revents.IN then
      self:accept()
    end
    if fds[self.rpc.fd].revents.IN then
      self:onHostReply()
    end
    if fds[self.events.fd].revents.IN then
      self:onHostEvent()
    end

    local order = {}
    for fd in pairs(fds) do
      if self.connections[fd] then
        order[#order + 1] = fd
      end
    end
    table.sort(order)

    for _, fd in ipairs(order) do
      local entry = fds[fd]
      local connection = self.connections[fd]
      if connection and entry.revents then
        if entry.revents.OUT then
          local ok, reason = connection.writer:flush()
          if not ok then
            self:dropConnection(connection, reason)
            connection = nil
          end
        end
        if connection and self.connections[fd] and (entry.revents.IN or entry.revents.HUP) then
          self:readable(connection)
        end
      end
    end
  end

  self:expire()
  self:dispatch()
  self:flushAll()
end

function Daemon:primeGeneration()
  local id = self:takeRequestId()
  local ok = pcall(function()
    self.rpc:reset()
    self.rpc:write({ type = "list", id = id })
  end)
  if not ok then
    log("could not ask the host for the bus generation")
    return
  end

  local deadline = clock.deadline(busd.primeTimeout)
  local message
  repeat
    message = self.rpc:read(clock.remaining(deadline))
    if message and type(message.id) == "number" and message.id ~= 0 and message.id ~= id then
      local reference = message.blob
      if type(reference) == "table" and type(reference.length) == "number"
          and reference.length > 0 and reference.length <= blob.maxInbound then
        self.payload:read(reference.length, busd.hostBlobTimeout)
      end
      message = nil
    end
  until message or clock.expired(deadline)

  if message and type(message.gen) == "number" then
    self:noteGeneration(message.gen)
    if message.type == "list" then
      self:cacheDeviceList(message.data)
    end
  else
    -- Shouldn't really happen, but we can cope (get one later).
    log("host did not report a bus generation at startup")
  end
end

function Daemon:run()
  self.running = true
  while self.running do
    self:step()
  end
end

function Daemon:stop()
  self.running = false
end

function Daemon:close()
  self:stop()

  local doomed = {}
  for _, connection in pairs(self.connections) do
    doomed[#doomed + 1] = connection
  end
  self.connections = {}
  self.sessions = {}
  self.tokens = {}
  self.queue = {}
  for _, connection in ipairs(doomed) do
    socket.close(connection.fd)
  end

  if self.listenFd then
    socket.close(self.listenFd)
    self.listenFd = nil
  end
  self.rpc:close()
  self.payload:close()
  self.events:close()
end

busd.Daemon = Daemon
busd.Writer = Writer
busd.Session = Session

return busd
