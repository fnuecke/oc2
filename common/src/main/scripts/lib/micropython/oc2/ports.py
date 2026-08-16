import ffi
import os

PORTS_PATH = "/sys/class/virtio-ports"


def find(name):
    # This build has no MICROPY_VFS, so os exposes ilistdir and no listdir at all.
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
_O_NONBLOCK = 0o4000


def set_nonblocking(fd):
    """Without this, read(n) blocks until it has all n bytes, so the only safe read is one
    byte at a time. With it a single read returns whatever is there."""
    flags = _fcntl(fd, _F_GETFL, 0)
    if flags < 0 or _fcntl(fd, _F_SETFL, flags | _O_NONBLOCK) < 0:
        raise OSError("could not set O_NONBLOCK on the virtio port")
