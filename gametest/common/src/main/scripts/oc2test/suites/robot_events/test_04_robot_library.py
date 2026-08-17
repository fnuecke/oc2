import robot

assert robot.move("downward", 5000), \
    "robot.move() failed, or ignored the completion event and timed out polling"
