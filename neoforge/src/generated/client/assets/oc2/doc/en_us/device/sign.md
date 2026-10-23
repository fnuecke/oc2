# Sign

## High-level API
Device name: `sign`

Provided by signs connected to a [bus interface](../block/bus_interface.md). Signs can have text on their `front` and `back`.

### Methods

`getColor(side:string):string`
Gets the color of the text on a side of the sign.
- `side`: `front` or `back`.
- Returns the name of the color.

`getText(side:string):table`
Gets the text on a side of the sign.
- `side`: `front` or `back`.
- Returns the current text lines.

`isGlowing(side:string):boolean`
Gets whether the text on a side of the sign glows.
- `side`: `front` or `back`.
- Returns `true` if the text glows.

`isReadonly():boolean`
Gets whether the sign is read-only.
- Returns whether the sign is read-only.

`setText(side:string, lines:table)`
Sets the text on a side of the sign.
- `side`: `front` or `back`.
- `lines`: up to four lines of text.

## Mid-level API
Device name: `SIGN`

Sides are numbered: `0` for front, `1` for back.

Colors are numbered as dyes: `0` white, `1` orange, `2` magenta, `3` light blue, `4` yellow, `5` lime, `6` pink, `7` gray, `8` light gray, `9` cyan, `10` purple, `11` blue, `12` brown, `13` green, `14` red, `15` black.

### Methods

`1 getLine`
Gets a line of text on a side of the sign.
- Takes two bytes, the side, `0` for front, `1` for back, and the line.
- Returns the text. Read while `OCDAV` is set to read fully.

`2 setLine`
Sets a line of text on a side of the sign.
- Takes the side, `0` for front, `1` for back, and the line as one byte each, then the text.

`3 getColor`
Gets the color of the text on a side of the sign.
- Takes one byte, `0` for front, `1` for back.
- Returns one byte, the color code.

`4 isGlowing`
Gets whether the text on a side of the sign glows.
- Takes one byte, `0` for front, `1` for back.
- Returns one byte, `1` if the text glows, `0` otherwise.

`5 isReadonly`
Gets whether the sign is read-only.
- Returns one byte, `1` if read-only, `0` otherwise.
