import sys

from harness import expect, raises, report

import devices

bus = devices.bus

# Discovery
redstone = bus.find("redstone")
if redstone is None:
    print("FAIL no redstone card on the bus")
    print("\n1 FAILURE(S)")
    sys.exit(1)
expect("redstone card found on the bus", redstone.device_id is not None, True)

# Metadata, built from the host's annotations rather than from our own constants
names = [method["name"] for method in bus.methods(redstone.device_id)]
expect("host lists setRedstoneOutput", "setRedstoneOutput" in names, True)
expect("host lists getRedstoneOutput", "getRedstoneOutput" in names, True)

# Both dispatch paths: the setter is synchronized onto the server thread, the getter
# answers straight from the VM thread.
redstone.setRedstoneOutput("up", 15)
expect("value set on the server thread reads back", redstone.getRedstoneOutput("up"), 15)
redstone.setRedstoneOutput("up", 0)
expect("and can be cleared again", redstone.getRedstoneOutput("up"), 0)

# Errors
raises("unknown method is refused", "unknown method",
       lambda: redstone.invoke("noSuchMethodExists"))
raises("unknown device is refused", "unknown device",
       lambda: bus.invoke("00000000-0000-0000-0000-000000000000",
                          "getRedstoneOutput", "up"))

# Overflow: more than the host's 4 KiB message limit has to come back as an error rather
# than as silence.
raises("oversized message is refused", "message too large",
       lambda: redstone.invoke("setRedstoneOutput", "up", "x" * (8 * 1024)))
expect("bus still usable after an oversized message", redstone.getRedstoneOutput("up"), 0)

# Recovery
expect("device list still resolves", bus.find("redstone") is not None, True)

report()
