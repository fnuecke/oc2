import io
import os
import select
import json


class Device:
    def __init__(self, device_bus, device_id):
        self.bus = device_bus
        self.device_id = device_id
        self.methods = None

    def __getattr__(self, item):
        return lambda *args: self.bus.invoke(self.device_id, item, *args)

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
                    if "name" in p:
                        doc += p["name"]
                    else:
                        doc += "arg" + str(i)
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
                        if "name" in p:
                            doc += p["name"]
                        else:
                            doc += "args" + str(i)
                        doc += "  " + p["description"] + "\n"
                    i += 1

            if "returnTypeDescription" in method and method["returnTypeDescription"]:
                doc += method["returnTypeDescription"] + "\n"

        return doc


class DeviceBus:
    MESSAGE_DELIMITER = "\0"

    def __init__(self, path):
        self.file = io.open(path, "+b")
        os.system("stty -F %s raw -echo" % path)
        self.poll = select.poll()
        self.poll.register(self.file.fileno(), select.POLLIN)
        self.buffer = None
        self.buffer_pos = 0

    def close(self):
        self.file.close()
        self.buffer = None

    def flush(self):
        self._clear_buffer()
        self._skip_input()

    def list(self):
        self.flush()
        self._write_message({'type': "list"})
        return self._read_message("list")

    def get(self, device_id):
        for device in self.list():
            if device["deviceId"] == device_id:
                return Device(self, device["deviceId"])
        return None

    def find(self, type_name):
        for device in self.list():
            if "typeNames" in device and type_name in device["typeNames"]:
                return Device(self, device["deviceId"])
        return None

    def methods(self, device_id):
        self.flush()
        self._write_message({"type": "methods", "data": device_id})
        return self._read_message("methods")

    def invoke(self, device_id, method_name, *args):
        self.flush()
        self._write_message({"type": "invoke", "data": {
            "deviceId": device_id,
            "name": method_name,
            "parameters": args
        }})
        return self._read_message("result")

    def _write_message(self, data):
        self.file.write(self.MESSAGE_DELIMITER + json.dumps(data) + self.MESSAGE_DELIMITER)

    def _read_message(self, expected_type):
        message = ""
        while True:
            if self.buffer is None:
                self._fill_buffer()

            value = self.buffer.decode()[self.buffer_pos:]

            if len(message) == 0 and value[0] == self.MESSAGE_DELIMITER:
                self.buffer_pos += 1
                value = value[1:]

            if value.find(self.MESSAGE_DELIMITER) != -1:
                value = value[:value.find(self.MESSAGE_DELIMITER) + 1]
                self.buffer_pos += len(value)
                if self.buffer_pos >= len(self.buffer):
                    self._clear_buffer()
            else:
                self._clear_buffer()

            message += value
 
            if message[-1] == self.MESSAGE_DELIMITER:
                data = json.loads(message)
                if data["type"] == expected_type:
                    if "data" in data:
                        return data["data"]
                    else:
                        return
                elif data["type"] == "error":
                    raise Exception(data["data"])
                else:
                    raise Exception("unexpected message type: %s" % data["type"])

    def _clear_buffer(self):
        self.buffer = None
        self.buffer_pos = 0

    def _fill_buffer(self):
        self.poll.poll()  # Blocking wait until we have some data.
        self.buffer = self._read(1024)
        self.buffer_pos = 0

    def _read(self, limit):
        # This is horrible, but don't know how to know how many bytes are available,
        # so reading one by one is necessary to avoid blocking.
        data = bytearray()
        bytesRead = 0
        while bytesRead < limit and len(self.poll.poll(0)) > 0:
            data.extend(self.file.read(1))
            bytesRead += 1
        return data

    def _skip_input(self):
        # This is horrible, but don't know how to know how many bytes are available,
        # so reading one by one is necessary to avoid blocking.
        while len(self.poll.poll(0)) > 0:
            self.file.read(1)


def bus():
    return DeviceBus("/dev/hvc0")
