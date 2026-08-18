import os
import time

from oc2 import blob as oc2_blob
from oc2 import channel as oc2_channel
from oc2 import ports as oc2_ports
from oc2 import socket as oc2_socket
from oc2.blob import Blob, PayloadChannel
from oc2.channel import Channel
from oc2.events import Events

SOCKET_PATH = "/run/oc2/bus"
PROTOCOL_VERSION = 1
TOKEN_LENGTH = 32
HELLO_TIMEOUT_MS = 5000
REQUEST_TIMEOUT_MS = 35000
MAX_EVENTS_PER_PUMP = 32
CONNECT_ATTEMPTS = 5
CONNECT_RETRY_MS = 200
MAX_REPLY_SIZE = oc2_channel.DEFAULT_MAX_FRAME

RPC_PORT_NAME = "oc2.rpc.0"
BLOB_PORT_NAME = "oc2.blob.0"
EVENT_PORT_NAME = "oc2.event.0"

ROLE_CODE = {"rpc": b"R", "blob": b"B", "event": b"E"}
DEFAULT_ROLES = ("rpc", "blob", "event")


# Device

def _parameter_name(parameter, index):
    return parameter.get("name") or ("arg" + str(index))


class Device:
    def __init__(self, bus, device):
        self.bus = bus
        self.device_id = device["deviceId"]
        self.type_names = list(device.get("typeNames") or [])
        self._methods = None

    def _method_list(self):
        if self._methods is None:
            self._methods = self.bus.methods(self.device_id)
        return self._methods

    def invoke(self, method_name, *args):
        return self.bus.invoke(self.device_id, method_name, *args)

    def __getattr__(self, item):
        if item.startswith("_"):
            raise AttributeError(item)
        for method in self._method_list():
            if method.get("name") == item:
                return lambda *args: self.bus.invoke(self.device_id, item, *args)
        raise AttributeError("device %s has no method %s" % (self.device_id, item))

    def __str__(self):
        out = []
        for method in self._method_list():
            out.append(method["name"])
            out.append("(")
            parameters = method.get("parameters") or []
            for i, p in enumerate(parameters):
                if i > 0:
                    out.append(", ")
                out.append(_parameter_name(p, i + 1))
                if p.get("type"):
                    out.append(": ")
                    out.append(p["type"])
            out.append(")")
            if method.get("returnType"):
                out.append(": ")
                out.append(method["returnType"])
            out.append("\n")

            if method.get("description"):
                out.append(method["description"])
                out.append("\n")

            for i, p in enumerate(parameters):
                if p.get("description"):
                    out.append("  ")
                    out.append(_parameter_name(p, i + 1))
                    out.append("  ")
                    out.append(p["description"])
                    out.append("\n")
        return "".join(out)


# Bus

def _copy_device(device):
    copy = {}
    for key, value in device.items():
        copy[key] = list(value) if isinstance(value, list) else value
    return copy


def _copy_devices(devices):
    return [_copy_device(device) for device in devices]


class DeviceBus:
    null = None

    def __init__(self, transport, rpc, payload, events):
        self.transport = transport
        self.rpc = rpc
        self.payload = payload
        self.events = events
        self.generation = None
        self.generation_confirmed = False
        self.device_list = None

    def close(self):
        if self.rpc:
            self.rpc.close()
        if self.payload:
            self.payload.close()
        if self.events:
            self.events.close()

    def flush(self):
        if self.transport != "ports":
            return
        self.rpc.reset()
        self.payload.reset()

    def _note_generation(self, envelope):
        gen = envelope.get("gen")
        if gen is None:
            if envelope.get("type") == "error":
                return
            raise Exception("host reply carried no bus generation")
        if gen != self.generation:
            self.generation = gen
            self.device_list = None
        self.generation_confirmed = True

    def _apply_event(self, event):
        if event.get("type") == "eventsDropped":
            self.device_list = None
            self.generation_confirmed = False
            return True

        if event.get("type") == "devicesChanged":
            if event.get("gen") != self.generation:
                self.generation = event.get("gen")
                self.device_list = None
            self.generation_confirmed = True
            return True
        return False

    def pump_events(self):
        if self.events is None:
            return 0

        count = 0
        for _ in range(MAX_EVENTS_PER_PUMP):
            event = self.events.poll()
            if event is None:
                break
            if self._apply_event(event):
                count += 1
        return count

    def wait_event(self, timeout=None, event_type=None):
        if self.events is None:
            raise Exception("this session has no event channel")

        while True:
            event = self.events.wait(timeout)
            if event is None:
                return None
            self._apply_event(event)
            if event_type is None or event.get("type") == event_type:
                return event

    def _request(self, message, expected):
        self.rpc.write(message)
        reply = self.rpc.read(REQUEST_TIMEOUT_MS)
        if reply is None:
            self.rpc.reset()
            if self.payload is not None:
                self.payload.reset()
            raise Exception("no reply from the host: timeout")
        self._note_generation(reply)
        if reply.get("type") == expected:
            return reply
        if reply.get("type") == "error":
            raise Exception(reply.get("data")
                            or "the host reported an error with no detail")
        raise Exception("unexpected message type: %s" % reply.get("type"))

    def _raw_list(self):
        self.pump_events()

        if self.device_list is not None and self.generation_confirmed:
            self.generation_confirmed = False
            return self.device_list

        self.flush()
        self.device_list = self._request({"type": "list"}, "list").get("data")
        return self.device_list

    def list(self):
        return _copy_devices(self._raw_list())

    def _lookup(self, matches):
        for _ in range(2):
            for device in self._raw_list():
                if matches(device):
                    return Device(self, _copy_device(device))
            if self.device_list is None:
                break  # was not cached, so looking again would return the same thing
            self.device_list = None
        return None

    def get(self, device_id):
        return self._lookup(lambda candidate: candidate.get("deviceId") == device_id)

    def find(self, type_name):
        return self._lookup(
            lambda candidate: type_name in (candidate.get("typeNames") or []))

    def methods(self, device_id):
        self.flush()
        return self._request({"type": "methods", "data": device_id}, "methods").get("data")

    def blob(self, data):
        return Blob(data)

    def invoke(self, device_id, method_name, *args):
        self.flush()

        parameters, payload = oc2_blob.extract(args)
        message = {"type": "invoke", "data": {
            "deviceId": device_id,
            "name": method_name,
            "parameters": parameters
        }}

        if payload is not None:
            if self.payload is None:
                raise Exception("this session has no payload channel; "
                                "open one with the blob role")
            self.payload.write(payload)
            message["blob"] = {"length": len(payload),
                               "checksum": oc2_blob.checksum(payload)}

        reply = self._request(message, "result")
        if reply.get("blob") is not None and self.payload is None:
            raise Exception("the host sent a payload, but this session has no payload channel")
        return oc2_blob.resolve(self.payload, reply)


# Transports

def _connect_ports():
    rpc = payload = events = None
    try:
        paths = {}
        for name in (RPC_PORT_NAME, BLOB_PORT_NAME, EVENT_PORT_NAME):
            paths[name] = oc2_ports.find(name)
            if paths[name] is None:
                raise Exception("no virtio port named %s was found" % name)

        rpc = Channel.open(paths[RPC_PORT_NAME])
        payload = PayloadChannel.open(paths[BLOB_PORT_NAME])
        events = Events.open(paths[EVENT_PORT_NAME])
    except Exception:
        for handle in (rpc, payload, events):
            if handle is not None:
                handle.close()
        raise

    return DeviceBus("ports", rpc, payload, events)


def _connect_socket(path, roles):
    for role in roles:
        if role not in ROLE_CODE:
            raise Exception("unknown role %s" % role)

    opened = []
    try:
        rpc_socket = oc2_socket.connect(path)
        opened.append(rpc_socket)

        rpc = Channel(rpc_socket, MAX_REPLY_SIZE)
        rpc.write_raw(ROLE_CODE["rpc"])
        rpc.write({"type": "hello", "version": PROTOCOL_VERSION, "roles": list(roles)})

        hello = rpc.read(HELLO_TIMEOUT_MS)
        if hello is None:
            raise Exception("the bus daemon did not answer")
        if hello.get("type") != "hello":
            raise Exception("the bus daemon refused the session: %s"
                            % (hello.get("data") or hello.get("type")))
        token = hello.get("token")
        if not isinstance(token, str) or len(token) != TOKEN_LENGTH:
            raise Exception("the bus daemon sent an unusable session token")

        token = token.encode()
        payload = events = None
        for role in roles:
            if role == "rpc":
                continue
            sock = oc2_socket.connect(path)
            opened.append(sock)
            attach = ROLE_CODE[role] + token
            if role == "blob":
                payload = PayloadChannel(sock)
                payload.write(attach)
            else:
                events = Events.from_stream(sock, MAX_REPLY_SIZE)
                events.channel.write_raw(attach)
    except Exception:
        for sock in opened:
            try:
                sock.close()
            except Exception:
                pass
        raise

    bus = DeviceBus("socket", rpc, payload, events)
    bus.generation = hello.get("gen")
    return bus


def connect(socket_path=None, roles=DEFAULT_ROLES):
    path = socket_path or os.getenv("OC2_BUS_SOCKET") or SOCKET_PATH
    use_socket = path not in ("none", "")

    socket_error = None
    direct_error = None

    for attempt in range(CONNECT_ATTEMPTS):
        if use_socket:
            try:
                return _connect_socket(path, roles)
            except Exception as e:
                socket_error = e

        try:
            return _connect_ports()
        except Exception as e:
            direct_error = e

        if attempt < CONNECT_ATTEMPTS - 1:
            time.sleep(CONNECT_RETRY_MS / 1000)

    if socket_error is not None:
        raise Exception("could not reach the bus daemon (%s), nor the ports directly (%s)"
                        % (socket_error, direct_error))
    raise direct_error
