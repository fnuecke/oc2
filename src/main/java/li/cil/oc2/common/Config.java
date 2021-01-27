package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final CommonSettings COMMON_INSTANCE;
    private static final ForgeConfigSpec COMMON_SPEC;

    ///////////////////////////////////////////////////////////////////

    public static long maxAllocatedMemory = 512 * Constants.MEGABYTE;
    public static int maxMemorySize = 8 * Constants.MEGABYTE;
    public static int maxHardDriveSize = 8 * Constants.MEGABYTE;
    public static int maxFlashMemorySize = 4 * Constants.KILOBYTE;
    public static int maxFloppySize = 512 * Constants.KILOBYTE;

    public static int blockOperationsModuleToolLevel = Items.DIAMOND_PICKAXE.getHarvestLevel(new ItemStack(Items.DIAMOND_PICKAXE), ToolType.PICKAXE, null, null);

    public static UUID fakePlayerUUID = UUID.fromString("e39dd9a7-514f-4a2d-aa5e-b6030621416d");

    ///////////////////////////////////////////////////////////////////

    static {
        final Pair<CommonSettings, ForgeConfigSpec> commonConfig = new ForgeConfigSpec.Builder().configure(CommonSettings::new);
        COMMON_INSTANCE = commonConfig.getKey();
        COMMON_SPEC = commonConfig.getValue();
    }

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfig.ModConfigEvent event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            maxAllocatedMemory = COMMON_INSTANCE.maxAllocatedMemory.get();
            maxMemorySize = COMMON_INSTANCE.maxMemorySize.get();
            maxHardDriveSize = COMMON_INSTANCE.maxHardDriveSize.get();
            maxFlashMemorySize = COMMON_INSTANCE.maxFlashMemorySize.get();

            fakePlayerUUID = UUID.fromString(COMMON_INSTANCE.fakePlayerUUID.get());
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class CommonSettings {
        public ForgeConfigSpec.LongValue maxAllocatedMemory;
        public ForgeConfigSpec.IntValue maxMemorySize;
        public ForgeConfigSpec.IntValue maxHardDriveSize;
        public ForgeConfigSpec.IntValue maxFlashMemorySize;

        public ForgeConfigSpec.IntValue blockOperationsModuleToolLevel;

        public ForgeConfigSpec.ConfigValue<String> fakePlayerUUID;

        public CommonSettings(final ForgeConfigSpec.Builder builder) {
            builder.push("vm");

            maxAllocatedMemory = builder
                    .translation(Constants.CONFIG_MAX_ALLOCATED_MEMORY)
                    .comment("The maximum amount of memory that may be allocated to run virtual machines.")
                    .defineInRange("maxAllocatedMemory", Config.maxAllocatedMemory, 0L, 64L * Constants.GIGABYTE);

            maxMemorySize = builder
                    .translation(Constants.CONFIG_MAX_MEMORY_SIZE)
                    .comment("The maximum size of a single memory device.")
                    .defineInRange("maxMemorySize", Config.maxMemorySize, 0, 256 * Constants.MEGABYTE);

            maxHardDriveSize = builder
                    .translation(Constants.CONFIG_MAX_HARD_DRIVE_SIZE)
                    .comment("The maximum size of a single hard drive device.")
                    .defineInRange("maxHardDriveSize", Config.maxHardDriveSize, 0, 512 * Constants.MEGABYTE);

            maxFlashMemorySize = builder
                    .translation(Constants.CONFIG_MAX_FLASH_MEMORY_SIZE)
                    .comment("The maximum size of a single flash memory device.")
                    .defineInRange("maxFlashMemorySize", Config.maxFlashMemorySize, 0, 128 * Constants.MEGABYTE);

            builder.pop();

            builder.push("gameplay");

            blockOperationsModuleToolLevel = builder
                    .translation(Constants.CONFIG_BLOCK_OPERATIONS_MODULE_TOOL_LEVEL)
                    .comment("Tool level the block operations module operates at.")
                    .defineInRange("modules.block_operations.toolLevel", Config.blockOperationsModuleToolLevel, 0, Integer.MAX_VALUE);

            builder.pop();

            builder.push("admin");

            fakePlayerUUID = builder
                    .translation(Constants.CONFIG_FAKE_PLAYER_UUID)
                    .comment("The UUID used for the ForgeFakePlayer used by robots.")
                    .define("fakePlayerUUID", Config.fakePlayerUUID.toString());

            builder.pop();
        }
    }
}
