# File Import/Export Card
![Tear down this wall!](item:oc2:file_import_export_card)

The file import/export card allows breaking the fourth wall. It allows:
- uploading files from your real computer into a virtual [computer](../block/computer.md).
- downloading files from a virtual computer onto your real computer.

For added convenience, the default Linux distribution provides two utility scripts for these operations, `import.lua` and `export.lua`.

`import.lua` will, when run, prompt for a file to upload to the virtual computer. The file will be stored in the current working directory. If a file with the name of the imported file already exists, the option to provide an alternative name will be offered.

`export.lua` takes as its parameter the path to a file in a virtual computer. It then downloads this file to your real computer and offers a save dialog, offering the choice of where to save the downloaded file, or to cancel the operation.

Both scripts will prompt all users currently interacting with the computer's terminal. For upload operations (`import.lua`), the first uploaded file will be used. The prompts on other clients will be canceled. For download operations all clients will be offered to save the exported file.