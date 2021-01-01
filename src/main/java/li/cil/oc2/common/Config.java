package li.cil.oc2.common;

import li.cil.oc2.api.API;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

@Mod.EventBusSubscriber(modid = API.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class Config {
    private static final CommonSettings COMMON_INSTANCE;
    private static final ForgeConfigSpec COMMON_SPEC;

    ///////////////////////////////////////////////////////////////////

    public static long maxAllocatedMemory = 512 * Constants.MEGABYTE;
    public static int maxMemorySize = 8 * Constants.MEGABYTE;
    public static int maxHardDriveSize = 8 * Constants.MEGABYTE;
    public static int maxFlashMemorySize = 4 * Constants.KILOBYTE;

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
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class CommonSettings {
        public ForgeConfigSpec.LongValue maxAllocatedMemory;
        public ForgeConfigSpec.IntValue maxMemorySize;
        public ForgeConfigSpec.IntValue maxHardDriveSize;
        public ForgeConfigSpec.IntValue maxFlashMemorySize;

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
        }
    }
}
