package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.vm.device.BlockDevice;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ReadOnlyBufferException;

public final class VirtIOBlockDevice extends AbstractVirtIODevice implements Steppable, Closeable {
    private static final int VIRTIO_BLK_SECTOR_SIZE = 512;

    /**
     * Maximum size of any single segment is in {@code size_max}.
     */
    private static final int VIRTIO_BLK_F_SIZE_MAX = 1;
    /**
     * Maximum number of segments in a request is in {@code seg_max}.
     */
    private static final int VIRTIO_BLK_F_SEG_MAX = 2;
    /**
     * Disk-style geometry specified in {@code geometry}.
     */
    private static final int VIRTIO_BLK_F_GEOMETRY = 4;
    /**
     * Device is read-only.
     */
    private static final int VIRTIO_BLK_F_RO = 5;
    /**
     * Block size of disk is in {@code blk_size}.
     */
    private static final int VIRTIO_BLK_F_BLK_SIZE = 6;
    /**
     * Cache flush command support.
     */
    private static final int VIRTIO_BLK_F_FLUSH = 9;
    /**
     * Device exports information on optimal I/O alignment.
     */
    private static final int VIRTIO_BLK_F_TOPOLOGY = 10;
    /**
     * Device can toggle its cache between writeback and writethrough modes.
     */
    private static final int VIRTIO_BLK_F_CONFIG_WCE = 11;
    /**
     * Device can support discard command, maximum discard sectors size in {@code max_discard_sectors} and
     * maximum discard segment number in {@code max_discard_seg}.
     */
    private static final int VIRTIO_BLK_F_DISCARD = 13;
    /**
     * Device can support write zeroes command, maximum write zeroes sectors size in {@code max_write_zeroes_sectors}
     * and maximum write zeroes segment number in {@code max_write_zeroes_seg}.
     */
    private static final int VIRTIO_BLK_F_WRITE_ZEROES = 14;

    private static final int VIRTIO_BLK_CFG_CAPACITY_OFFSET = 0;
    private static final int VIRTIO_BLK_CFG_CAPACITYH_OFFSET = 4;
    private static final int VIRTIO_BLK_CFG_SIZE_MAX_OFFSET = 8;
    private static final int VIRTIO_BLK_CFG_SEG_MAX_OFFSET = 12;
    private static final int VIRTIO_BLK_CFG_GEOMETRY_CYLINDERS_OFFSET = 16;
    private static final int VIRTIO_BLK_CFG_GEOMETRY_HEADS_OFFSET = 18;
    private static final int VIRTIO_BLK_CFG_GEOMETRY_SECTORS_OFFSET = 19;
    private static final int VIRTIO_BLK_CFG_BLK_SIZE_OFFSET = 20;
    private static final int VIRTIO_BLK_CFG_TOPOLOGY_PHYSICAL_BLOCK_EXP_OFFSET = 24;
    private static final int VIRTIO_BLK_CFG_TOPOLOGY_ALIGNMENT_OFFSET_OFFSET = 25;
    private static final int VIRTIO_BLK_CFG_TOPOLOGY_MIN_IO_SIZE_OFFSET = 26;
    private static final int VIRTIO_BLK_CFG_TOPOLOGY_OPT_IO_SIZE_OFFSET = 28;
    private static final int VIRTIO_BLK_CFG_WRITEBACK_OFFSET = 32;
    private static final int VIRTIO_BLK_CFG_MAX_DISCARD_SECTORS_OFFSET = 36;
    private static final int VIRTIO_BLK_CFG_MAX_DISCARD_SEG_OFFSET = 40;
    private static final int VIRTIO_BLK_CFG_DISCARD_SECTOR_ALIGNMENT_OFFSET = 44;
    private static final int VIRTIO_BLK_CFG_MAX_WRITE_ZEROES_SECTORS_OFFSET = 48;
    private static final int VIRTIO_BLK_CFG_MAX_WRITE_ZEROES_SEG_OFFSET = 52;
    private static final int VIRTIO_BLK_CFG_WRITE_ZEROES_MAY_UNMAP_OFFSET = 56;

    private static final int VIRTIO_BLK_T_IN = 0;
    private static final int VIRTIO_BLK_T_OUT = 1;
    private static final int VIRTIO_BLK_T_FLUSH = 4;
    private static final int VIRTIO_BLK_T_DISCARD = 11;
    private static final int VIRTIO_BLK_T_WRITE_ZEROES = 13;

    private static final int VIRTIO_BLK_S_OK = 0;
    private static final int VIRTIO_BLK_S_IOERR = 1;
    private static final int VIRTIO_BLK_S_UNSUPP = 2;

    private static final int VIRTQ_REQUEST = 0;

    private static final int MAX_SEGMENT_SIZE = 32 * 512;
    private static final int MAX_SEGMENT_COUNT = 64;
    private static final int BYTES_PER_THOUSAND_CYCLES = 32;

    private static final ThreadLocal<ByteBuffer> requestHeaderBuffer = new ThreadLocal<>();

    private final BlockDevice block;
    private int remainingByteProcessingQuota;

    public VirtIOBlockDevice(final MemoryMap memoryMap, final BlockDevice block) {
        super(memoryMap, VirtIODeviceSpec.builder(VirtIODeviceType.VIRTIO_DEVICE_ID_BLOCK_DEVICE)
                .configSpaceSize(56)
                .queueCount(1)
                .features((block.isReadonly() ? VIRTIO_BLK_F_RO : 0) |
                          VIRTIO_BLK_F_SIZE_MAX |
                          VIRTIO_BLK_F_SEG_MAX)
                .build());
        this.block = block;
    }

    @Override
    public void close() throws IOException {
        block.close();
    }

    @Override
    public void step(final int cycles) {
        if ((getStatus() & VIRTIO_STATUS_FAILED) != 0) {
            return;
        }

        if (remainingByteProcessingQuota <= 0) {
            remainingByteProcessingQuota += Math.max(1, cycles * BYTES_PER_THOUSAND_CYCLES / 1000);
        }

        try {
            while (remainingByteProcessingQuota > 0) {
                final int processedBytes = processRequest();
                if (processedBytes < 0) {
                    break;
                }
                remainingByteProcessingQuota -= processedBytes;
            }
        } catch (final VirtIODeviceException | MemoryAccessException e) {
            error();
        }
    }

    @Override
    protected int loadConfig(final int offset, final int sizeLog2) {
        // struct virtio_blk_config {
        //     le64 capacity;
        //     le32 size_max;
        //     le32 seg_max;
        //     struct virtio_blk_geometry {
        //         le16 cylinders;
        //         u8 heads;
        //         u8 sectors;
        //     } geometry;
        //     le32 blk_size;
        //     struct virtio_blk_topology {
        //         // # of logical blocks per physical block (log2)
        //         u8 physical_block_exp;
        //         // offset of first aligned logical block
        //         u8 alignment_offset;
        //         // suggested minimum I/O size in blocks
        //         le16 min_io_size;
        //         // optimal (suggested maximum) I/O size in blocks
        //         le32 opt_io_size;
        //     } topology;
        //     u8 writeback;
        //     u8 unused0[3];
        //     le32 max_discard_sectors;
        //     le32 max_discard_seg;
        //     le32 discard_sector_alignment;
        //     le32 max_write_zeroes_sectors;
        //     le32 max_write_zeroes_seg;
        //     u8 write_zeroes_may_unmap;
        //     u8 unused1[3];
        // };
        switch (offset) {
            case VIRTIO_BLK_CFG_CAPACITY_OFFSET: {
                return (int) (capacityToSectorCount(block.getCapacity()) & 0xFFFFFFFFL);
            }
            case VIRTIO_BLK_CFG_CAPACITYH_OFFSET: {
                return (int) (capacityToSectorCount(block.getCapacity()) >>> 32);
            }
            case VIRTIO_BLK_CFG_SIZE_MAX_OFFSET: {
                return MAX_SEGMENT_SIZE;
            }
            case VIRTIO_BLK_CFG_SEG_MAX_OFFSET: {
                return MAX_SEGMENT_COUNT;
            }
        }
        return super.loadConfig(offset, sizeLog2);
    }

    @Override
    protected void storeConfig(final int offset, final int value, final int sizeLog2) {
        // No config fields that can be changed by driver.
    }

    @Override
    protected void handleFeaturesNegotiated() {
        setQueueNotifications(VIRTQ_REQUEST, false);
    }

    private int processRequest() throws VirtIODeviceException, MemoryAccessException {
        final VirtqueueIterator queue = getQueueIterator(VIRTQ_REQUEST);
        if (queue == null) {
            return -1;
        }

        if (!queue.hasNext()) {
            return -1;
        }
        final DescriptorChain chain = queue.next();

        final int processedBytes = chain.readableBytes() + chain.writableBytes();

        // struct virtio_blk_req {
        //     le32 type;
        //     le32 reserved;
        //     le64 sector;
        //     u8 data[][512];
        //     u8 status;
        // };
        //
        // struct virtio_blk_discard_write_zeroes {
        //     le64 sector;
        //     le32 num_sectors;
        //     struct {
        //         le32 unmap:1;
        //         le32 reserved:31;
        //     } flags;
        // };

        final ByteBuffer header = getRequestHeaderBuffer();
        if (chain.readableBytes() < header.limit()) {
            throw new VirtIODeviceException();
        }

        chain.get(header);
        header.flip();

        final int type = header.getInt();
        header.getInt(); // reserved
        final long sector = header.getLong();

        switch (type) {
            case VIRTIO_BLK_T_IN: {
                // Expect to have completely read the header.
                if (chain.readableBytes() > 0) {
                    throw new VirtIODeviceException();
                }

                // Size of virtio_blk_req.data must be a multiple of 512.
                if ((chain.writableBytes() - 1) % VIRTIO_BLK_SECTOR_SIZE != 0) {
                    throw new VirtIODeviceException();
                }

                // Ensure driver respects virtio_blk_config.size_max and virtio_blk_config.seg_max.
                if (chain.writableBytes() > MAX_SEGMENT_COUNT * MAX_SEGMENT_SIZE) {
                    chain.skip(chain.writableBytes() - 1);
                    chain.put((byte) VIRTIO_BLK_S_IOERR);
                    break;
                }

                final long offset = sector * VIRTIO_BLK_SECTOR_SIZE;
                int status = VIRTIO_BLK_S_OK;
                try {
                    chain.put(block.getView(offset, chain.writableBytes() - 1));
                } catch (final IllegalArgumentException e) {
                    chain.skip(chain.writableBytes() - 1);
                    status = VIRTIO_BLK_S_IOERR;
                }

                chain.put((byte) status);
                break;
            }
            case VIRTIO_BLK_T_OUT: {
                // Only expect having to write status.
                if (chain.writableBytes() != 1) {
                    throw new VirtIODeviceException();
                }

                // Size of virtio_blk_req.data must be a multiple of 512.
                if (chain.readableBytes() % VIRTIO_BLK_SECTOR_SIZE != 0) {
                    throw new VirtIODeviceException();
                }

                // Ensure driver respects virtio_blk_config.size_max and virtio_blk_config.seg_max.
                if (chain.readableBytes() > MAX_SEGMENT_COUNT * MAX_SEGMENT_SIZE) {
                    chain.skip(chain.readableBytes());
                    chain.put((byte) VIRTIO_BLK_S_IOERR);
                    break;
                }

                final long offset = sector * VIRTIO_BLK_SECTOR_SIZE;
                int status = VIRTIO_BLK_S_OK;
                try {
                    chain.get(block.getView(offset, chain.readableBytes()));
                } catch (final IllegalArgumentException | ReadOnlyBufferException e) {
                    chain.skip(chain.readableBytes());
                    status = VIRTIO_BLK_S_IOERR;
                }
                chain.put((byte) status);
                break;
            }
            case VIRTIO_BLK_T_FLUSH:
            case VIRTIO_BLK_T_DISCARD:
            case VIRTIO_BLK_T_WRITE_ZEROES:
            default: {
                chain.skip(chain.readableBytes());
                chain.skip(chain.writableBytes() - 1);
                chain.put((byte) VIRTIO_BLK_S_UNSUPP);
                break;
            }
        }

        chain.use();

        return processedBytes;
    }

    private static long capacityToSectorCount(final long capacity) {
        // We may lose some bytes here, but that's better than claiming there are
        // more bytes than there actually are.
        return capacity / VIRTIO_BLK_SECTOR_SIZE;
    }

    private static ByteBuffer getRequestHeaderBuffer() {
        ByteBuffer buffer = requestHeaderBuffer.get();
        if (buffer == null) {
            buffer = ByteBuffer.allocate(16);
            buffer.order(ByteOrder.LITTLE_ENDIAN);
            requestHeaderBuffer.set(buffer);
        }
        buffer.clear();
        return buffer;
    }
}
