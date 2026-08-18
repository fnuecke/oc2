#!/usr/bin/env lua
--- Entry point for the bus daemon. See oc2/busd.lua for details.

local busd = require("oc2.busd")
local Channel = require("oc2.channel")
local blob = require("oc2.blob")
local ports = require("oc2.ports")
local socket = require("oc2.socket")

local signal = require("posix.signal")
local stat = require("posix.sys.stat")
local unistd = require("posix.unistd")

local socketPath = os.getenv("OC2_BUS_SOCKET") or busd.socketPath

local listenFd

local function die(format, ...)
  busd.log(format, ...)
  if listenFd then
    socket.close(listenFd)
    os.remove(socketPath)
  end
  os.exit(1)
end

--- /run is a tmpfs, so this is gone after every boot.
local function ensureDirectory(path)
  if path:sub(1, 1) ~= "/" then
    return -- a relative socket path is the caller's business, not ours to guess at
  end

  local directory = path:match("^(.*)/[^/]+$")
  if not directory or directory == "" or stat.stat(directory) then
    return
  end

  local mode = tonumber("755", 8)
  local made = ""
  for part in directory:gmatch("[^/]+") do
    made = made .. "/" .. part
    if not stat.stat(made) then
      stat.mkdir(made, mode)
      stat.chmod(made, mode)
    end
  end
end

ensureDirectory(socketPath)

local listenReason
listenFd, listenReason = socket.listen(socketPath)
if not listenFd then
  die("could not listen on %s: %s", socketPath, tostring(listenReason))
end

local function openPort(name, open)
  local path = ports.find(name)
  if not path then
    die("no virtio port named %s was found", name)
  end
  local handle, reason = open(path)
  if not handle then
    -- EBUSY means something else already holds the port: another daemon, or a script that
    -- started while none was running. Exiting frees the socket so clients stop waiting on us.
    die("could not open %s (%s): %s", name, path, tostring(reason))
  end
  return handle
end

local rpc = openPort("oc2.rpc.0", Channel.open)
local payload = openPort("oc2.blob.0", blob.open)
local events = openPort("oc2.event.0", function(path) return Channel.open(path, true) end)

local daemon = busd.new({
  rpcFd = rpc.fd, blobFd = payload.fd, eventFd = events.fd, listenFd = listenFd,
})

signal.signal(signal.SIGTERM, function() daemon:stop() end)
signal.signal(signal.SIGINT, function() daemon:stop() end)

daemon:primeGeneration()

busd.log("listening on %s as pid %d", socketPath, unistd.getpid())

local ok, reason = pcall(daemon.run, daemon)

daemon:close()
os.remove(socketPath)

if not ok then
  busd.log("stopped: %s", tostring(reason))
  os.exit(1)
end
