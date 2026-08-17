#!/bin/sh

# Guest side of the test channel. Announces itself on the test port, then runs whichever
# suite the host asks for and reports one record per test file. Uses a dedicated port
# to avoid interference with tested things which most likely will use the other ports.
#
# Guest -> host:  ready | case <PASS|FAIL> <name> | detail <line> | end <ran> <failed>
# Host -> guest:  run <suite>

PORT_NAME="oc2.test.0"
SUITES="$(dirname "$(readlink -f "$0")")/suites"

find_port() {
    for entry in /sys/class/virtio-ports/*; do
        [ -r "$entry/name" ] || continue
        if [ "$(cat "$entry/name")" = "$PORT_NAME" ]; then
            echo "/dev/${entry##*/}"
            return 0
        fi
    done
    return 1
}

emit() {
    printf '%s\n' "$*" >&3
}

emit_detail() {
    printf '%s\n' "$1" | while IFS= read -r line; do
        emit "detail $line"
    done
}

run_file() {
    file="$1"
    name="${file##*/}"

    case "$file" in
        *.lua) output=$($TIMEOUT lua "$file" 2>&1) ;;
        *.py) output=$($TIMEOUT micropython "$file" 2>&1) ;;
        *) return 0 ;;
    esac
    status=$?

    ran=$((ran + 1))
    if [ "$status" -eq 0 ]; then
        emit "case PASS $name"
        return 0
    fi

    failed=$((failed + 1))
    emit "case FAIL $name"
    emit_detail "exit status $status"
    if [ -n "$output" ]; then
        emit_detail "$output"
    fi
}

run_suite() {
    suite="$1"
    ran=0
    failed=0

    if [ -z "$suite" ] || [ ! -d "$SUITES/$suite" ]; then
        failed=1
        emit "case FAIL $suite"
        emit_detail "no such suite in $SUITES"
    else
        for file in "$SUITES/$suite"/test_*; do
            [ -f "$file" ] && run_file "$file"
        done
    fi

    emit "end $ran $failed"
}

PORT=$(find_port) || exit 0

TIMEOUT=""
if command -v timeout >/dev/null 2>&1; then
    TIMEOUT="timeout 60"
fi

# The runner is started from init, which has no profile, so manually do what a login shell would.
[ -f /etc/profile.d/lua_path.sh ] && . /etc/profile.d/lua_path.sh
[ -f /etc/profile.d/python_path.sh ] && . /etc/profile.d/python_path.sh

# Test files sit in a suite directory but share the helpers one level up.
LUA_PATH="$SUITES/?.lua${LUA_PATH:+;$LUA_PATH}"
MICROPYPATH="$SUITES${MICROPYPATH:+:$MICROPYPATH}"
export LUA_PATH MICROPYPATH

exec 3<>"$PORT"

emit ready

while IFS= read -r command <&3; do
    set -- $command
    case "${1:-}" in
        run) run_suite "${2:-}" ;;
        *) ;;
    esac
done
