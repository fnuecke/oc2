local Channel = require("oc2.channel")

local Events = {}
Events.__index = Events

function Events.open(path)
  local channel, reason = Channel.open(path, true)
  if not channel then
    return nil, reason
  end
  return setmetatable({ channel = channel }, Events)
end

function Events:close()
  self.channel:close()
end

function Events:poll()
  return self.channel:read(0)
end

function Events:wait(timeout)
  return self.channel:read(timeout or -1)
end

return Events
