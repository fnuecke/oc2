local harness = require("harness")
local expect, report = harness.expect, harness.report

local state = {
  clock = 0,
  events = {},
  results = {},
  queueReplies = {},
  resultCalls = 0,
  waitCalls = 0,
  queueCalls = 0,
  lastEventType = nil,
  lastQueued = nil,
}

package.preload["oc2.clock"] = function()
  local fake = {}
  function fake.ms() return state.clock end
  function fake.deadline(timeout) return state.clock + timeout end
  function fake.remaining(deadline)
    local left = deadline - state.clock
    return left > 0 and left or 0
  end
  function fake.expired(deadline) return deadline - state.clock <= 0 end
  return fake
end

local function pop(key)
  return table.remove(state[key], 1)
end

local function queueAction(kind)
  return function()
    state.queueCalls = state.queueCalls + 1
    state.lastQueued = kind
    local reply = pop("queueReplies")
    return reply == nil or reply
  end
end

local device = {
  getLastActionId = function() return 1 end,
  getActionResult = function()
    state.resultCalls = state.resultCalls + 1
    return pop("results")
  end,
  move = queueAction("move"),
  turn = queueAction("turn"),
}

package.preload["devices"] = function()
  return {
    find = function(_, typeName) return typeName == "robot" and device or nil end,
    waitEvent = function(_, timeout, eventType)
      state.waitCalls = state.waitCalls + 1
      state.lastEventType = eventType
      while true do
        local event = pop("events")
        if not event then
          state.clock = state.clock + timeout -- the wait timed out
          return nil
        end
        if not eventType or event.type == eventType then
          return event
        end
      end
    end,
  }
end

local function reset()
  state.clock = 0
  state.events = {}
  state.results = {}
  state.queueReplies = {}
  state.resultCalls = 0
  state.waitCalls = 0
  state.queueCalls = 0
  state.lastEventType = nil
  state.lastQueued = nil
  package.loaded["robot"] = nil
  return require("robot")
end

local function completed(actionId, result)
  state.events[#state.events + 1] =
      { type = "robotActionCompleted", data = { actionId = actionId, result = result } }
end

-- Completion event
local robot = reset()
state.results = { "INCOMPLETE" }
completed(1, "SUCCESS")
expect("move succeeded on the event", robot.move("forward"), true)
expect("only the initial poll was needed", state.resultCalls, 1)
expect("the wait blocked on the event channel", state.waitCalls, 1)
expect("it asked for the completion event by name", state.lastEventType, "robotActionCompleted")
expect("it queued a move", state.lastQueued, "move")

-- Failure event
robot = reset()
state.results = { "INCOMPLETE" }
completed(1, "FAILURE")
expect("failure is reported as failure", robot.move("forward"), false)
expect("failure needed no extra poll", state.resultCalls, 1)

-- Another action's event
robot = reset()
state.results = { "INCOMPLETE", "INCOMPLETE", "SUCCESS" }
completed(99, "FAILURE")
expect("another action's event is ignored", robot.move("forward"), true)
expect("we polled again after the stray event", state.resultCalls, 3)

-- Another event type
robot = reset()
state.results = { "INCOMPLETE" }
state.events = { { type = "devicesChanged", gen = 3 } }
completed(1, "SUCCESS")
expect("a devicesChanged did not end the wait", robot.move("forward"), true)
expect("it still ended on the completion, with no extra poll", state.resultCalls, 1)

-- Timeout falls back to polling
robot = reset()
state.results = { "INCOMPLETE", "INCOMPLETE", "SUCCESS" }
expect("polling still completes", robot.move("forward"), true)
expect("one wait per poll", state.waitCalls, 2)
expect("each timed-out wait cost its full interval", state.clock, 2000)

-- Action timeout
robot = reset()
for i = 1, 100 do state.results[i] = "INCOMPLETE" end
expect("a stuck action times out", robot.move("forward", 3000), false)
expect("it gave up within the timeout", state.clock <= 4000, true)

-- Unknown action id
robot = reset()
expect("an unknown action is not success", robot.move("forward"), false)

-- Queue full until a slot frees
robot = reset()
state.queueReplies = { false, false, true }
state.results = { "SUCCESS" }
completed(1, "SUCCESS")
expect("queueing retried until it fit", robot.moveAsync("forward"), true)
expect("one wait per rejected attempt", state.waitCalls, 2)
expect("the queue wait also names the event", state.lastEventType, "robotActionCompleted")
expect("it kept trying until the queue took it", state.queueCalls, 3)

-- Queue timeout
robot = reset()
for i = 1, 100 do state.queueReplies[i] = false end
expect("a permanently full queue times out", robot.moveAsync("forward", 3000), false)

-- Turning
robot = reset()
state.results = { "INCOMPLETE" }
completed(1, "SUCCESS")
expect("turn waits the same way", robot.turn("left"), true)
expect("turn used the event", state.resultCalls, 1)
expect("turn queued a turn, not a move", state.lastQueued, "turn")

report()
