local ports = {}

local ports_path = "/sys/class/virtio-ports"

local function readPortName(entry)
  local file = io.open(ports_path .. "/" .. entry .. "/name", "r")
  if not file then
    return nil
  end
  local value = file:read("l")
  file:close()
  return value
end

function ports.find(name)
  local ok, dirent = pcall(require, "posix.dirent")
  if not ok then
    return nil
  end

  local listed, entries = pcall(dirent.dir, ports_path)
  if not listed or not entries then
    return nil
  end

  for _, entry in ipairs(entries) do
    if entry ~= "." and entry ~= ".." then
      if readPortName(entry) == name then
        return "/dev/" .. entry
      end
    end
  end
end

return ports
