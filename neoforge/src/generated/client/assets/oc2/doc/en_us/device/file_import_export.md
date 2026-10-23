# File Import Export

## High-level API
Device name: `file_import_export`

Provided by the [file import/export card](../item/file_import_export_card.md).

### Exporting
Call `beginExportFile()` with a name, append the contents with `writeExportFile()`, then call `finishExportFile()`. Every user at the terminal is offered to save the file. Files are limited to 512 KiB.

### Importing
Call `requestImportFile()` to prompt every user at the terminal for a file. Poll `beginImportFile()` until it returns the file's name and size; the first file a user picks wins, and the prompts on other clients are canceled. Then call `readImportFile()` until it returns nothing. `reset()` cancels either operation.

Methods fail with an error when called in the wrong order, or when the users canceled.

### Methods

`beginExportFile(name:string)`
Begins exporting a file. Provide its contents with writeExportFile() and complete the export with finishExportFile(). Fails if the device is currently exporting or importing.
- `name`: the name of the file being exported.

`beginImportFile():table`
Checks whether a requested file has arrived and, if so, starts reading it. Poll this after requestImportFile(). Fails if no import was requested, or every user canceled.
- Returns a table with the file's `name` and `size`, or nothing while no file was picked yet.

`finishExportFile()`
Finishes the export and offers every user at the terminal to save the file. Fails if the device is not currently exporting.

`readImportFile():string`
Reads the next chunk of the file being imported. Fails if beginImportFile() did not succeed yet.
- Returns up to 4 KiB of data, or nothing once the whole file was read.

`requestImportFile():boolean`
Begins an import by prompting every user at the terminal to pick a file. Fails if the device is currently exporting or importing.
- Returns whether anyone was prompted; `false` when nobody is using the terminal.

`reset()`
Cancels any export or import in progress and returns the device to its idle state.

`writeExportFile(data:string)`
Appends data to the file being exported. Fails if the device is not currently exporting, or the file grows past 512 KiB.
- `data`: the data to append to the file being exported.
