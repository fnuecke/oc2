import sys

failures = 0


def _pass(name, detail):
    print("ok   %-52s %s" % (name, detail))


def _fail(name, detail):
    global failures
    print("FAIL %-52s %s" % (name, detail))
    failures += 1


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
    print("\nALL PASS" if failures == 0 else "\n%d FAILURE(S)" % failures)
    sys.exit(0 if failures == 0 else 1)
