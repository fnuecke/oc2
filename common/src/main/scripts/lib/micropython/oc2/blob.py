import binascii
import io
import select

from oc2 import ports
from oc2.channel import read_exactly, write_all

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
    return binascii.crc32(data)


def _encode(value, payloads, indices, depth):
    if isinstance(value, Blob):
        index = indices.get(value)
        if index is None:
            index = len(payloads)
            indices[value] = index
            payloads.append(value.data)
        return {KEY: index}
    if isinstance(value, (bytes, bytearray)):
        # json.dumps would encode these as a string. Binary data must be wrapped, so it is
        # sent on the payload channel and the host receives a byte array.
        raise Exception("wrap binary parameters in bus.blob(...)")
    if isinstance(value, (dict, list, tuple)):
        if depth > MAX_DEPTH:
            raise Exception("parameters are nested too deeply")
        if isinstance(value, dict):
            return {key: _encode(item, payloads, indices, depth + 1) for key, item in value.items()}
        return [_encode(item, payloads, indices, depth + 1) for item in value]
    return value


def extract(args):
    payloads = []
    indices = {}
    parameters = [_encode(value, payloads, indices, 0) for value in args]
    return parameters, payloads


def substitute(value, payloads, depth=0):
    if not isinstance(value, (dict, list)):
        return value

    if depth > MAX_DEPTH:
        raise Exception("host reply is nested too deeply")

    if isinstance(value, dict):
        if KEY in value:
            return payloads[value[KEY]]
        for key in value:
            value[key] = substitute(value[key], payloads, depth + 1)
    else:
        for i in range(len(value)):
            value[i] = substitute(value[i], payloads, depth + 1)
    return value


class PayloadChannel:
    def __init__(self, stream):
        self.file = stream
        ports.set_nonblocking(self.file.fileno())
        ports.set_cloexec(self.file.fileno())
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
        self.write_poll = select.poll()
        self.write_poll.register(self.file.fileno(), select.POLLOUT)

    @classmethod
    def open(cls, path):
        stream = io.open(path, "+b")
        try:
            return cls(stream)
        except Exception:
            stream.close()
            raise

    def close(self):
        if self.file:
            self.poll.unregister(self.file.fileno())
            self.file.close()
            self.file = None  # closing twice would close whatever reused the number

    def reset(self):
        while True:
            data = self.file.read(CHUNK_SIZE)
            if not data:
                break

    def write(self, data):
        if len(data) > MAX_OUTBOUND:
            raise Exception("binary payload of %d bytes exceeds the host limit of %d bytes"
                            % (len(data), MAX_OUTBOUND))

        write_all(self.file, self.write_poll, data)

    def read(self, length, timeout=READ_TIMEOUT_MS):
        return read_exactly(self.file, self.poll, length, timeout)


def length(references):
    total = 0
    for reference in references:
        if reference["length"] < 0:
            return -1
        total += reference["length"]
    return total


def resolve(channel, message):
    references = message.get("blobs")
    if not references:
        return message.get("data")

    total = length(references)
    if total < 0 or total > MAX_INBOUND:
        raise Exception("host announced an implausible payload size: %s" % total)

    payloads = []
    for reference in references:
        try:
            data = channel.read(reference["length"])
        except Exception as e:
            channel.reset()
            raise Exception("could not read binary payload: %s" % e)

        if checksum(data) != reference["checksum"]:
            channel.reset()
            raise Exception("binary payload failed its checksum; "
                            "the data channel is corrupt or out of sync")
        payloads.append(data)

    return substitute(message.get("data"), payloads)
