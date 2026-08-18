import socket as _socket
import struct

from oc2 import ports

AF_UNIX = 1

# struct sockaddr_un { unsigned short sun_family; char sun_path[108]; }
_SOCKADDR_UN = "H108s"


def address(path):
    if isinstance(path, str):
        path = path.encode()
    if len(path) > 107:
        raise OSError("socket path is too long: %s" % path.decode())
    return struct.pack(_SOCKADDR_UN, AF_UNIX, path)


def connect(path):
    sock = _socket.socket(_socket.AF_UNIX, _socket.SOCK_STREAM)
    try:
        sock.connect(address(path))
    except Exception:
        sock.close()
        raise
    sock.setblocking(False)
    ports.set_cloexec(sock.fileno())
    return sock
