# Transposer
![Less posing, more rights](block:oc2:transposer)

The transposer moves items and fluids between containers next to it. This includes chests, cauldrons and machines supporting automation. Moving to or from an unblocked side drops items into the world and picks them up again, or fills with and drains fluids.

It can be controlled using both the [high-level API](../hlapi.md) and the [mid-level API](../mlapi.md). For example:  
`local d = require("devices")`  
`local t = d:find("transposer")`  
`print(t:moveItems("north", 0,`  
`                  "south", 0, 64))`

## API
Programs control the transposer through the `transposer` device. See the [transposer device](../device/transposer.md) reference for its methods.
