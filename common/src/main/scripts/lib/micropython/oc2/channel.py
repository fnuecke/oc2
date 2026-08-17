import io
import json
import select

from oc2 import clock, ports

DELIMITER = b"\0"
READ_SIZE = 4096
WRITE_TIMEOUT_MS = 10000


class Channel:
    def __init__(self, path, read_only=False):
        self.file = io.open(path, "rb" if read_only else "+b")
        ports.set_nonblocking(self.file.fileno())
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
        self.write_poll = select.poll()
        self.write_poll.register(self.file.fileno(), select.POLLOUT)
        self.buffer = None
        self.buffer_pos = 0
        # A frame split across reads stays here, so a poll that times out mid-frame costs
        # nothing: the next call picks it up where it left off.
        self.parts = []

    def close(self):
        if self.file:
            self.poll.unregister(self.file.fileno())
            self.file.close()
            self.file = None  # closing twice would close whatever reused the number
        self.buffer = None
        self.parts = []

    def reset(self):
        self.buffer = None
        self.buffer_pos = 0
        self.parts = []
        while self.file.read(READ_SIZE):
            pass

    def _fill(self, timeout):
        ready = self.poll.poll() if timeout is None else self.poll.poll(timeout)
        if not ready:
            return False

        revents = ready[0][1]
        if not (revents & select.POLLIN):
            raise OSError("virtio port is not readable (revents=0x%x)" % revents)

        data = self.file.read(READ_SIZE)
        if not data:
            raise OSError("virtio port reported readable but returned no data "
                          "(revents=0x%x)" % revents)

        self.buffer = data
        self.buffer_pos = 0
        return True

    def read(self, timeout=None):
        deadline = None if timeout is None else clock.deadline(timeout)

        while True:
            if self.buffer is None:
                remaining = None if deadline is None else clock.remaining(deadline)
                if not self._fill(remaining):
                    return None  # genuine timeout; self.parts keeps any partial frame

            if not self.parts:
                while self.buffer_pos < len(self.buffer) and self.buffer[self.buffer_pos] == 0:
                    self.buffer_pos += 1

            if self.buffer_pos >= len(self.buffer):
                self.buffer = None
                continue

            stop = self.buffer.find(DELIMITER, self.buffer_pos)
            if stop == -1:
                self.parts.append(self.buffer[self.buffer_pos:])
                self.buffer = None
                continue

            self.parts.append(self.buffer[self.buffer_pos:stop])
            self.buffer_pos = stop + 1
            if self.buffer_pos >= len(self.buffer):
                self.buffer = None

            frame = b"".join(self.parts)
            self.parts = []

            try:
                message = json.loads(frame)
            except ValueError:
                continue
            if isinstance(message, dict):
                return message

    def write(self, data):
        write_all(self.file, self.write_poll,
                  DELIMITER + json.dumps(data).encode() + DELIMITER)


def write_all(file, write_poll, payload):
    view = memoryview(payload)
    offset = 0
    while offset < len(payload):
        written = file.write(view[offset:])
        if not written:
            if not write_poll.poll(WRITE_TIMEOUT_MS):
                raise OSError("timed out writing to the virtio port")
            continue
        offset += written
