local robot = require("robot")

assert(robot.move("down", 5000),
  "robot.move() failed, or ignored the completion event and timed out polling")
