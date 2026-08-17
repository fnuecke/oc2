local cjson = require("cjson")

local virtio = { NUL = string.char(0), requests = 0, gated = false }

local function newPort(fd, path, name, solicited)
  return { fd = fd, path = path, name = name, solicited = solicited, armed = false,
           chunks = {}, writes = {} }
end

virtio.rpc = newPort(7, "/dev/vport0p0", "oc2.rpc.0", true)
virtio.blob = newPort(8, "/dev/vport0p1", "oc2.blob.0", true)
virtio.event = newPort(9, "/dev/vport0p2", "oc2.event.0", false)

local all = { virtio.rpc, virtio.blob, virtio.event }
local byFd, byPath, nameFiles, entries = {}, {}, {}, { ".", ".." }
for _, port in ipairs(all) do
  local entry = port.path:match("[^/]+$")
  byFd[port.fd] = port
  byPath[port.path] = port
  nameFiles["/sys/class/virtio-ports/" .. entry .. "/name"] = port.name
  entries[#entries + 1] = entry
end

local function readable(port)
  if virtio.gated and port.solicited and not port.armed then
    return false
  end
  return #port.chunks > 0
end

local function arm(value)
  for _, port in ipairs(all) do port.armed = value end
end

package.preload["posix.fcntl"] = function()
  return {
    open = function(path)
      local port = byPath[path]
      if not port then
        return nil, "no such port " .. tostring(path)
      end
      return port.fd
    end,
    O_RDWR = 2, O_RDONLY = 0, O_CLOEXEC = 524288, O_NONBLOCK = 2048,
  }
end
package.preload["posix.unistd"] = function()
  return {
    read = function(fd, n)
      local port = byFd[fd]
      if not port or not readable(port) then
        return nil
      end
      local chunk = table.remove(port.chunks, 1)
      if n and #chunk > n then
        table.insert(port.chunks, 1, chunk:sub(n + 1))
        chunk = chunk:sub(1, n)
      end
      if port == virtio.rpc then
        port.armed = false
      end
      return chunk
    end,
    write = function(fd, data)
      local port = byFd[fd]
      if port then
        port.writes[#port.writes + 1] = data
        if port == virtio.rpc then
          virtio.requests = virtio.requests + 1
          arm(true)
        end
      end
      return #data
    end,
    close = function() end,
  }
end
package.preload["posix.poll"] = function()
  return { rpoll = function(fd)
    local port = byFd[fd]
    return (port and readable(port)) and 1 or 0
  end }
end
package.preload["posix.dirent"] = function()
  return { dir = function() return entries end }
end

local realOpen = io.open
io.open = function(path, mode)
  if nameFiles[path] then
    return { read = function() return nameFiles[path] end, close = function() end }
  end
  return realOpen(path, mode)
end

function virtio.encode(message)
  return cjson.encode(message)
end

function virtio.decode(text)
  return cjson.decode(text)
end

function virtio.frame(message)
  return virtio.NUL .. virtio.encode(message) .. virtio.NUL
end

function virtio.feed(port, chunks)
  for i = #port.chunks, 1, -1 do port.chunks[i] = nil end
  for _, chunk in ipairs(chunks) do port.chunks[#port.chunks + 1] = chunk end
end

function virtio.add(port, chunk)
  port.chunks[#port.chunks + 1] = chunk
end

function virtio.reply(...)
  arm(false)
  local framed = {}
  for i, message in ipairs({ ... }) do framed[i] = virtio.frame(message) end
  virtio.feed(virtio.rpc, framed)
  for _, port in ipairs(all) do
    for i = #port.writes, 1, -1 do port.writes[i] = nil end
  end
end

return virtio
