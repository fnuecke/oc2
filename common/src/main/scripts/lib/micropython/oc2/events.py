from oc2.channel import Channel


class Events:
    def __init__(self, path):
        self.channel = Channel(path)

    def close(self):
        self.channel.close()

    def poll(self):
        return self.channel.read(0)

    def wait(self, timeout=None):
        return self.channel.read(timeout)
