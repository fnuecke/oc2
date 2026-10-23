# Lectern

## High-level API
Device name: `lectern`

Provided by lecterns connected to a [bus interface](../block/bus_interface.md). Pages are numbered from `0`.

### Methods

`getPage():number`
Gets the page the book on the lectern is open at.
- Returns the page.

`getPageCount():number`
Gets how many pages the book on the lectern has.
- Returns the number of pages, `0` when empty.

## Mid-level API
Device name: `LECTRN`

### Methods

`1 getPage`
Gets the page the book on the lectern is open at.
- Returns one byte, the page.

`2 getPageCount`
Gets how many pages the book on the lectern has.
- Returns one byte, the number of pages, `0` when empty.
