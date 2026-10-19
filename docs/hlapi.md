# HLAPI Wire Protocol

The high-level API (HLAPI) is a JSON based RPC protocol to allow RISC-V guests to discover and use devices on the bus.
This document describes it from the guest's side. The Lua and MicroPython `devices` libraries shipped with the mod are
documented in the [manual](../common/src/main/resources/assets/oc2/doc/en_us/hlapi.md), device methods and events in
the [device pages](../common/src/generated/resources/assets/oc2/doc/en_us/device/index.md).

## Transport

The high-level API uses a single virtio console device with three ports:

| Port          | Direction     | Content                                     |
|---------------|---------------|---------------------------------------------|
| `oc2.rpc.0`   | both          | Requests and replies                        |
| `oc2.blob.0`  | both          | Binary payloads                             |
| `oc2.event.0` | host to guest | Events. Writes from the guest are discarded |

The device address and the names of the device nodes may change, look up ports by name. On Linux, each entry in
`/sys/class/virtio-ports` has a `name` file, the device node is `/dev/<entry>`. The terminal is a separate UART.

Note that on Linux a port can only be opened by one process at a time. Data arriving on a port that has not been opened
by a process is discarded. In particular, events sent while `oc2.event.0` is not opened are lost.

If `oc2.rpc.0` or `oc2.blob.0` is open but not read from, the host cannot send the pending reply. It reads no further
requests until it can. The machine keeps running. If `oc2.event.0` is open but not read from, events are queued and
eventually dropped, see [Events](#events).

If you intend to run code directly on the bare metal, you'll need to bring your own virtio driver.

## Message Framing

`oc2.rpc.0` and `oc2.event.0` transport UTF-8 JSON frames, separated by NUL bytes. The host writes each frame as NUL,
JSON, NUL. This ensures resynchronization if for any reason some message is incomplete or leaves the channel in a broken
state (e.g. when a program stops while reading or writing a frame). As such, two NULs between frames are normal, guests
must ignore empty frames. A frame that fails to parse should be dropped. Each request must end with a NUL. A leading NUL
is recommended. The host ignores empty frames as well.

`oc2.blob.0` is an unframed byte stream. The `blobs` field of a message describes the bytes being sent. This relies on
only a single request being in flight per direction at a time.

## Request Flow

Requests are handled one at a time, in the order they were sent. The host reads the next request only after it has sent
the reply to the previous one, including associated binary payloads, if any. Until then, further requests stay in the
port buffer. Keep only one request in flight at a time, since the payloads of queued requests would get mixed up on the
blob port, see above.

Most methods run on the server thread, so their reply is sent in the next server tick, i.e. after up to 50 ms. Methods
declared as unsynchronized reply within the same VM step. The `methods` reply does not provide this information, and
from the view of the guest, this is considered an implementation detail. Don't rely on this property being stable.

The host never times out a request. When the machine stops or resets, requests in flight, unsent replies and queued
events are discarded. Saving and loading the world preserves them. Loading also rebuilds the bus,
see [Generation](#generation).

Before sending a request, a guest should discard any stale data on `oc2.rpc.0` and `oc2.blob.0`. It should also ignore
replies with a non-zero `id` that does not match its request, for example a late reply to a request that timed out on
the guest's side. The shipped libraries do both, with a timeout of 35 seconds.

## Message Format

Every message is a JSON object. Fields without a value are omitted.

| Field      | Type    | Present                                                                     |
|------------|---------|-----------------------------------------------------------------------------|
| `type`     | string  | Always.                                                                     |
| `id`       | integer | Always in host messages. Optional in requests, defaults to `0`.             |
| `gen`      | integer | Always in host messages. Ignored in requests.                               |
| `deviceId` | string  | Device events only. A UUID.                                                 |
| `data`     | any     | If the message has a value.                                                 |
| `blobs`    | array   | If payloads belong to the message. See [Binary Payloads](#binary-payloads). |

The order of fields is undefined.

`id` is a 32-bit integer chosen by the client, and returned in the reply. Use positive values. Errors for an oversized
frame, invalid JSON, an invalid message or a payload error have `id` set to `0`, since the host cannot associate them
with a request. They still belong to the request in flight. Events have `id` set to `0` as well.

## Requests

Requests are sent from guest to host on `oc2.rpc.0`.

### `list`

Lists the devices on the bus. It has no `data`.

Example request:

```json
{
    "type": "list",
    "id": 7
}
```

Example response:

```json
{
    "type": "list",
    "id": 7,
    "gen": 1,
    "data": [
        {
            "deviceId": "5b0e39c6-6a1a-4c3c-9d8c-3c1f8e7b2a10",
            "typeNames": [
                "redstone"
            ]
        }
    ]
}
```

`typeNames` includes names set by the player, e.g. on bus interfaces. Devices without methods are not listed. The order
of devices and names is undefined.

### `methods`

Lists the methods of a device. `data` is the device id.

Example request:

```json
{
    "type": "methods",
    "id": 8,
    "data": "5b0e39c6-6a1a-4c3c-9d8c-3c1f8e7b2a10"
}
```

Example response:

```json
{
    "type": "methods",
    "id": 8,
    "gen": 1,
    "data": [
        {
            "name": "setRedstoneOutput",
            "returnType": "void",
            "description": "Sets the ...",
            "parameters": [
                {
                    "name": "side",
                    "description": "the side, ...",
                    "type": "Side"
                },
                {
                    "name": "value",
                    "description": "the signal ...",
                    "type": "int"
                }
            ]
        }
    ]
}
```

There is one entry per overload, so a name may occur more than once. `returnType` and parameter `type` are Java class
names, for information only. `byte[]` marks a [payload](#binary-payloads) parameter. `description`,
`returnValueDescription` and the parameters' `name` and `description` are optional. A method without declared overloads
is listed with the parameter name `"..."`: `{"name":"foo","parameters":[{"name":"..."}]}`.

### `invoke`

Calls a method. `data` contains `deviceId`, `name` and `parameters`, an array of positional parameters. If
`parameters` is missing or not an array, it is treated as an empty array.

Example request:

```json
{
    "type": "invoke",
    "id": 9,
    "data": {
        "deviceId": "5b0e39c6-6a1a-4c3c-9d8c-3c1f8e7b2a10",
        "name": "getRedstoneInput",
        "parameters": [
            "north"
        ]
    }
}
```

Example response:

```json
{
    "type": "result",
    "id": 9,
    "gen": 1,
    "data": 15
}
```

The reply is a `result`, with the return value in `data`. `data` is omitted if the method returns nothing or `null`.
Alternatively, any request may result in an `error`, see [Errors](#errors).

## Events

Events are sent from host to guest on `oc2.event.0`, with the same framing. They are independent of requests.

- Device events have the event name as `type`, the sender's `deviceId` and optional `data`. Events are device specific,
  they're usually documented in the device's API page in the manual. An example event:
  ```json
  {
      "type": "robotActionCompleted",
      "id": 0,
      "gen": 1,
      "deviceId": "5b0e39c6-6a1a-4c3c-9d8c-3c1f8e7b2a10",
      "data": {
          "actionId": 7,
          "result": "SUCCESS"
      }
  }
  ```
- `devicesChanged` is sent each time the bus was rebuilt, even if the set of devices did not change. It has no `data`.
  ```json
  {
      "type": "devicesChanged",
      "id": 0,
      "gen": 2
  }
  ```
- `eventsDropped` reports events were dropped due to overflow. `data` is the number of events dropped since the report.
  ```json
  {
      "type": "eventsDropped",
      "id": 0,
      "gen": 2,
      "data": 12
  }
  ```

The host queues at most 8 KiB of framed events. An event that does not fit is dropped. The following `eventsDropped`
is queued regardless of the limit. At most one `eventsDropped` is queued at a time, events dropped after it are counted
in the next one. Note that a dropped event may have been a `devicesChanged`.

## Bus Generation

`gen` is a counter that is incremented each time the bus is rebuilt. It is `0` when the machine starts, and is saved
with the machine. Loading the world, or the chunk the machine is in, rebuilds the bus, so `gen` increases and a
`devicesChanged` is sent.

A device id survives rebuilds, reboots and world reloads. It only changes when devices are added to or removed from the
block face or item providing the device. A client may cache the `list` reply, and the `methods` replies of its devices,
as long as `gen` does not change. After an `eventsDropped`, the cache should be considered outdated until the next host
message (which will establish whether generation changed).

## Binary Payloads

`byte[]` values are not sent as JSON. They are sent on `oc2.blob.0`, back to back. In the JSON, the marker
`{"$blob": n}` takes the place of each value, where `n` is the index of the payload. The message describes its payloads,
in order, in its `blobs` field:

```json
{
    "type": "result",
    "id": 24,
    "gen": 1,
    "data": {
        "first": {
            "$blob": 0
        },
        "second": {
            "$blob": 1
        }
    },
    "blobs": [
        {
            "length": 5,
            "checksum": 907060870
        },
        {
            "length": 2,
            "checksum": 3633523372
        }
    ]
}
```

Several markers may refer to the same payload. Payloads no marker refers to are allowed. An empty `blobs` array is the
same as none.

`checksum` is the payload's CRC-32 (reference is zlib), an unsigned 32-bit integer. `"hello"` gives `907060870`, no
bytes give `0`. A minimal implementation in C:

```c
uint32_t checksum(const uint8_t *p, size_t n) {
    uint32_t crc = 0xFFFFFFFF;
    for (size_t i = 0; i < n; i++) {
        crc ^= p[i];
        for (int k = 0; k < 8; k++) crc = (crc >> 1) ^ (0xEDB88320 & -(crc & 1));
    }
    return ~crc;
}
```

### Guest to Host

Only `invoke` may have `blobs`, other requests fail with `malformed message`. Write all payload bytes to `oc2.blob.0`
before writing the JSON frame. A `byte[]` parameter only accepts a marker or `null`. A marker with an index not in
`blobs` fails with `invalid parameter signature`. Payload bytes not used by the next request are discarded.

### Host to Guest

Only `result` may have `blobs`. Markers may be anywhere in `data`, also nested. After reading the reply, read the
payloads from `oc2.blob.0` in order, `length` bytes each.

## Value Marshalling

Values are converted using [Gson](https://github.com/google/gson) defaults, with custom converters for some types, e.g.
item stacks, fluid stacks, directions and sides. `byte[]` is handled as [binary payload](#binary-payloads).

### Parameters

Each JSON value is converted to the declared Java type. The first overload with the right number of parameters, for
which all conversions succeed, is called. Conversion is lenient, e.g. fractions are truncated and integers wrap around.

### Results

Numbers, strings and booleans are converted directly. Java arrays and collections become JSON arrays, Java maps and
objects become JSON objects, enums become the name of their constant. `null` fields of objects and maps are omitted,
`null` elements of arrays are kept. Whole-number doubles are written as `15.0`. `NaN` and infinities fail the call.

## Errors

Example error message:

```json
{
    "type": "error",
    "id": 23,
    "gen": 1,
    "data": "unknown method"
}
```

`data` is a message for display, do not match on it. The `id` of the request is returned, except where noted.

Examples:

- `message too large`: Request frame larger than 4 KiB.
- `malformed message`: Not a JSON object, including `null` and whitespace-only frames, or invalid message format, e.g. a
  missing or unknown `type`, a non-numeric `id`, an invalid UUID, a non-object `data` in `invoke`, `blobs` on a request
  other than `invoke`, or a `blobs` entry without a numeric `length` and `checksum`.
- `unknown method`: No method with that name.

## Appendix: `oc2busd`

The Linux image shipped with the mod runs `oc2busd`. It opens the three ports and shares them between processes. It is
not part of the protocol. Programs on another OS must use the ports directly. On the shipped Linux, a program can either
use the daemon's socket, or stop the daemon to free the ports. The shipped libraries use the ports directly when the
daemon is not running.

The daemon listens on the `AF_UNIX` stream socket `/run/oc2/bus`, or `$OC2_BUS_SOCKET` if set. A session consists of one
connection per role:

1. Connect, write the byte `R`, then the frame `{"type":"hello","version":1,"roles":["rpc","blob","event"]}`.
   `roles` may list fewer roles, `rpc` is always included.
2. The daemon replies `{"type":"hello","version":1,"token":"<32 hex digits>","gen":1}`. `gen` is missing until the
   daemon has learned it. If the hello is not accepted, the daemon replies with an `error` and closes the connection. If
   the frame cannot be parsed, it closes the connection without a reply.
3. For each additional role, connect and write `B` (blob) or `E` (event), followed by the 32-byte token.

After that, each connection uses the protocol of its port, with these differences:

- `list` and `methods` are answered from a cache, keyed by `gen`.
- The daemon has errors of its own, which may lack `gen`. If the host does not reply within 30 seconds, the request
  fails with `the host did not answer in time`.
- Replies have the `id` of the request, also for errors the host sent with `id` `0`. If the request had no `id`, neither
  has the reply.
- Sending a second request before the first one was answered fails, and closes the session.
- `eventsDropped` refers to the session's own event buffer of 64 KiB, and has no `id`. It is sent right before the next
  event that fits, so if no further event arrives, it is not sent.

The daemon's source is [`busd.lua`](../common/src/main/scripts/lib/lua/oc2/busd.lua).
