#!/bin/sh

# Keeps the bus daemon running.
#
# Usage: oc2busd-supervise.sh <daemon-path> <pid-file>

DAEMON="$1"
PID_FILE="$2"

if [ -z "$DAEMON" ] || [ -z "$PID_FILE" ]; then
    echo "usage: $0 <daemon-path> <pid-file>" >&2
    exit 1
fi

MIN_RUN_SECONDS=10
MAX_DELAY=60

echo $$ >"$PID_FILE"
trap 'rm -f "$PID_FILE"; exit 0' INT TERM
trap 'rm -f "$PID_FILE"' EXIT

uptime_seconds() {
    cut -d. -f1 /proc/uptime
}

delay=1
while true; do
    started=$(uptime_seconds)

    lua "$DAEMON" 2>&1 | logger -t oc2busd

    stopped=$(uptime_seconds)
    if [ $((stopped - started)) -ge "$MIN_RUN_SECONDS" ]; then
        # It ran for a while, so whatever ended it was not a start-up problem.
        delay=1
    else
        # Backs off rather than giving up. The common cause of a fast failure is another
        # process holding the virtio ports, and that clears on its own -- whereas stopping for
        # good would leave the machine on the one-script-at-a-time fallback until it reboots.
        delay=$((delay * 2))
        [ "$delay" -gt "$MAX_DELAY" ] && delay=$MAX_DELAY
        logger -t oc2busd "daemon exited immediately; retrying in ${delay}s"
    fi

    sleep "$delay"
done
