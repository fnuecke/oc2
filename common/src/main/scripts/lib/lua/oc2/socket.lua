local fcntl = require("posix.fcntl")
local signal = require("posix.signal")
local stat = require("posix.sys.stat")
local sys = require("posix.sys.socket")
local unistd = require("posix.unistd")

local socket = {}

socket.backlog = 16
socket.defaultMode = tonumber("666", 8)

local SOL_SOCKET = 1
local SO_PEERCRED = 17

signal.signal(signal.SIGPIPE, signal.SIG_IGN)

local function updateFlags(fd, getCommand, setCommand, flag)
  local flags, reason, number = fcntl.fcntl(fd, getCommand)
  if not flags then
    return nil, reason, number
  end
  local ok, setReason, setNumber = fcntl.fcntl(fd, setCommand, flags | flag)
  if not ok then
    return nil, setReason, setNumber
  end
  return true
end

function socket.setNonBlocking(fd)
  return updateFlags(fd, fcntl.F_GETFL, fcntl.F_SETFL, fcntl.O_NONBLOCK)
end

function socket.setCloseOnExec(fd)
  return updateFlags(fd, fcntl.F_GETFD, fcntl.F_SETFD, fcntl.FD_CLOEXEC)
end

local function prepare(fd)
  local ok, reason, number = socket.setNonBlocking(fd)
  if not ok then
    unistd.close(fd)
    return nil, reason, number
  end
  socket.setCloseOnExec(fd)
  return fd
end

function socket.connect(path)
  local fd, reason, number = sys.socket(sys.AF_UNIX, sys.SOCK_STREAM, 0)
  if not fd then
    return nil, reason, number
  end

  local ok, connectReason, connectNumber = sys.connect(fd, { family = sys.AF_UNIX, path = path })
  if not ok then
    unistd.close(fd)
    return nil, string.format("%s (%s)", tostring(connectReason), path), connectNumber
  end

  return prepare(fd)
end

function socket.listen(path, mode)
  local probe = socket.connect(path)
  if probe then
    unistd.close(probe)
    return nil, string.format("another listener is already bound to %s", path)
  end
  os.remove(path)

  local fd, reason, number = sys.socket(sys.AF_UNIX, sys.SOCK_STREAM, 0)
  if not fd then
    return nil, reason, number
  end

  local function fail(failureReason, failureNumber)
    unistd.close(fd)
    os.remove(path)
    return nil, string.format("%s (%s)", tostring(failureReason), path), failureNumber
  end

  local bound, bindReason, bindNumber = sys.bind(fd, { family = sys.AF_UNIX, path = path })
  if not bound then
    return fail(bindReason, bindNumber)
  end

  local chmodded, chmodReason, chmodNumber = stat.chmod(path, mode or socket.defaultMode)
  if not chmodded then
    return fail(chmodReason, chmodNumber)
  end

  local listening, listenReason, listenNumber = sys.listen(fd, socket.backlog)
  if not listening then
    return fail(listenReason, listenNumber)
  end

  local ready, prepareReason, prepareNumber = prepare(fd)
  if not ready then
    os.remove(path) -- prepare already closed the descriptor
    return nil, string.format("%s (%s)", tostring(prepareReason), path), prepareNumber
  end
  return ready
end

function socket.accept(listenFd)
  local fd, reason, number = sys.accept(listenFd)
  if not fd then
    return nil, reason, number
  end
  return prepare(fd)
end

function socket.peerPid(fd)
  local ok, credentials = pcall(sys.getsockopt, fd, SOL_SOCKET, SO_PEERCRED)
  if ok and math.type(credentials) == "integer" then
    return credentials
  end
  return nil
end

function socket.close(fd)
  if not fd then
    return nil, "no descriptor"
  end
  return unistd.close(fd)
end

return socket
