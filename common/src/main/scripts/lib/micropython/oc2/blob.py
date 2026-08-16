import io
import select

KEY = "$blob"
MAX_OUTBOUND_SIZE = 512 * 1024
CHUNK_SIZE = 4096
READ_TIMEOUT_MS = 5000
MAX_INBOUND_SIZE = MAX_OUTBOUND_SIZE


class Blob:
    def __init__(self, data):
        self.data = data.encode() if isinstance(data, str) else data


def checksum(data):
    total = 0
    length = len(data)

    whole = length - (length % 4)
    for offset in range(0, whole, 4):
        word = (data[offset] | (data[offset + 1] << 8)
                | (data[offset + 2] << 16) | (data[offset + 3] << 24))
        total = ((total << 1) | (total >> 31)) & 0xFFFFFFFF
        total = (total + word) & 0xFFFFFFFF

    if whole < length:  # zero-padded tail, once
        word = 0
        for i in range(whole, length):
            word |= data[i] << ((i - whole) * 8)
        total = ((total << 1) | (total >> 31)) & 0xFFFFFFFF
        total = (total + word) & 0xFFFFFFFF

    return total - 0x100000000 if total >= 0x80000000 else total


def extract(args):
    payload = None
    parameters = []
    for value in args:
        if isinstance(value, Blob):
            if payload is not None:
                raise Exception("a call may carry at most one binary payload")
            payload = value.data
            parameters.append({KEY: True})
        elif isinstance(value, (bytes, bytearray)):
            # json.dumps would quietly encode these as a string. Binary has to be declared,
            # so it travels on the payload channel and the host sees a byte array.
            raise Exception("wrap binary parameters in bus.blob(...)")
        else:
            parameters.append(value)
    return parameters, payload


def substitute(value, payload, depth=0):
    if depth > 32:
        raise Exception("host reply is nested too deeply")

    if isinstance(value, dict):
        if value.get(KEY):
            return payload
        for key in value:
            value[key] = substitute(value[key], payload, depth + 1)
    elif isinstance(value, list):
        for i in range(len(value)):
            value[i] = substitute(value[i], payload, depth + 1)
    return value


class PayloadChannel:
    def __init__(self, path):
        self.file = io.open(path, "+b")
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)

    def close(self):
        if self.file:
            self.poll.unregister(self.file.fileno())
            self.file.close()
            self.file = None  # closing twice would close whatever reused the number

    def reset(self):
        while len(self.poll.poll(0)) > 0:
            if not self.file.read(1):
                break

    def write(self, data):
        if len(data) > MAX_OUTBOUND_SIZE:
            raise Exception("binary payload of %d bytes exceeds the host limit of %d bytes"
                            % (len(data), MAX_OUTBOUND_SIZE))

        view = memoryview(data)
        offset = 0
        while offset < len(data):
            written = self.file.write(view[offset:offset + CHUNK_SIZE])
            if not written:
                raise Exception("could not write binary payload")
            offset += written
        # Deliberately no flush(): MicroPython maps it to fsync(), virtio_console has no
        # .fsync, and the kernel returns EINVAL -- so flushing here failed every binary
        # call. Nothing is buffered anyway; the fd is written through.

    def read(self, length):
        parts = bytearray()
        while len(parts) < length:
            if not self.poll.poll(READ_TIMEOUT_MS):
                raise Exception("timed out waiting for the rest of the payload")
            chunk = self.file.read(min(length - len(parts), CHUNK_SIZE))
            if not chunk:
                raise Exception("end of file on the binary channel")
            parts.extend(chunk)
        return bytes(parts)


def resolve(channel, message):
    reference = message.get("blob") if message else None
    if not isinstance(reference, dict) or "length" not in reference:
        return message.get("data") if message else None

    if channel is None:
        raise Exception("host sent a binary payload but no data channel was found")

    length = reference["length"]
    if not isinstance(length, int):
        raise Exception("host announced a payload size that is not a number: %s" % (length,))
    if length < 0 or length > MAX_INBOUND_SIZE:
        raise Exception("host announced an implausible payload size: %s" % length)

    data = channel.read(length)
    if checksum(data) != reference.get("checksum"):
        channel.reset()
        raise Exception("binary payload failed its checksum; "
                        "the data channel is corrupt or out of sync")

    return substitute(message.get("data"), data)
