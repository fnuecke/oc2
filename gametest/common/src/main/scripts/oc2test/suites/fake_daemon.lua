-- A bus daemon with a scripted host behind it, listening on the socket path given in argv.
--   lua fake_daemon.lua <socket-path> [seconds]

local busd = require("oc2.busd")
local blob = require("oc2.blob")
local Channel = require("oc2.channel")
local socket = require("oc2.socket")
local sys = require("posix.sys.socket")

local path = assert(arg[1], "usage: fake_daemon.lua <socket-path> [seconds]")
local seconds = tonumber(arg[2]) or 30

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

local listenFd = assert(socket.listen(path))
local daemon = busd.new({
  rpcFd = rpcGuest, blobFd = blobGuest, eventFd = eventGuest, listenFd = listenFd,
})

local host = Channel.fromFd(rpcHost)
local hostPayload = blob.fromFd(blobHost)
local hostEvents = Channel.fromFd(eventHost)

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

local generation = 1
daemon.generation = generation -- stands in for primeGeneration, which would block on us

local deadline = os.time() + seconds
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

daemon:close()
os.remove(path)
