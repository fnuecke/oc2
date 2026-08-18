import json
import os
import sys

NUL = b"\0"

requests = 0
gated = False


class Port:
    def __init__(self, fd, path, name, solicited):
        self.fd = fd
        self.path = path
        self.name = name
        self.solicited = solicited
        self.armed = False
        self.chunks = []
        self.writes = []

    def fileno(self):
        return self.fd

    def readable(self):
        if gated and self.solicited and not self.armed:
            return False
        return bool(self.chunks)

    def read(self, n=-1):
        if not self.readable():
            return b""
        chunk = self.chunks[0]
        if 0 <= n < len(chunk):
            self.chunks[0] = chunk[n:]
            chunk = chunk[:n]
        else:
            self.chunks.pop(0)
        if self is rpc:
            self.armed = False
        return chunk

    def readinto(self, buf):
        chunk = self.read(len(buf))
        if not chunk:
            return 0 if chunk == b"" else None
        buf[:len(chunk)] = chunk
        return len(chunk)

    def write(self, data):
        global requests
        chunk = bytes(data)
        self.writes.append(chunk)
        if self is rpc:
            requests += 1
            _arm(True)
        return len(chunk)

    def close(self):
        pass


rpc = Port(7, "/dev/vport0p0", "oc2.rpc.0", True)
blob = Port(8, "/dev/vport0p1", "oc2.blob.0", True)
event = Port(9, "/dev/vport0p2", "oc2.event.0", False)

_ALL = [rpc, blob, event]
_BY_PATH = {port.path: port for port in _ALL}
_BY_NAME = {port.name: port.path for port in _ALL}


def _arm(value):
    for port in _ALL:
        port.armed = value


class _Stub:
    pass


class _Poll:
    def __init__(self, port):
        self.port = port

    def register(self, *a):
        pass

    def unregister(self, *a):
        pass

    def poll(self, timeout=None):
        return [(self.port.fd, 1)] if self.port.readable() else []


_last_opened = [None]


def _open(path, mode):
    _last_opened[0] = _BY_PATH[path]
    return _last_opened[0]


io_stub = _Stub()
io_stub.open = _open
select_stub = _Stub()
select_stub.POLLIN = 1
select_stub.POLLOUT = 4
select_stub.poll = lambda: _Poll(_last_opened[0])
sys.modules["io"] = io_stub
sys.modules["select"] = select_stub


class _FfiLib:
    def func(self, *a):
        return lambda *args: 0


ffi_stub = _Stub()
ffi_stub.open = lambda name: _FfiLib()
sys.modules["ffi"] = ffi_stub

# The bus prefers the daemon whenever its socket is there. These suites are about the ports, so
# the socket transport is made unavailable outright rather than left to depend on whether a
# daemon happens to be running alongside the test.
os.putenv("OC2_BUS_SOCKET", "none")


def _no_socket(*args, **kwargs):
    raise OSError("the socket transport is stubbed out under the port mock")


socket_stub = _Stub()
socket_stub.connect = _no_socket
socket_stub.address = _no_socket
sys.modules["oc2.socket"] = socket_stub

from oc2 import ports as _oc2_ports  # noqa: E402

_oc2_ports.find = lambda name: _BY_NAME.get(name)


def encode(message):
    return json.dumps(message).encode()


def decode(text):
    return json.loads(text)


def frame(message):
    return NUL + encode(message) + NUL


def feed(port, chunks):
    port.chunks.clear()
    port.chunks.extend(chunks)


def add(port, chunk):
    port.chunks.append(chunk)


def reply(*messages):
    _arm(False)
    feed(rpc, [frame(message) for message in messages])
    for port in _ALL:
        port.writes.clear()
