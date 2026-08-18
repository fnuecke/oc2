local bus, reason = require("oc2.bus").connect()

return assert(bus, "could not open the device bus: " .. tostring(reason))
