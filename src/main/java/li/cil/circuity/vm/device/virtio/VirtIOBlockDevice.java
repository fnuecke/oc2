package li.cil.circuity.vm.device.virtio;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Steppable;
import li.cil.circuity.vm.device.BlockDevice;

public final class VirtIOBlockDevice extends AbstractVirtIODevice implements Steppable {
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

    private static final int VIRTIO_BLK_T_IN = 0;
    private static final int VIRTIO_BLK_T_OUT = 1;
    private static final int VIRTIO_BLK_T_FLUSH = 4;
    private static final int VIRTIO_BLK_T_DISCARD = 11;
    private static final int VIRTIO_BLK_T_WRITE_ZEROES = 13;

    private static final int VIRTIO_BLK_S_OK = 0;
    private static final int VIRTIO_BLK_S_IOERR = 1;
    private static final int VIRTIO_BLK_S_UNSUPP = 2;

    private static final int VIRTQ_REQUEST = 0;

    protected static final int BYTES_PER_CYCLE = 32;

    private final BlockDevice block;
    private Request request;

    public VirtIOBlockDevice(final MemoryMap memoryMap, final BlockDevice block) {
        super(memoryMap, VirtIODeviceSpec.builder(VirtIODeviceType.VIRTIO_DEVICE_ID_BLOCK_DEVICE)
                .configSpaceSize(56)
                .queueCount(1)
                .features(block.isReadonly() ? VIRTIO_BLK_F_RO : 0)
                .build());
        this.block = block;
    }

    @Override
    public void step(final int cycles) {

        final VirtqueueIterator queue = getQueueIterator(VIRTQ_REQUEST);
    }

    @Override
    protected void initializeConfig() {
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
    }

    @Override
    protected int loadConfig(final int offset, final int sizeLog2) {
        switch (offset) {
            case VIRTIO_BLK_CFG_CAPACITY_OFFSET: {
                return (int) (capacityToSectorCount(block.getCapacity()) & 0xFFFFFFFFL);
            }
            case VIRTIO_BLK_CFG_CAPACITYH_OFFSET: {
                return (int) (capacityToSectorCount(block.getCapacity()) >>> 32);
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

    private static long capacityToSectorCount(final long capacity) {
        return capacity / VIRTIO_BLK_SECTOR_SIZE + 1;
    }

    /**
     * Requests are defined as such:
     * <pre>{@code
     * struct virtio_blk_req {
     *     le32 type;
     *     le32 reserved;
     *     le64 sector;
     *     u8 data[][512];
     *     u8 status;
     * };
     *
     * struct virtio_blk_discard_write_zeroes {
     *     le64 sector;
     *     le32 num_sectors;
     *     struct {
     *         le32 unmap:1;
     *         le32 reserved:31;
     *     } flags;
     * };
     * }</pre>
     */
    protected static final class Request {

    }
}
