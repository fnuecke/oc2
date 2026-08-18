from oc2.bus import Device, DeviceBus, connect

try:
    bus = connect()
except Exception as e:
    raise Exception("could not open the device bus: %s" % e)
