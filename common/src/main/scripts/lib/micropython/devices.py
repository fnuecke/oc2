from oc2 import blob as oc2_blob
from oc2 import ports as oc2_ports
from oc2.blob import Blob, PayloadChannel
from oc2.channel import Channel
from oc2.events import Events

REQUEST_TIMEOUT_MS = 30000
MAX_EVENTS_PER_PUMP = 32

PORT_NAME = "oc2.rpc.0"
BLOB_PORT_NAME = "oc2.blob.0"
EVENT_PORT_NAME = "oc2.event.0"


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


def _copy_device(device):
    copy = {}
    for key, value in device.items():
        copy[key] = list(value) if isinstance(value, list) else value
    return copy


def _copy_devices(devices):
    return [_copy_device(device) for device in devices]


class DeviceBus:
    null = None

    def __init__(self, path, blob_path, event_path):
        self.rpc = None
        self.payload = None
        self.events = None
        self.generation = None
        self.generation_confirmed = False
        self.device_list = None

        try:
            self.rpc = Channel(path)
            self.payload = PayloadChannel(blob_path)
            self.events = Events(event_path)
        except Exception:
            self.close()
            raise

    def close(self):
        if self.rpc:
            self.rpc.close()
        if self.payload:
            self.payload.close()
        if self.events:
            self.events.close()

    def flush(self):
        self.rpc.reset()
        self.payload.reset()

    def _note_generation(self, envelope):
        gen = envelope.get("gen")
        if gen is None:
            raise Exception("host reply carried no bus generation")
        if gen != self.generation:
            self.generation = gen
            self.device_list = None
        self.generation_confirmed = True

    def _apply_event(self, event):
        if event.get("type") == "devicesChanged":
            if event.get("gen") != self.generation:
                self.generation = event.get("gen")
                self.device_list = None
            self.generation_confirmed = True
            return True
        return False

    def pump_events(self):
        count = 0
        for _ in range(MAX_EVENTS_PER_PUMP):
            event = self.events.poll()
            if event is None:
                break
            if self._apply_event(event):
                count += 1
        return count

    def wait_event(self, timeout=None, event_type=None):
        while True:
            event = self.events.wait(timeout)
            if event is None:
                return None
            self._apply_event(event)
            if event_type is None or event.get("type") == event_type:
                return event

    # ------------------------------------------------------------- #

    def _request(self, message, expected):
        self.rpc.write(message)
        reply = self.rpc.read(REQUEST_TIMEOUT_MS)
        if reply is None:
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
            self.payload.write(payload)
            message["blob"] = {"length": len(payload),
                               "checksum": oc2_blob.checksum(payload)}

        return oc2_blob.resolve(self.payload, self._request(message, "result"))


def _open_bus():
    port = oc2_ports.find(PORT_NAME)
    if port is None:
        raise Exception("no virtio port named %s was found" % PORT_NAME)
    blob_port = oc2_ports.find(BLOB_PORT_NAME)
    if blob_port is None:
        raise Exception("no virtio port named %s was found" % BLOB_PORT_NAME)
    event_port = oc2_ports.find(EVENT_PORT_NAME)
    if event_port is None:
        raise Exception("no virtio port named %s was found" % EVENT_PORT_NAME)
    return DeviceBus(port, blob_port, event_port)


bus = _open_bus()
