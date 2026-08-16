import io
import json
import select

from time import ticks_ms, ticks_add, ticks_diff

DELIMITER = b"\0"
READ_SIZE = 4096


class Channel:
    def __init__(self, path):
        self.file = io.open(path, "+b")
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
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
        # One byte at a time is genuinely the only safe way here: MicroPython's read(n)
        # blocks until it has n bytes or hits EOF (py/stream.c), there is no read1, and
        # this build exposes no way to set O_NONBLOCK. Do not "optimise" this into a
        # larger read -- it will hang.
        while self.poll.poll(0):
            if not self.file.read(1):
                break

    def _fill(self, timeout):
        ready = self.poll.poll() if timeout is None else self.poll.poll(timeout)
        if not ready:
            return False

        revents = ready[0][1]
        if not (revents & select.POLLIN):
            raise OSError("virtio port is not readable (revents=0x%x)" % revents)

        data = bytearray()
        while len(data) < READ_SIZE and self.poll.poll(0):
            chunk = self.file.read(1)
            if not chunk:
                break
            data.extend(chunk)

        if not data:
            raise OSError("virtio port reported readable but returned no data "
                          "(revents=0x%x)" % revents)

        self.buffer = bytes(data)
        self.buffer_pos = 0
        return True

    def read(self, timeout=None):
        deadline = None if timeout is None else ticks_add(ticks_ms(), timeout)

        while True:
            if self.buffer is None:
                remaining = None
                if deadline is not None:
                    remaining = ticks_diff(deadline, ticks_ms())
                    if remaining < 0:
                        remaining = 0
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
                return json.loads(frame)
            except ValueError:
                pass

    def write(self, data):
        self.file.write(DELIMITER + json.dumps(data).encode() + DELIMITER)
