import io
import json
import select

from oc2 import clock, ports

DELIMITER = b"\0"
READ_SIZE = 4096
WRITE_TIMEOUT_MS = 10000
DEFAULT_MAX_FRAME = 256 * 1024


def frame(message):
    """Encodes a message into the framing used on the wire."""
    return DELIMITER + json.dumps(message).encode() + DELIMITER


class Channel:
    def __init__(self, stream, max_frame=DEFAULT_MAX_FRAME):
        self.file = stream
        ports.set_nonblocking(self.file.fileno())
        ports.set_cloexec(self.file.fileno())
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
        self.write_poll = select.poll()
        self.write_poll.register(self.file.fileno(), select.POLLOUT)
        self.max_frame = max_frame
        self.buffer = None
        self.buffer_pos = 0
        # A frame split across reads stays here, so a poll that times out mid-frame costs
        # nothing: the next call picks it up where it left off.
        self.parts = []
        self.parts_size = 0

    @classmethod
    def open(cls, path, read_only=False, max_frame=DEFAULT_MAX_FRAME):
        stream = io.open(path, "rb" if read_only else "+b")
        try:
            return cls(stream, max_frame)
        except Exception:
            stream.close()
            raise

    def close(self):
        if self.file:
            self.poll.unregister(self.file.fileno())
            self.write_poll.unregister(self.file.fileno())
            self.file.close()
            self.file = None  # closing twice would close whatever reused the number
        self.buffer = None
        self.buffer_pos = 0
        self.parts = []
        self.parts_size = 0

    def reset(self):
        self.buffer = None
        self.buffer_pos = 0
        self.parts = []
        self.parts_size = 0
        while True:
            data = self.file.read(READ_SIZE)
            if not data:
                break

    def _fill(self, timeout):
        deadline = None if timeout is None else clock.deadline(timeout)

        while True:
            remaining = None if deadline is None else clock.remaining(deadline)
            ready = self.poll.poll() if remaining is None else self.poll.poll(remaining)
            if not ready:
                return False

            revents = ready[0][1]
            if not (revents & select.POLLIN):
                raise OSError("channel is not readable (revents=0x%x)" % revents)

            data = self.file.read(READ_SIZE)
            if data:
                self.buffer = data
                self.buffer_pos = 0
                return True
            if data is not None:
                raise OSError("end of file")

            if deadline is not None and clock.remaining(deadline) <= 0:
                return False

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
                tail = self.buffer[self.buffer_pos:]
                self.parts.append(tail)
                self.parts_size += len(tail)
                self.buffer = None
                if self.parts_size > self.max_frame:
                    self.parts = []
                    self.parts_size = 0
                    raise OSError("frame exceeds %d bytes" % self.max_frame)
                continue

            self.parts.append(self.buffer[self.buffer_pos:stop])
            self.buffer_pos = stop + 1
            if self.buffer_pos >= len(self.buffer):
                self.buffer = None

            body = b"".join(self.parts)
            self.parts = []
            self.parts_size = 0

            try:
                message = json.loads(body)
            except ValueError:
                continue
            if isinstance(message, dict):
                return message

    def write(self, data):
        write_all(self.file, self.write_poll, frame(data))

    def write_raw(self, data):
        write_all(self.file, self.write_poll, data)


def read_exactly(file, poll, length, timeout):
    if timeout is None or timeout < 0:
        raise OSError("read_exactly needs a bounded timeout")

    deadline = clock.deadline(timeout)
    buffer = bytearray(length)
    view = memoryview(buffer)
    got = 0
    while got < length:
        if not poll.poll(clock.remaining(deadline)):
            raise OSError("timed out waiting for the rest of the payload")
        count = file.readinto(view[got:got + min(length - got, READ_SIZE)])
        if count is None:
            continue  # readable, but nothing yet
        if not count:
            raise OSError("end of file")
        got += count
    return bytes(buffer)


def write_all(file, write_poll, payload):
    view = memoryview(payload)
    offset = 0
    while offset < len(payload):
        written = file.write(view[offset:])
        if not written:
            ready = write_poll.poll(WRITE_TIMEOUT_MS)
            if not ready:
                raise OSError("timed out writing to the channel")
            # poll reports ERR and HUP whatever events were asked for, so a retry loop that
            # trusts it alone would spin against a peer that has gone away.
            if ready[0][1] & (select.POLLERR | select.POLLHUP):
                raise OSError("the far end of the channel went away")
            continue
        offset += written
