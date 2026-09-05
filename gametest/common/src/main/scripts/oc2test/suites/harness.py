import sys

failures = 0
checks = 0


def _pass(name, detail):
    global checks
    checks += 1
    print("ok   %-52s %s" % (name, detail))


def _fail(name, detail):
    global checks, failures
    checks += 1
    failures += 1
    print("FAIL %-52s %s" % (name, detail))


def expect(name, got, want):
    if got == want:
        _pass(name, repr(got))
    else:
        _fail(name, "got %r want %r" % (got, want))


def raises(name, needle, fn):
    try:
        result = fn()
    except Exception as e:
        if needle in str(e):
            _pass(name, "raised")
        else:
            _fail(name, "wrong error %s" % e)
        return
    _fail(name, "returned %r" % (result,))


def report():
    print("\nchecks %d" % checks)
    print("ALL PASS" if failures == 0 else "%d FAILURE(S)" % failures)
    sys.exit(0 if failures == 0 else 1)
