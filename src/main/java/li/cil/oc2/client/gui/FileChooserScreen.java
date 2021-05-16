package li.cil.oc2.client.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.list.ExtendedList;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FileChooserScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final int MARGIN = 30;
    private static final int WIDGET_SPACING = 8;

    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_ENTRY_HEIGHT = 12;

    private static final TranslationTextComponent OPEN_TITLE_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.title.load");
    private static final TranslationTextComponent SAVE_TITLE_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.title.save");
    private static final TranslationTextComponent FILE_NAME_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.text_field.filename");
    private static final TranslationTextComponent LOAD_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.confirm_button.load");
    private static final TranslationTextComponent SAVE_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.confirm_button.save");
    private static final TranslationTextComponent OVERWRITE_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.confirm_button.overwrite");
    private static final TranslationTextComponent CANCEL_TEXT = new TranslationTextComponent("gui.oc2.file_chooser.cancel_button");

    ///////////////////////////////////////////////////////////////////

    private static Path directory = Paths.get("").toAbsolutePath();

    ///////////////////////////////////////////////////////////////////

    private final FileChooserCallback callback;
    private final boolean isLoad;

    private final Screen previousScreen;

    private FileList fileList;
    private TextFieldWidget fileNameTextField;
    private Button okButton;

    private boolean isComplete;

    ///////////////////////////////////////////////////////////////////

    @FunctionalInterface
    public
    interface FileChooserCallback {
        void onFileSelected(Path path);

        default void onCanceled() {
        }
    }

    ///////////////////////////////////////////////////////////////////

    public static void openFileChooserForSave(final String name, final FileChooserCallback callback) {
        final Screen currentScreen = Minecraft.getInstance().currentScreen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.closeScreen();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, false);
        Minecraft.getInstance().displayGuiScreen(screen);
        screen.fileNameTextField.setText(name);
    }

    public static void openFileChooserForLoad(final FileChooserCallback callback) {
        final Screen currentScreen = Minecraft.getInstance().currentScreen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.closeScreen();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, true);
        Minecraft.getInstance().displayGuiScreen(screen);
    }

    ///////////////////////////////////////////////////////////////////

    public FileChooserScreen(final FileChooserCallback callback, final boolean isLoad) {
        super(isLoad ? OPEN_TITLE_TEXT : SAVE_TITLE_TEXT);

        this.callback = callback;
        this.isLoad = isLoad;

        this.previousScreen = Minecraft.getInstance().currentScreen;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void onClose() {
        if (!isComplete) {
            callback.onCanceled();
        }

        if (previousScreen != null) {
            minecraft.enqueue(() -> minecraft.displayGuiScreen(previousScreen));
        }
    }

    @Override
    public void render(final MatrixStack matrixStack, final int mouseX, final int mouseY, final float partialTicks) {
        super.renderBackground(matrixStack);
        fileList.render(matrixStack, mouseX, mouseY, partialTicks);
        fileNameTextField.render(matrixStack, mouseX, mouseY, partialTicks);
        super.render(matrixStack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();
        minecraft.keyboardListener.enableRepeatEvents(true);

        final int widgetsWidth = width - MARGIN * 2;
        final int listHeight = height - MARGIN - WIDGET_SPACING - TEXT_FIELD_HEIGHT - WIDGET_SPACING - BUTTON_HEIGHT - MARGIN;
        fileList = new FileList(MARGIN, listHeight, LIST_ENTRY_HEIGHT);
        addListener(fileList);

        final int fileNameTop = MARGIN + listHeight + WIDGET_SPACING;
        fileNameTextField = new TextFieldWidget(font, MARGIN, fileNameTop, widgetsWidth, TEXT_FIELD_HEIGHT, FILE_NAME_TEXT);
        fileNameTextField.setResponder(s -> {
            fileList.setSelected(null);
            updateButtons();
        });
        fileNameTextField.setMaxStringLength(1024);
        addListener(fileNameTextField);

        final int buttonTop = fileNameTop + TEXT_FIELD_HEIGHT + WIDGET_SPACING;
        final int buttonCount = 2;
        final int buttonWidth = widgetsWidth / buttonCount - (buttonCount - 1) * WIDGET_SPACING;
        okButton = addButton(new Button(MARGIN, buttonTop, buttonWidth, BUTTON_HEIGHT, StringTextComponent.EMPTY, this::handleOkPressed));
        addButton(new Button(MARGIN + buttonWidth + WIDGET_SPACING, buttonTop, buttonWidth, BUTTON_HEIGHT, CANCEL_TEXT, this::handleCancelPressed));

        fileList.refreshFiles(directory);

        updateButtons();
    }

    ///////////////////////////////////////////////////////////////////

    private boolean isParentPath() {
        if (directory == null) {
            return false;
        }

        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return selected.file == null || selected.file.equals(directory.getParent());
        }

        final String selectedFileEntry = fileNameTextField.getText();
        return "..".equals(selectedFileEntry);
    }

    @Nullable
    private Optional<Path> getPath() {
        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Optional.ofNullable(selected.file);
        }

        if (directory == null) {
            return Optional.empty();
        }

        final String selectedFileEntry = fileNameTextField.getText();
        if (selectedFileEntry == null || "".equals(selectedFileEntry) || ".".equals(selectedFileEntry)) {
            return Optional.empty();
        }

        try {
            return Optional.of(directory.resolve(selectedFileEntry));
        } catch (final InvalidPathException e) {
            return Optional.empty();
        }
    }

    private void confirm() {
        if (isParentPath()) {
            fileList.refreshFiles(getPath().orElse(null));
            fileNameTextField.setText("");
            return;
        }

        getPath().ifPresent(path -> {
            if (path == null || Files.isDirectory(path)) {
                fileList.refreshFiles(path);
                fileNameTextField.setText("");
                return;
            }
            if (Files.isRegularFile(path)) {
                isComplete = true;
                callback.onFileSelected(path);
                closeScreen();
            } else if (!isLoad) {
                isComplete = true;
                callback.onFileSelected(path);
                closeScreen();
            } // else: cannot load non-existing file
        });
    }

    private void cancel() {
        isComplete = true;
        callback.onCanceled();
        closeScreen();
    }

    private void updateButtons() {
        okButton.active = false;
        okButton.setMessage(isLoad ? LOAD_TEXT : SAVE_TEXT);
        okButton.clearFGColor();

        if (isParentPath()) {
            okButton.active = true;
            return;
        }

        getPath().ifPresent(path -> {
            if (isLoad) {
                okButton.active = Files.exists(path);
            } else {
                okButton.active = true;
                if (Files.isRegularFile(path)) {
                    okButton.setMessage(OVERWRITE_TEXT);
                    okButton.setFGColor(0xFF0000);
                }
            }
        });
    }

    private void handleOkPressed(final Button button) {
        confirm();
    }

    private void handleCancelPressed(final Button button) {
        cancel();
    }

    ///////////////////////////////////////////////////////////////////

    private final class FileList extends ExtendedList<FileList.FileEntry> {
        public FileList(final int y, final int height, final int slotHeight) {
            super(FileChooserScreen.this.minecraft, FileChooserScreen.this.width, FileChooserScreen.this.height, y, y + height, slotHeight);
        }

        public void refreshFiles(final Path directory) {
            FileChooserScreen.directory = directory;

            setScrollAmount(0);
            clearEntries();

            if (directory != null && Files.isDirectory(directory)) {
                addEntry(createDirectoryEntry(directory.getParent(), ".."));

                try {
                    final List<Path> files = Files.list(directory)
                            .sorted((p1, p2) -> {
                                if (Files.isDirectory(p1) && !Files.isDirectory(p2)) {
                                    return -1;
                                }
                                if (!Files.isDirectory(p1) && Files.isDirectory(p2)) {
                                    return 1;
                                }
                                return p1.getFileName().compareTo(p2.getFileName());
                            })
                            .collect(Collectors.toList());
                    for (final Path path : files) {
                        if (Files.isHidden(path)) {
                            continue;
                        }

                        if (Files.isDirectory(path)) {
                            addEntry(createDirectoryEntry(path));
                        } else {
                            addEntry(createFileEntry(path));
                        }
                    }
                } catch (final IOException | SecurityException e) {
                    LOGGER.error(e);
                }
            } else {
                for (final Path path : FileSystems.getDefault().getRootDirectories()) {
                    addEntry(createDirectoryEntry(path, path.toString()));
                }
            }
        }

        @Override
        public void setSelected(@Nullable final FileChooserScreen.FileList.FileEntry entry) {
            super.setSelected(entry);
            updateButtons();
        }

        private FileList.FileEntry createFileEntry(final Path file) {
            return new FileList.FileEntry(file, new StringTextComponent(file.getFileName().toString()));
        }

        private FileList.FileEntry createDirectoryEntry(final Path path) {
            return createDirectoryEntry(path, path.getFileName().toString() + path.getFileSystem().getSeparator());
        }

        private FileList.FileEntry createDirectoryEntry(final Path path, final String displayName) {
            return new FileList.FileEntry(path, new StringTextComponent(displayName)
                    .modifyStyle(s -> s.setColor(Color.fromInt(0xA0A0FF))));
        }

        private final class FileEntry extends ExtendedList.AbstractListEntry<FileEntry> {
            private final Path file;
            private final ITextComponent displayName;

            private long lastEntryClickTime = 0;

            public FileEntry(final Path file, final ITextComponent displayName) {
                this.file = file;
                this.displayName = displayName;
            }

            @Override
            public void render(final MatrixStack stack, final int index, final int top, final int left, final int width, final int height,
                               final int mouseX, final int mouseY, final boolean isHovered, final float deltaTime) {
                font.func_243246_a(stack, displayName, left, top, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
                final boolean isLeftClick = button == 0;
                if (isLeftClick) {
                    if (file == null || (directory != null && file.equals(directory.getParent()))) {
                        fileNameTextField.setText("..");
                    } else {
                        final Path fileName = file.getFileName();
                        fileNameTextField.setText(fileName != null ? fileName.toString() : file.toString());
                    }
                    fileNameTextField.setCursorPositionZero();
                    fileNameTextField.setSelectionPos(0);
                    setSelected(this);

                    final boolean isDoubleClick = System.currentTimeMillis() - lastEntryClickTime < 250;
                    if (isDoubleClick) {
                        confirm();
                    }

                    lastEntryClickTime = System.currentTimeMillis();
                }

                return false;
            }
        }
    }
}
