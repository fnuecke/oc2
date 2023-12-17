/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static li.cil.oc2.common.util.TranslationUtils.text;

public final class FileChooserScreen extends Screen {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final int MARGIN = 30;
    private static final int WIDGET_SPACING = 8;

    private static final int TEXT_FIELD_HEIGHT = 20;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LIST_ENTRY_HEIGHT = 12;

    private static final Component OPEN_TITLE_TEXT = text("gui.{mod}.file_chooser.title.load");
    private static final Component SAVE_TITLE_TEXT = text("gui.{mod}.file_chooser.title.save");
    private static final Component FILE_NAME_TEXT = text("gui.{mod}.file_chooser.text_field.filename");
    private static final Component LOAD_TEXT = text("gui.{mod}.file_chooser.confirm_button.load");
    private static final Component SAVE_TEXT = text("gui.{mod}.file_chooser.confirm_button.save");
    private static final Component OVERWRITE_TEXT = text("gui.{mod}.file_chooser.confirm_button.overwrite");
    private static final Component CANCEL_TEXT = text("gui.{mod}.file_chooser.cancel_button");

    ///////////////////////////////////////////////////////////////////

    private static Path directory = Paths.get("").toAbsolutePath();

    ///////////////////////////////////////////////////////////////////

    private final FileChooserCallback callback;
    private final boolean isLoad;

    private final Screen previousScreen;

    private FileList fileList;
    private EditBox fileNameTextField;
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
        final Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.onClose();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, false);
        Minecraft.getInstance().setScreen(screen);
        screen.fileNameTextField.setValue(name);
    }

    public static void openFileChooserForLoad(final FileChooserCallback callback) {
        final Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof FileChooserScreen) {
            currentScreen.onClose();
        }

        final FileChooserScreen screen = new FileChooserScreen(callback, true);
        Minecraft.getInstance().setScreen(screen);
    }

    ///////////////////////////////////////////////////////////////////

    public FileChooserScreen(final FileChooserCallback callback, final boolean isLoad) {
        super(isLoad ? OPEN_TITLE_TEXT : SAVE_TITLE_TEXT);

        this.callback = callback;
        this.isLoad = isLoad;

        this.previousScreen = Minecraft.getInstance().screen;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void onClose() {
        if (!isComplete) {
            callback.onCanceled();
        }

        if (previousScreen != null) {
            getMinecraft().tell(() -> getMinecraft().setScreen(previousScreen));
        }
    }

    @Override
    public void render(final PoseStack stack, final int mouseX, final int mouseY, final float partialTicks) {
        super.renderBackground(stack);
        fileList.render(stack, mouseX, mouseY, partialTicks);
        fileNameTextField.render(stack, mouseX, mouseY, partialTicks);
        super.render(stack, mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void init() {
        super.init();
        getMinecraft().keyboardHandler.setSendRepeatsToGui(true);

        final int widgetsWidth = width - MARGIN * 2;
        final int listHeight = height - MARGIN - WIDGET_SPACING - TEXT_FIELD_HEIGHT - WIDGET_SPACING - BUTTON_HEIGHT - MARGIN;
        fileList = new FileList(MARGIN, listHeight, LIST_ENTRY_HEIGHT);
        addWidget(fileList);

        final int fileNameTop = MARGIN + listHeight + WIDGET_SPACING;
        fileNameTextField = new EditBox(font, MARGIN, fileNameTop, widgetsWidth, TEXT_FIELD_HEIGHT, FILE_NAME_TEXT);
        fileNameTextField.setResponder(s -> {
            fileList.setSelected(null);
            updateButtons();
        });
        fileNameTextField.setMaxLength(1024);
        addWidget(fileNameTextField);

        final int buttonTop = fileNameTop + TEXT_FIELD_HEIGHT + WIDGET_SPACING;
        final int buttonCount = 2;
        final int buttonWidth = widgetsWidth / buttonCount - (buttonCount - 1) * WIDGET_SPACING;
        okButton = addRenderableWidget(new Button(MARGIN, buttonTop, buttonWidth, BUTTON_HEIGHT, Component.empty(), this::handleOkPressed));
        addRenderableWidget(new Button(MARGIN + buttonWidth + WIDGET_SPACING, buttonTop, buttonWidth, BUTTON_HEIGHT, CANCEL_TEXT, this::handleCancelPressed));

        fileList.refreshFiles(directory);

        updateButtons();
    }

    @Override
    public void onFilesDrop(final List<Path> files) {
        files.stream().filter(file -> {
            try {
                return Files.exists(file) && !Files.isHidden(file);
            } catch (final IOException | SecurityException ignored) {
                return false;
            }
        }).findFirst().ifPresent(fileList::selectPath);
    }

    ///////////////////////////////////////////////////////////////////

    private boolean isParentPath() {
        if (directory == null) {
            return false;
        }

        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Objects.equals(selected.file, directory.getParent());
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        return "..".equals(selectedFileEntry);
    }

    private Optional<Path> getPath() {
        final FileList.FileEntry selected = fileList.getSelected();
        if (selected != null) {
            return Optional.ofNullable(selected.file);
        }

        if (directory == null) {
            return Optional.empty();
        }

        final String selectedFileEntry = fileNameTextField.getValue();
        if ("".equals(selectedFileEntry) || ".".equals(selectedFileEntry)) {
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
            return;
        }

        getPath().ifPresent(path -> {
            if (Files.isDirectory(path)) {
                fileList.refreshFiles(path);
                return;
            }
            if (Files.isRegularFile(path)) {
                isComplete = true;
                callback.onFileSelected(path);
                onClose();
            } else if (!isLoad) {
                isComplete = true;
                callback.onFileSelected(path);
                onClose();
            } // else: cannot load non-existing file
        });
    }

    private void cancel() {
        isComplete = true;
        callback.onCanceled();
        onClose();
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

    private final class FileList extends ObjectSelectionList<FileList.FileEntry> {
        public FileList(final int y, final int height, final int slotHeight) {
            super(FileChooserScreen.this.getMinecraft(), FileChooserScreen.this.width, FileChooserScreen.this.height, y, y + height, slotHeight);
        }

        public void refreshFiles(@Nullable final Path directory) {
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
                        }).toList();
                    for (final Path path : files) {
                        try {
                            if (Files.isHidden(path)) {
                                continue;
                            }

                            if (Files.isDirectory(path)) {
                                addEntry(createDirectoryEntry(path));
                            } else {
                                addEntry(createFileEntry(path));
                            }
                        } catch (final IOException | SecurityException ignored) {
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

            fileNameTextField.setValue("");
        }

        public void selectPath(final Path path) {
            if (Files.isDirectory(path)) {
                refreshFiles(path);
            } else {
                refreshFiles(path.getParent());
                children().stream().filter(entry -> path.equals(entry.file))
                    .findFirst().ifPresent(entry -> {
                        entry.select();
                        centerScrollOn(entry);
                    });
            }
        }

        @Override
        public void setSelected(@Nullable final FileChooserScreen.FileList.FileEntry entry) {
            super.setSelected(entry);
            updateButtons();
        }

        private FileList.FileEntry createFileEntry(final Path file) {
            return new FileList.FileEntry(file, Component.literal(file.getFileName().toString()));
        }

        private FileList.FileEntry createDirectoryEntry(final Path path) {
            return createDirectoryEntry(path, path.getFileName().toString() + path.getFileSystem().getSeparator());
        }

        private FileList.FileEntry createDirectoryEntry(@Nullable final Path path, final String displayName) {
            final TextColor color = path != null && Files.exists(path)
                ? TextColor.fromRgb(0xA0A0FF)
                : TextColor.fromLegacyFormat(ChatFormatting.GRAY);
            return new FileList.FileEntry(path, Component.literal(displayName)
                .withStyle(s -> s.withColor(color)));
        }

        private final class FileEntry extends ObjectSelectionList.Entry<FileEntry> {
            @Nullable private final Path file;
            private final Component displayName;

            private long lastEntryClickTime = 0;

            public FileEntry(@Nullable final Path file, final Component displayName) {
                this.file = file;
                this.displayName = displayName;
            }

            @Override
            public void render(final PoseStack stack, final int index, final int top, final int left, final int width, final int height,
                               final int mouseX, final int mouseY, final boolean isHovered, final float deltaTime) {
                font.drawShadow(stack, displayName, left, top, 0xFFFFFFFF);
            }

            @Override
            public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
                final boolean isLeftClick = button == 0;
                if (isLeftClick) {
                    select();

                    final boolean isDoubleClick = System.currentTimeMillis() - lastEntryClickTime < 250;
                    if (isDoubleClick && okButton.active) {
                        confirm();
                    }

                    lastEntryClickTime = System.currentTimeMillis();
                }

                return false;
            }

            public void select() {
                if (directory != null && Objects.equals(directory.getParent(), file)) {
                    fileNameTextField.setValue("..");
                } else if (file != null) {
                    final Path fileName = file.getFileName();
                    fileNameTextField.setValue(fileName != null ? fileName.toString() : file.toString());
                } else {
                    return;
                }
                fileNameTextField.moveCursorToStart();
                fileNameTextField.setHighlightPos(0);
                setSelected(this);
            }

            @Override
            public Component getNarration() {
                return Component.translatable("narrator.select", displayName);
            }
        }
    }
}
