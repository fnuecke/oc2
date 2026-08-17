import time

POLL_INTERVAL_MS = 1000
DEFAULT_TIMEOUT_MS = 30000
ACTION_COMPLETED_EVENT = "robotActionCompleted"

direction = {
    "forward": "forward",
    "backward": "backward",
    "upward": "upward",
    "downward": "downward",
    "left": "left",
    "right": "right",
}


class Robot:
    def __init__(self, bus, device):
        if device is None:
            raise Exception("robot device not found")
        self.bus = bus
        self.device = device

    def energy(self):
        return self.device.getEnergyStored()

    def capacity(self):
        return self.device.getEnergyCapacity()

    def slot(self, value=None):
        if value is not None:
            self.device.setSelectedSlot(value)
        return self.device.getSelectedSlot()

    def stack(self, slot=None):
        return self.device.getStackInSlot(self.slot() if slot is None else slot)

    def move(self, direction, timeout=DEFAULT_TIMEOUT_MS):
        if not self.move_async(direction, timeout):
            return False
        return self._wait_for_last_action(timeout)

    def move_async(self, direction, timeout=DEFAULT_TIMEOUT_MS):
        return self._queue(self.device.move, direction, timeout)

    def turn(self, direction, timeout=DEFAULT_TIMEOUT_MS):
        if not self.turn_async(direction, timeout):
            return False
        return self._wait_for_last_action(timeout)

    def turn_async(self, direction, timeout=DEFAULT_TIMEOUT_MS):
        return self._queue(self.device.turn, direction, timeout)

    def _queue(self, action, direction, timeout):
        if not direction:
            raise Exception("no direction specified")
        deadline = time.time() + timeout / 1000.0
        while not action(direction):
            if time.time() >= deadline:
                return False
            self.bus.wait_event(POLL_INTERVAL_MS, ACTION_COMPLETED_EVENT)
        return True

    def _wait_for_last_action(self, timeout=DEFAULT_TIMEOUT_MS):
        id = self.device.getLastActionId()
        deadline = time.time() + timeout / 1000.0
        result = self.device.getActionResult(id)
        while result and result == "INCOMPLETE":
            if time.time() >= deadline:
                return False
            event = self.bus.wait_event(POLL_INTERVAL_MS, ACTION_COMPLETED_EVENT)
            result = (_completed_action_result(event, id)
                      or self.device.getActionResult(id))
        return result == "SUCCESS"


def _completed_action_result(event, id):
    if event and event["data"]["actionId"] == id:
        return event["data"]["result"]
    return None


def robot(bus):
    return Robot(bus, bus.find("robot"))
