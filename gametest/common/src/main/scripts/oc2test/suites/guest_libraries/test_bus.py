"""The MicroPython client against a real daemon, over real sockets."""

import os
import sys
import time

from harness import expect, raises, report

from oc2 import bus as oc2_bus

PATH = "/tmp/oc2test-bus-py"
SUITES = os.getenv("OC2TEST_SUITES") or "/mnt/builtin/../suites"


def _start_daemon():
    os.system("lua %s/fake_daemon.lua %s 30 >/dev/null 2>&1 &" % (SUITES, PATH))

    for _ in range(100):
        try:
            os.stat(PATH)
            return True
        except OSError:
            time.sleep(0.1)
    return False


if not _start_daemon():
    print("FAIL the fake daemon never created its socket at %s" % PATH)
    print("\n1 FAILURE(S)")
    sys.exit(1)

first = None
for _ in range(20):
    try:
        first = oc2_bus.connect(socket_path=PATH)
        break
    except Exception:
        time.sleep(0.1)

expect("the client reached the daemon", first is not None, True)
if first is None:
    report()

expect("it chose the socket transport", first.transport, "socket")
expect("and learned the generation from the handshake", first.generation, 1)

devices = first.list()
expect("list came back through the daemon", len(devices), 2)

# Over a socket the daemon holds the one list for the whole machine, so a session that keeps a
# copy of its own would be the only thing able to go stale.
expect("and the session kept no list of its own", first.device_list, None)
expect("asking again still answers", len(first.list()), 2)
expect("with the host's devices", devices[0]["deviceId"], "redstone-1")

redstone = first.find("redstone")
expect("find located a device by type", redstone and redstone.device_id, "redstone-1")
expect("and invoking a method returns the host's answer", redstone.getRedstoneOutput(), 15)

raises("a host error surfaces as an error", "the host said no",
       lambda: first.invoke("redstone-1", "explode"))

payload = b"the quick brown fox"
expect("a payload round-trips through the daemon",
       first.invoke("redstone-1", "echoBlob", first.blob(payload)), payload)

second = oc2_bus.connect(socket_path=PATH)
expect("a second session opens alongside the first", second.transport, "socket")
expect("and works independently", second.find("robot") is not None, True)

first.invoke("redstone-1", "bumpGeneration")
expect("the invoking session sees the event",
       (first.wait_event(2000, "devicesChanged") or {}).get("type"), "devicesChanged")
expect("so does the other one",
       (second.wait_event(2000, "devicesChanged") or {}).get("type"), "devicesChanged")
expect("the list still resolves after the change", len(first.list()), 2)

quiet = oc2_bus.connect(socket_path=PATH, roles=("rpc",))
expect("an rpc-only session works", len(quiet.list()), 2)

# A session that declined a role must say so rather than reaching for a channel it never opened.
raises("a payload needs a payload channel", "no payload channel",
       lambda: quiet.invoke("redstone-1", "echoBlob", quiet.blob(b"x")))
raises("waiting on a session with no event channel says so", "no event channel",
       lambda: quiet.wait_event(10))
expect("pumping one is simply a no-op", quiet.pump_events(), 0)

# flush() is deliberately inert on a socket: the session is nobody else's, and resetting would
# throw away a reply the daemon has already written.
first.flush()
expect("flush does not disturb a socket session", first.find("redstone") is not None, True)
expect("the first session is unaffected by the second", len(first.list()), 2)

# Every failed connect has to give its descriptors back, or a script that retries in a loop runs
# the machine out of them.
def _open_descriptors():
    return len([entry for entry in os.ilistdir("/proc/self/fd")])


oc2_bus.CONNECT_ATTEMPTS = 1  # no point sitting through the startup-race retries here
before_failures = _open_descriptors()
for _ in range(5):
    try:
        oc2_bus.connect(socket_path="/tmp/oc2test-bus-py-nothing")
    except Exception:
        pass
expect("a failed connect leaks no descriptors", _open_descriptors(), before_failures)

first.close()
second.close()
quiet.close()

# The ports are stubbed out rather than left to whether something else happens to hold them --
# and so that a failure here cannot take the machine-wide ports for itself mid-suite.
oc2_bus.oc2_ports.find = lambda name: None

raises("connecting with nothing available fails", "nor the ports",
       lambda: oc2_bus.connect(socket_path="/tmp/oc2test-bus-py-absent"))

try:
    os.remove(PATH)
except OSError:
    pass

report()
