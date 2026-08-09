import time

direction = {
    "forward": "forward",
    "backward": "backward",
    "upward": "upward",
    "downward": "downward",
    "left": "left",
    "right": "right",
}


class Robot:
    def __init__(self, device):
        if device is None:
            raise Exception("robot device not found")
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
        return self.device.getStackInSlot(slot or self.slot())

    def move(self, direction):
        self.move_async(direction)
        return self._wait_for_last_action()

    def move_async(self, direction):
        if not direction:
            raise Exception("no direction specified")
        while not self.device.move(direction):
            time.sleep(1)

    def turn(self, direction):
        self.turn_async(direction)
        return self._wait_for_last_action()

    def turn_async(self, direction):
        if not direction:
            raise Exception("no direction specified")
        while not self.device.turn(direction):
            time.sleep(1)

    def _wait_for_last_action(self):
        id = self.device.getLastActionId()
        result = self.device.getActionResult(id)
        while result and result == "INCOMPLETE":
            time.sleep(1)
            result = self.device.getActionResult(id)
        return result == "SUCCESS"


def robot(bus):
    return Robot(bus.find("robot"))
