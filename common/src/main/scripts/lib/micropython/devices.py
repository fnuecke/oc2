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


class Device:
    def __init__(self, device_bus, device_id, type_names=None):
        self.bus = device_bus
        self.device_id = device_id
        self.type_names = type_names or []
        self.methods = None

    def invoke(self, method_name, *args):
        return self.bus.invoke(self.device_id, method_name, *args)

    def __getattr__(self, item):
        if item.startswith("_"):
            raise AttributeError(item)
        if self.methods is None:
            self.methods = self.bus.methods(self.device_id)
        for method in self.methods:
            if method.get("name") == item:
                return lambda *args: self.bus.invoke(self.device_id, item, *args)
        raise AttributeError("device %s has no method %s" % (self.device_id, item))

    def __str__(self):
        if self.methods is None:
            self.methods = self.bus.methods(self.device_id)
        doc = ""
        for method in self.methods:
            doc += method["name"] + "("
            if "parameters" in method:
                i = 0
                for p in method["parameters"]:
                    if i > 0:
                        doc += ", "
                    doc += p["name"] if "name" in p else "arg" + str(i)
                    if "type" in p:
                        doc += ": " + p["type"]
                    i += 1
            doc += ")"
            if "returnType" in method:
                doc += ": " + method["returnType"]
            doc += "\n"

            if "description" in method and method["description"]:
                doc += method["description"] + "\n"

            if "parameters" in method:
                i = 0
                for p in method["parameters"]:
                    if "description" in p:
                        doc += "  "
                        doc += p["name"] if "name" in p else "args" + str(i)
                        doc += "  " + p["description"] + "\n"
                    i += 1
        return doc


def _copy_devices(devices):
    result = []
    for device in devices:
        copy = {}
        for key, value in device.items():
            copy[key] = list(value) if isinstance(value, list) else value
        result.append(copy)
    return result


class DeviceBus:
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
        if "gen" not in envelope:
            raise Exception("host reply carried no bus generation")
        if envelope["gen"] != self.generation:
            self.generation = envelope["gen"]
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
        """Waits for the next event, or for the next one of event_type if given. Events of
        other types are still applied, then skipped; the timeout applies per wait."""
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
            raise Exception("no reply from the host")
        self._note_generation(reply)
        if reply["type"] == expected:
            return reply
        if reply["type"] == "error":
            raise Exception(reply["data"])
        raise Exception("unexpected message type: %s" % reply["type"])

    def _raw_list(self):
        self.pump_events()

        if self.device_list is not None and self.generation_confirmed:
            self.generation_confirmed = False
            return self.device_list

        self.flush()
        self.device_list = self._request({"type": "list"}, "list")["data"]
        return self.device_list

    def list(self):
        return _copy_devices(self._raw_list())

    def _lookup(self, matches):
        for _ in range(2):
            for device in self._raw_list():
                if matches(device):
                    return Device(self, device["deviceId"], device.get("typeNames"))
            if self.device_list is None:
                break  # was not cached, so looking again would return the same thing
            self.device_list = None
        return None

    def get(self, device_id):
        return self._lookup(lambda candidate: candidate["deviceId"] == device_id)

    def find(self, type_name):
        return self._lookup(
            lambda candidate: type_name in (candidate.get("typeNames") or []))

    def methods(self, device_id):
        self.flush()
        return self._request({"type": "methods", "data": device_id}, "methods")["data"]

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


def bus():
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
