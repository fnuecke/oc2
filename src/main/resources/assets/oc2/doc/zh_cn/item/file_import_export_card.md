# 文件导入/导出卡
![在墙上哭泣!](item:oc2:file_import_export_card)

文件导入/导出卡允许电脑打破第四面墙. 它允许:
- 从实体机上传文件到虚拟[电脑](../block/computer.md).
- 从虚拟电脑下载文件到实体机.

为了方便性, 标准的发行版提供两个脚本来进行该操作, `import.lua` 和 `export.lua`.

`import.lua` 将会做以下事：当运行时, prompt for a file to upload to the virtual computer. The file will be stored in the current working directory. If a file with the name of the imported file already exists, the option to provide an alternative name will be offered.

`export.lua` takes as its parameter the path to a file in a virtual computer. It then downloads this file to your real computer and offers a save dialog, offering the choice of where to save the downloaded file, or to cancel the operation.

Both scripts will prompt all users currently interacting with the computer's terminal. For upload operations (`import.lua`), the first uploaded file will be used. The prompts on other clients will be canceled. For download operations all clients will be offered to save the exported file.