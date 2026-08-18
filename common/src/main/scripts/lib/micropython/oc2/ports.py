import ffi
import os

PORTS_PATH = "/sys/class/virtio-ports"


def find(name):
    try:
        entries = [entry[0] for entry in os.ilistdir(PORTS_PATH)]
    except OSError:
        return None

    for entry in entries:
        try:
            with open("%s/%s/name" % (PORTS_PATH, entry)) as f:
                if f.read().strip() == name:
                    return "/dev/%s" % entry
        except Exception:
            pass
    return None


_libc = ffi.open(None)
_fcntl = _libc.func("i", "fcntl", "iii")
_F_GETFL = 3
_F_SETFL = 4
_F_GETFD = 1
_F_SETFD = 2
_O_NONBLOCK = 0o4000
_FD_CLOEXEC = 1


def set_nonblocking(fd):
    flags = _fcntl(fd, _F_GETFL, 0)
    if flags < 0 or _fcntl(fd, _F_SETFL, flags | _O_NONBLOCK) < 0:
        raise OSError("could not set O_NONBLOCK on the descriptor")


def set_cloexec(fd):
    flags = _fcntl(fd, _F_GETFD, 0)
    if flags >= 0:
        _fcntl(fd, _F_SETFD, flags | _FD_CLOEXEC)
