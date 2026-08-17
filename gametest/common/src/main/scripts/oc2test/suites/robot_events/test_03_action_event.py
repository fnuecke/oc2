import devices

bus = devices.bus()
robot = bus.find("robot")
assert robot is not None, "no robot device on the bus"

robot.move("upward")
action_id = robot.getLastActionId()

event = bus.wait_event(10000, "robotActionCompleted")
assert event is not None, "no robotActionCompleted event arrived within 10s"
assert event.get("type") == "robotActionCompleted", "unexpected event type %s" % event.get("type")

data = event.get("data")
assert data is not None, "event carried no data"
assert data.get("actionId") == action_id, \
    "event was for action %s, expected %s" % (data.get("actionId"), action_id)
assert data.get("result") == "SUCCESS", "action reported %s" % data.get("result")
