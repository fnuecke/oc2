from oc2.channel import Channel


class Events:
    def __init__(self, channel):
        self.channel = channel

    @classmethod
    def from_stream(cls, stream, max_frame=None):
        if max_frame is None:
            return cls(Channel(stream))
        return cls(Channel(stream, max_frame))

    @classmethod
    def open(cls, path):
        return cls(Channel.open(path, read_only=True))

    def close(self):
        self.channel.close()

    def poll(self):
        return self.channel.read(0)

    def wait(self, timeout=None):
        return self.channel.read(timeout)
