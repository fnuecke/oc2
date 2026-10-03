import time

import devices
import harness

scanner = devices.bus.find("scanner")
assert scanner is not None, "no scanner device on the bus"

scan = None
for _ in range(30):
    try:
        scan = scanner.scan()
        break
    except Exception:
        time.sleep(1)

harness.expect("a scan succeeds once recharged", scan is not None, True)
if scan is None:
    harness.report()
harness.expect("hardness arrives as binary", isinstance(scan["hardness"], (bytes, bytearray)), True)
harness.expect("hardness covers the cube", len(scan["hardness"]), 343)
harness.expect("the robot's own space is air", scan["hardness"][3 * 49 + 3 * 7 + 3], 0)
harness.expect("entities arrive as a list", isinstance(scan["entities"], list), True)
harness.expect("the sky is a yes or no", isinstance(scanner.canSeeSky(), bool), True)

harness.report()
