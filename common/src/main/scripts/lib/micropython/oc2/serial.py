"""Addressed data-frames over a serial port.

Frame format: 01h  to  from  length  payload...  checksum

Usage:

    from oc2 import serial
    line = serial.open()
    line.send(7, b"status?") # to address 7
    sender, data, to = line.receive(5000)
"""

import os
import select

from oc2 import clock, ports
from oc2.channel import write_all

_open = open

SOH = 0x01
MAX_PAYLOAD = 200
BROADCAST = 255
READ_CHUNK = 256


def _checksum(data):
    total = 0
    for byte in data:
        total = (total + byte) % 256
    return total


def _base_address(device):
    try:
        with _open("/sys/class/tty/%s/iomem_base" % device.rsplit("/", 1)[-1]) as f:
            return int(f.read().strip(), 0)
    except OSError:
        return None


def _card_settings(device):
    import devices

    base = _base_address(device)
    if base is None:
        return None, "%s is not a serial port" % device

    for entry in devices.bus.list():
        if "serial" not in (entry.get("typeNames") or []):
            continue
        card = devices.bus.get(entry.get("deviceId"))
        if card.getBaseAddress() == base:
            return (card, card.getAddress()), None

    return None, "no serial interface card behind %s" % device


def encode(to, sender, data):
    if len(data) > MAX_PAYLOAD:
        raise ValueError("payload does not fit a frame")

    header = bytes((to % 256, sender % 256, len(data)))
    return bytes((SOH,)) + header + data + bytes((_checksum(header + data),))


def decode(buffer, address):
    marker = bytes((SOH,))
    pending = None
    start = buffer.find(marker)
    while start >= 0:
        if len(buffer) - start < 4:
            if pending is None:
                pending = start
            break
        length = buffer[start + 3]
        end = start + 5 + length
        if length <= MAX_PAYLOAD:
            if len(buffer) < end:
                if pending is None:
                    pending = start
            elif buffer[end - 1] == _checksum(buffer[start + 1:end - 1]):
                to = buffer[start + 1]
                if to == address or to == BROADCAST:
                    return (buffer[start + 2], buffer[start + 4:end - 1], to), buffer[end:]
                buffer = buffer[end:]
                pending = None
                start = buffer.find(marker)
                continue
        start = buffer.find(marker, start + 1)
    if pending is None:
        return None, b""
    return None, buffer[pending:]


class Line:
    def __init__(self, port, card, address):
        self.port = port
        self.card = card
        self.address = address
        self.buffer = b""
        ports.set_nonblocking(port.fileno())
        ports.set_cloexec(port.fileno())
        self.poller = select.poll()
        self.poller.register(port.fileno(), select.POLLIN)
        self.write_poll = select.poll()
        self.write_poll.register(port.fileno(), select.POLLOUT)

    def close(self):
        self.port.close()

    def send(self, to, data):
        write_all(self.port, self.write_poll, encode(to, self.address, data))

    def broadcast(self, data):
        self.send(BROADCAST, data)

    def baud_rate(self):
        return self.card.getBaudRate()

    def collisions(self):
        return self.card.getTxErrorCount()

    def receive(self, timeout=None):
        deadline = None if timeout is None else clock.deadline(timeout)

        while True:
            frame = self._parse()
            if frame is not None:
                return frame

            if not self._fill(deadline):
                return None, None, None

    def _fill(self, deadline):
        while True:
            wait = None if deadline is None else clock.remaining(deadline)
            if wait is not None and wait <= 0:
                return False

            ready = self.poller.poll() if wait is None else self.poller.poll(wait)
            if not ready:
                return False

            chunk = self.port.read(READ_CHUNK)
            if chunk:
                self.buffer += chunk
                return True
            if chunk is not None:
                return False

    def _parse(self):
        frame, self.buffer = decode(self.buffer, self.address)
        return frame


def open(device="/dev/ttyS1", baud=None):
    settings, reason = _card_settings(device)
    if settings is None:
        raise OSError(reason)

    card, address = settings

    os.system("stty -F %s %s raw -echo -crtscts 2>/dev/null" % (device, baud if baud else ""))

    return Line(_open(device, "r+b"), card, address)
