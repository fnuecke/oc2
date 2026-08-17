import sys

from harness import expect, report

state = {
    "clock": 0,
    "events": [],
    "results": [],
    "queue_replies": [],
    "result_calls": 0,
    "wait_calls": 0,
    "queue_calls": 0,
    "last_event_type": None,
    "last_queued": None,
}


class FakeTime:
    def ticks_ms(self):
        return state["clock"]

    def ticks_add(self, ticks, delta):
        return ticks + delta

    def ticks_diff(self, a, b):
        return a - b


sys.modules["time"] = FakeTime()


def pop(key, default=None):
    queue = state[key]
    return queue.pop(0) if queue else default


class FakeDevice:
    def getLastActionId(self):
        return 1

    def getActionResult(self, action_id):
        state["result_calls"] += 1
        return pop("results")

    def move(self, direction):
        return self._queue("move")

    def turn(self, direction):
        return self._queue("turn")

    def _queue(self, kind):
        state["queue_calls"] += 1
        state["last_queued"] = kind
        return pop("queue_replies", True)


class FakeBus:
    def find(self, type_name):
        return FakeDevice() if type_name == "robot" else None

    def wait_event(self, timeout=None, event_type=None):
        state["wait_calls"] += 1
        state["last_event_type"] = event_type
        while True:
            event = pop("events")
            if event is None:
                state["clock"] += timeout  # the wait timed out
                return None
            if event_type is None or event.get("type") == event_type:
                return event


class _Stub:
    pass


fake_devices = _Stub()
sys.modules["devices"] = fake_devices


def reset():
    state["clock"] = 0
    state["events"] = []
    state["results"] = []
    state["queue_replies"] = []
    state["result_calls"] = 0
    state["wait_calls"] = 0
    state["queue_calls"] = 0
    state["last_event_type"] = None
    state["last_queued"] = None
    fake_devices.bus = FakeBus()
    if "robot" in sys.modules:
        del sys.modules["robot"]
    return __import__("robot")


def completed(action_id, result):
    state["events"].append({"type": "robotActionCompleted",
                            "data": {"actionId": action_id, "result": result}})


# Completion event
robot = reset()
state["results"] = ["INCOMPLETE"]
completed(1, "SUCCESS")
expect("move succeeded on the event", robot.move("forward"), True)
expect("only the initial poll was needed", state["result_calls"], 1)
expect("the wait blocked on the event channel", state["wait_calls"], 1)
expect("it asked for the completion event by name",
       state["last_event_type"], "robotActionCompleted")
expect("it queued a move", state["last_queued"], "move")

# Failure event
robot = reset()
state["results"] = ["INCOMPLETE"]
completed(1, "FAILURE")
expect("failure is reported as failure", robot.move("forward"), False)
expect("failure needed no extra poll", state["result_calls"], 1)

# Another action's event
robot = reset()
state["results"] = ["INCOMPLETE", "INCOMPLETE", "SUCCESS"]
completed(99, "FAILURE")
expect("another action's event is ignored", robot.move("forward"), True)
expect("we polled again after the stray event", state["result_calls"], 3)

# Another event type
robot = reset()
state["results"] = ["INCOMPLETE"]
state["events"] = [{"type": "devicesChanged", "gen": 3}]
completed(1, "SUCCESS")
expect("a devicesChanged did not end the wait", robot.move("forward"), True)
expect("it still ended on the completion, with no extra poll", state["result_calls"], 1)

# Timeout falls back to polling
robot = reset()
state["results"] = ["INCOMPLETE", "INCOMPLETE", "SUCCESS"]
expect("polling still completes", robot.move("forward"), True)
expect("one wait per poll", state["wait_calls"], 2)
expect("each timed-out wait cost its full interval", state["clock"], 2000)

# Action timeout
robot = reset()
state["results"] = ["INCOMPLETE"] * 100
expect("a stuck action times out", robot.move("forward", 3000), False)
expect("it gave up within the timeout", state["clock"] <= 4000, True)

# Unknown action id
robot = reset()
expect("an unknown action is not success", robot.move("forward"), False)

# Queue full until a slot frees
robot = reset()
state["queue_replies"] = [False, False, True]
state["results"] = ["SUCCESS"]
completed(1, "SUCCESS")
expect("queueing retried until it fit", robot.move_async("forward"), True)
expect("one wait per rejected attempt", state["wait_calls"], 2)
expect("the queue wait also names the event",
       state["last_event_type"], "robotActionCompleted")
expect("it kept trying until the queue took it", state["queue_calls"], 3)

# Queue timeout
robot = reset()
state["queue_replies"] = [False] * 100
expect("a permanently full queue times out", robot.move_async("forward", 3000), False)

# Turning
robot = reset()
state["results"] = ["INCOMPLETE"]
completed(1, "SUCCESS")
expect("turn waits the same way", robot.turn("left"), True)
expect("turn used the event", state["result_calls"], 1)
expect("turn queued a turn, not a move", state["last_queued"], "turn")

report()
