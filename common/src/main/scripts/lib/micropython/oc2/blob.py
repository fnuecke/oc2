import io
import select

from oc2 import ports
from oc2.channel import write_all

KEY = "$blob"
MAX_OUTBOUND = 512 * 1024
MAX_INBOUND = MAX_OUTBOUND
CHUNK_SIZE = 32 * 1024
READ_TIMEOUT_MS = 5000
MAX_DEPTH = 32


class Blob:
    def __init__(self, data):
        if isinstance(data, str):
            data = data.encode()
        elif not isinstance(data, (bytes, bytearray)):
            raise Exception("a binary payload must be bytes or str, got %s"
                            % type(data).__name__)
        self.data = data


def wrap(data):
    return Blob(data)


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
    if not isinstance(value, (dict, list)):
        return value

    if depth > MAX_DEPTH:
        raise Exception("host reply is nested too deeply")

    if isinstance(value, dict):
        if value.get(KEY):
            return payload
        for key in value:
            value[key] = substitute(value[key], payload, depth + 1)
    else:
        for i in range(len(value)):
            value[i] = substitute(value[i], payload, depth + 1)
    return value


class PayloadChannel:
    def __init__(self, path):
        self.file = io.open(path, "+b")
        ports.set_nonblocking(self.file.fileno())
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
        self.write_poll = select.poll()
        self.write_poll.register(self.file.fileno(), select.POLLOUT)

    def close(self):
        if self.file:
            self.poll.unregister(self.file.fileno())
            self.file.close()
            self.file = None  # closing twice would close whatever reused the number

    def reset(self):
        while self.file.read(CHUNK_SIZE):
            pass

    def write(self, data):
        if len(data) > MAX_OUTBOUND:
            raise Exception("binary payload of %d bytes exceeds the host limit of %d bytes"
                            % (len(data), MAX_OUTBOUND))

        write_all(self.file, self.write_poll, data)

    def read(self, length):
        parts = bytearray()
        while len(parts) < length:
            if not self.poll.poll(READ_TIMEOUT_MS):
                raise Exception("timed out waiting for the rest of the payload")
            chunk = self.file.read(min(length - len(parts), CHUNK_SIZE))
            if chunk is None:
                continue  # readable, but nothing yet
            if not chunk:
                raise Exception("end of file")
            parts.extend(chunk)
        return bytes(parts)


def resolve(channel, message):
    reference = message.get("blob")
    if reference is None:
        return message.get("data")

    length = reference["length"]
    if length < 0 or length > MAX_INBOUND:
        raise Exception("host announced an implausible payload size: %s" % length)

    try:
        data = channel.read(length)
    except Exception as e:
        channel.reset()
        raise Exception("could not read binary payload: %s" % e)

    if checksum(data) != reference.get("checksum"):
        channel.reset()
        raise Exception("binary payload failed its checksum; "
                        "the data channel is corrupt or out of sync")

    return substitute(message.get("data"), data)
