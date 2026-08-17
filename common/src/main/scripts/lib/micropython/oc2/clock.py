import time


def ms():
    return time.ticks_ms()


def deadline(timeout):
    return time.ticks_add(time.ticks_ms(), timeout)


def remaining(deadline):
    left = time.ticks_diff(deadline, time.ticks_ms())
    return left if left > 0 else 0


def expired(deadline):
    return time.ticks_diff(deadline, time.ticks_ms()) <= 0
