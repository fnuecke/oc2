from devices import bus
from oc2 import clock

POLL_INTERVAL_MS = 1000
DEFAULT_TIMEOUT_MS = 30000
ACTION_COMPLETED_EVENT = "robotActionCompleted"

_robot = bus.find("robot")
if _robot is None:
    raise Exception("robot device not found")

direction = {
    "forward": "forward",
    "backward": "backward",
    "upward": "upward",
    "downward": "downward",
    "left": "left",
    "right": "right",
}


def _deadline_from(timeout):
    return clock.deadline(DEFAULT_TIMEOUT_MS if timeout is None else timeout)


def _completed_action_result(event, action_id):
    if event and event["data"]["actionId"] == action_id:
        return event["data"]["result"]
    return None


def _wait_for_last_action(timeout):
    action_id = _robot.getLastActionId()
    deadline = _deadline_from(timeout)

    result = _robot.getActionResult(action_id)
    while result and result == "INCOMPLETE":
        if clock.expired(deadline):
            return False
        result = (_completed_action_result(
            bus.wait_event(POLL_INTERVAL_MS, ACTION_COMPLETED_EVENT), action_id)
            or _robot.getActionResult(action_id))

    return result == "SUCCESS"


def _queue_action(action, direction, timeout):
    if not direction:
        raise Exception("no direction specified")
    deadline = _deadline_from(timeout)
    while not action(direction):
        if clock.expired(deadline):
            return False
        bus.wait_event(POLL_INTERVAL_MS, ACTION_COMPLETED_EVENT)
    return True


def energy():
    return _robot.getEnergyStored()


def capacity():
    return _robot.getEnergyCapacity()


def slot(value=None):
    if value is not None:
        _robot.setSelectedSlot(value)
    return _robot.getSelectedSlot()


def stack(slot=None):
    if slot is None:
        slot = _robot.getSelectedSlot()
    return _robot.getStackInSlot(slot)


def move(direction, timeout=None):
    if not move_async(direction, timeout):
        return False
    return _wait_for_last_action(timeout)


def move_async(direction, timeout=None):
    return _queue_action(_robot.move, direction, timeout)


def turn(direction, timeout=None):
    if not turn_async(direction, timeout):
        return False
    return _wait_for_last_action(timeout)


def turn_async(direction, timeout=None):
    return _queue_action(_robot.turn, direction, timeout)
