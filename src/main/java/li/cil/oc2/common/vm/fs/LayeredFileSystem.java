package li.cil.oc2.common.vm.fs;

import li.cil.sedna.fs.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;

public final class LayeredFileSystem implements FileSystem {
    private final ArrayList<FileSystem> fileSystems = new ArrayList<>();

    public void addLayer(final FileSystem fileSystem) {
        fileSystems.add(fileSystem);
    }

    public void clear() {
        fileSystems.clear();
    }

    @Override
    public FileSystemStats statfs() throws IOException {
        final FileSystemStats result = new FileSystemStats();
        for (final FileSystem fileSystem : fileSystems) {
            final FileSystemStats stats = fileSystem.statfs();
            result.blockCount += stats.blockCount; // not correct if blocksize differs, but whatever
            result.freeBlockCount += stats.freeBlockCount;
            result.availableBlockCount += stats.availableBlockCount;
            result.fileCount += stats.fileCount;
            result.freeFileCount += stats.freeFileCount;
        }
        return result;
    }

    @Override
    public long getUniqueId(final Path path) throws IOException {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.getUniqueId(path);
            }
        }

        throw new FileNotFoundException();
    }

    @Override
    public boolean exists(final Path path) {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isDirectory(final Path path) {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.isDirectory(path);
            }
        }

        return false;
    }

    @Override
    public boolean isWritable(final Path path) {
        return false;
    }

    @Override
    public boolean isReadable(final Path path) {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.isReadable(path);
            }
        }

        return false;
    }

    @Override
    public boolean isExecutable(final Path path) {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.isExecutable(path);
            }
        }

        return false;
    }

    @Override
    public BasicFileAttributes getAttributes(final Path path) throws IOException {
        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.getAttributes(path);
            }
        }

        throw new FileNotFoundException();
    }

    @Override
    public void mkdir(final Path path) throws IOException {
        throw new IOException();
    }

    @Override
    public FileHandle open(final Path path, final int flags) throws IOException {
        if ((flags & FileMode.WRITE) != 0) {
            throw new IOException();
        }

        for (final FileSystem fileSystem : fileSystems) {
            if (fileSystem.exists(path)) {
                return fileSystem.open(path, flags);
            }
        }

        throw new FileNotFoundException();
    }

    @Override
    public FileHandle create(final Path path, final int flags) throws IOException {
        throw new IOException();
    }

    @Override
    public void unlink(final Path path) throws IOException {
        throw new IOException();
    }

    @Override
    public void rename(final Path oldPath, final Path newPath) throws IOException {
        throw new IOException();
    }
}
