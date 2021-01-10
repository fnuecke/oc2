package li.cil.oc2.common.vm.fs;

import com.google.gson.JsonObject;
import it.unimi.dsi.fastutil.objects.Object2LongArrayMap;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.util.ResourceUtils;
import li.cil.sedna.fs.*;
import net.minecraft.resources.IResource;
import net.minecraft.resources.IResourceManager;
import net.minecraft.resources.data.IMetadataSectionSerializer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nullable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.*;
import java.util.stream.Collectors;

public final class ResourceFileSystem implements FileSystem {
    private final IResourceManager resourceManager;
    private final Node root;
    private final Object2LongArrayMap<Node> sizes = new Object2LongArrayMap<>();

    ///////////////////////////////////////////////////////////////////

    public ResourceFileSystem(final IResourceManager resourceManager, final ResourceLocation rootLocation) {
        this.resourceManager = resourceManager;
        this.root = new Node(resourceManager);

        final String rootLocationPath = rootLocation.getPath();
        final Collection<ResourceLocation> allLocations = resourceManager.getAllResourceLocations(rootLocationPath, s -> true);
        for (final ResourceLocation location : allLocations) {
            final String path = location.getPath();
            assert path.startsWith(rootLocationPath);
            final String localPath = path.substring(rootLocationPath.length());
            if (localPath.isEmpty() || "/".equals(localPath)) {
                continue; // Skip the directory we're using as root.
            }

            // Ensure we have a mutable list since insert removes the first item for each level.
            final ArrayList<String> pathParts = Arrays.stream(localPath.split("/")).filter(s -> !s.isEmpty())
                    .collect(Collectors.toCollection(ArrayList::new));
            this.root.insert(pathParts, location);
        }

        root.buildEntries();
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public FileSystemStats statfs() {
        return new FileSystemStats();
    }

    @Override
    public long getUniqueId(final Path path) throws IOException {
        return getNodeOrThrow(path).hashCode();
    }

    @Override
    public boolean exists(final Path path) {
        return getNode(path) != null;
    }

    @Override
    public boolean isDirectory(final Path path) {
        final Node node = getNode(path);
        return node != null && node.isDirectory;
    }

    @Override
    public boolean isWritable(final Path path) {
        return false;
    }

    @Override
    public boolean isReadable(final Path path) {
        return exists(path);
    }

    @Override
    public boolean isExecutable(final Path path) {
        final Node node = getNode(path);
        return node != null && node.isExecutable;
    }

    @Override
    public BasicFileAttributes getAttributes(final Path path) throws IOException {
        final Node node = getNodeOrThrow(path);
        return new BasicFileAttributes() {
            @Nullable
            @Override
            public FileTime lastModifiedTime() {
                return null;
            }

            @Nullable
            @Override
            public FileTime lastAccessTime() {
                return null;
            }

            @Nullable
            @Override
            public FileTime creationTime() {
                return null;
            }

            @Override
            public boolean isRegularFile() {
                return !node.isDirectory;
            }

            @Override
            public boolean isDirectory() {
                return node.isDirectory;
            }

            @Override
            public boolean isSymbolicLink() {
                return false;
            }

            @Override
            public boolean isOther() {
                return false;
            }

            @Override
            public long size() {
                return getCachedSize(node);
            }

            @Override
            public Object fileKey() {
                return node;
            }
        };
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

        final Node node = getNodeOrThrow(path);
        if (node.isDirectory) {
            return new FileHandle() {
                @Override
                public int read(final long offset, final ByteBuffer buffer) throws IOException {
                    throw new IOException();
                }

                @Override
                public int write(final long offset, final ByteBuffer buffer) throws IOException {
                    throw new IOException();
                }

                @Override
                public List<DirectoryEntry> readdir() {
                    return node.entries;
                }

                @Override
                public void close() {
                }
            };
        } else {
            assert node.location != null;

            // Resource InputStreams don't always support seeking, so we have to copy to memory :/
            final InputStream stream = resourceManager.getResource(node.location).getInputStream();
            final ByteBuffer data = ByteBuffer.wrap(IOUtils.toByteArray(stream));
            stream.close();
            return new FileHandle() {
                @Override
                public int read(final long offset, final ByteBuffer buffer) throws IOException {
                    if (offset < 0 || offset > data.capacity()) {
                        throw new IOException();
                    }
                    data.position((int) offset);
                    final int count = Math.min(buffer.remaining(), data.capacity() - data.position());
                    data.limit(data.position() + count);
                    buffer.put(data);
                    return count;
                }

                @Override
                public int write(final long offset, final ByteBuffer buffer) throws IOException {
                    throw new IOException();
                }

                @Override
                public List<DirectoryEntry> readdir() throws IOException {
                    throw new IOException();
                }

                @Override
                public void close() throws IOException {
                    stream.close();
                }
            };
        }
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

    ///////////////////////////////////////////////////////////////////

    private long getCachedSize(final Node node) {
        if (node.isDirectory || node.location == null) {
            return 0L;
        }

        return sizes.computeIfAbsent(node, n -> {
            try (final IResource resource = node.resourceManager.getResource(node.location)) {
                final InputStream stream = resource.getInputStream();
                final byte[] buffer = new byte[4 * Constants.KILOBYTE];

                long size = 0;
                int readCount;
                while ((readCount = stream.read(buffer)) != -1) {
                    size += readCount;
                }

                return size;
            } catch (final IOException e) {
                return 0L;
            }
        });
    }

    @Nullable
    private Node getNode(final Path path) {
        Node node = root;
        for (final String part : path.getParts()) {
            final Node child = node.children.get(part);
            if (child == null) {
                return null;
            }
            node = child;
        }

        return node;
    }

    private Node getNodeOrThrow(final Path path) throws IOException {
        final Node node = getNode(path);
        if (node == null) {
            throw new FileNotFoundException();
        }
        return node;
    }

    ///////////////////////////////////////////////////////////////////

    private static final class Node {
        public final IResourceManager resourceManager;
        @Nullable public final ResourceLocation location;
        public final boolean isExecutable;
        public final boolean isDirectory;
        public final HashMap<String, Node> children = new HashMap<>();
        public final ArrayList<DirectoryEntry> entries = new ArrayList<>();

        public Node(final IResourceManager resourceManager) {
            this(resourceManager, null);
        }

        public Node(final IResourceManager resourceManager, @Nullable final ResourceLocation location) {
            this.resourceManager = resourceManager;
            this.location = location;

            boolean isDirectory;
            boolean isExecutable;
            if (location != null) {
                try {
                    // If we can successfully retrieved resource it's a file. Directories cause errors.
                    resourceManager.getResource(location);
                    final FileAttributesMetadataSection metadata = ResourceUtils.getMetadata(resourceManager, location, FileAttributesMetadataSection.SERIALIZER);
                    isExecutable = metadata != null && metadata.isExecutable();
                    isDirectory = false;
                } catch (final IOException e) {
                    isExecutable = true;
                    isDirectory = true;
                }
            } else {
                isExecutable = true;
                isDirectory = true;
            }

            this.isExecutable = isExecutable;
            this.isDirectory = isDirectory;
        }

        public void insert(final List<String> path, final ResourceLocation location) {
            final String head = path.remove(0);
            if (path.isEmpty()) {
                final Node node = new Node(resourceManager, location);
                children.put(head, node);
            } else {
                children.computeIfAbsent(head, unused -> new Node(resourceManager)).insert(path, location);
            }
        }

        public void buildEntries() {
            children.forEach((name, child) -> {
                final DirectoryEntry directoryEntry = new DirectoryEntry();
                directoryEntry.name = name;
                directoryEntry.type = child.isDirectory ? FileType.DIRECTORY : FileType.FILE;
                entries.add(directoryEntry);

                child.buildEntries();
            });
        }
    }

    private static final class FileAttributesMetadataSectionSerializer implements IMetadataSectionSerializer<FileAttributesMetadataSection> {
        @Override
        public String getSectionName() {
            return "attributes";
        }

        @Override
        public FileAttributesMetadataSection deserialize(final JsonObject json) {
            final boolean isExecutable = JSONUtils.getBoolean(json, "is_executable");
            return new FileAttributesMetadataSection(isExecutable);
        }
    }

    private static class FileAttributesMetadataSection {
        public static final FileAttributesMetadataSectionSerializer SERIALIZER = new FileAttributesMetadataSectionSerializer();

        public final boolean isExecutable;

        public FileAttributesMetadataSection(final boolean isExecutable) {
            this.isExecutable = isExecutable;
        }

        public boolean isExecutable() {
            return isExecutable;
        }
    }
}
