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

    public static double busCableEnergyPerTick = 0.1;
    public static double busInterfaceEnergyPerTick = 0.5;
    public static int computerEnergyPerTick = 10;
    public static int computerEnergyStorage = 2000;
    public static int robotEnergyPerTick = 5;
    public static int robotEnergyStorage = 750000;
    public static int chargerEnergyPerTick = 2500;
    public static int chargerEnergyStorage = 10000;

    public static double memoryEnergyPerMegabytePerTick = 0.5;
    public static double hardDriveEnergyPerMegabytePerTick = 1;
    public static int networkInterfaceEnergyPerTick = 1;
    public static int redstoneInterfaceCardEnergyPerTick = 1;
    public static int blockOperationsModuleEnergyPerTick = 2;
    public static int inventoryOperationsModuleEnergyPerTick = 1;

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

    public static boolean computersUseEnergy() {
        return computerEnergyPerTick > 0 && computerEnergyStorage > 0;
    }

    public static boolean robotsUseEnergy() {
        return robotEnergyPerTick > 0 && robotEnergyStorage > 0;
    }

    public static boolean chargerUseEnergy() {
        return chargerEnergyPerTick > 0 && chargerEnergyStorage > 0;
    }

    ///////////////////////////////////////////////////////////////////

    @SubscribeEvent
    public static void handleModConfigEvent(final ModConfig.ModConfigEvent event) {
        if (event.getConfig().getSpec() == COMMON_SPEC) {
            maxAllocatedMemory = COMMON_INSTANCE.maxAllocatedMemory.get();
            maxMemorySize = COMMON_INSTANCE.maxMemorySize.get();
            maxHardDriveSize = COMMON_INSTANCE.maxHardDriveSize.get();
            maxFlashMemorySize = COMMON_INSTANCE.maxFlashMemorySize.get();

            busCableEnergyPerTick = COMMON_INSTANCE.busCableEnergyPerTick.get();
            busInterfaceEnergyPerTick = COMMON_INSTANCE.busInterfaceEnergyPerTick.get();

            computerEnergyPerTick = COMMON_INSTANCE.computerEnergyPerTick.get();
            computerEnergyStorage = COMMON_INSTANCE.computerEnergyStorage.get();
            robotEnergyPerTick = COMMON_INSTANCE.robotEnergyPerTick.get();
            robotEnergyStorage = COMMON_INSTANCE.robotEnergyStorage.get();
            chargerEnergyPerTick = COMMON_INSTANCE.chargerEnergyPerTick.get();
            chargerEnergyStorage = COMMON_INSTANCE.chargerEnergyStorage.get();

            memoryEnergyPerMegabytePerTick = COMMON_INSTANCE.memoryEnergyPerMegabytePerTick.get();
            hardDriveEnergyPerMegabytePerTick = COMMON_INSTANCE.hardDriveEnergyPerMegabytePerTick.get();
            networkInterfaceEnergyPerTick = COMMON_INSTANCE.networkInterfaceEnergyPerTick.get();
            redstoneInterfaceCardEnergyPerTick = COMMON_INSTANCE.redstoneInterfaceCardEnergyPerTick.get();
            blockOperationsModuleEnergyPerTick = COMMON_INSTANCE.blockOperationsModuleEnergyPerTick.get();
            inventoryOperationsModuleEnergyPerTick = COMMON_INSTANCE.inventoryOperationsModuleEnergyPerTick.get();


            blockOperationsModuleToolLevel = COMMON_INSTANCE.blockOperationsModuleToolLevel.get();

            fakePlayerUUID = UUID.fromString(COMMON_INSTANCE.fakePlayerUUID.get());
        }
    }

    ///////////////////////////////////////////////////////////////////

    private static final class CommonSettings {
        public final ForgeConfigSpec.LongValue maxAllocatedMemory;
        public final ForgeConfigSpec.IntValue maxMemorySize;
        public final ForgeConfigSpec.IntValue maxHardDriveSize;
        public final ForgeConfigSpec.IntValue maxFlashMemorySize;

        public final ForgeConfigSpec.DoubleValue busCableEnergyPerTick;
        public final ForgeConfigSpec.DoubleValue busInterfaceEnergyPerTick;
        public final ForgeConfigSpec.IntValue computerEnergyPerTick;
        public final ForgeConfigSpec.IntValue computerEnergyStorage;
        public final ForgeConfigSpec.IntValue robotEnergyPerTick;
        public final ForgeConfigSpec.IntValue robotEnergyStorage;
        public final ForgeConfigSpec.IntValue chargerEnergyPerTick;
        public final ForgeConfigSpec.IntValue chargerEnergyStorage;

        public final ForgeConfigSpec.DoubleValue memoryEnergyPerMegabytePerTick;
        public final ForgeConfigSpec.DoubleValue hardDriveEnergyPerMegabytePerTick;
        public final ForgeConfigSpec.IntValue networkInterfaceEnergyPerTick;
        public final ForgeConfigSpec.IntValue redstoneInterfaceCardEnergyPerTick;
        public final ForgeConfigSpec.IntValue blockOperationsModuleEnergyPerTick;
        public final ForgeConfigSpec.IntValue inventoryOperationsModuleEnergyPerTick;

        public final ForgeConfigSpec.IntValue blockOperationsModuleToolLevel;

        public final ForgeConfigSpec.ConfigValue<String> fakePlayerUUID;

        public CommonSettings(final ForgeConfigSpec.Builder builder) {
            builder.push("vm");
            {
                maxAllocatedMemory = builder.defineInRange("maxAllocatedMemory", Config.maxAllocatedMemory, 0L, 64L * Constants.GIGABYTE);
                maxMemorySize = builder.defineInRange("maxMemorySize", Config.maxMemorySize, 0, 256 * Constants.MEGABYTE);
                maxHardDriveSize = builder.defineInRange("maxHardDriveSize", Config.maxHardDriveSize, 0, 512 * Constants.MEGABYTE);
                maxFlashMemorySize = builder.defineInRange("maxFlashMemorySize", Config.maxFlashMemorySize, 0, 128 * Constants.MEGABYTE);
            }
            builder.pop();

            builder.push("energy");
            {
                builder.push("blocks");
                {
                    busCableEnergyPerTick = builder.defineInRange("busCableEnergyPerTick", Config.busCableEnergyPerTick, 0, Integer.MAX_VALUE);
                    busInterfaceEnergyPerTick = builder.defineInRange("busInterfaceEnergyPerTick", Config.busInterfaceEnergyPerTick, 0, Integer.MAX_VALUE);
                    computerEnergyPerTick = builder.defineInRange("computerEnergyPerTick", Config.computerEnergyPerTick, 0, Integer.MAX_VALUE);
                    computerEnergyStorage = builder.defineInRange("computerEnergyStorage", Config.computerEnergyStorage, 0, Integer.MAX_VALUE);
                    robotEnergyPerTick = builder.defineInRange("robotEnergyPerTick", Config.robotEnergyPerTick, 0, Integer.MAX_VALUE);
                    robotEnergyStorage = builder.defineInRange("robotEnergyStorage", Config.robotEnergyStorage, 0, Integer.MAX_VALUE);
                    chargerEnergyPerTick = builder.defineInRange("chargerEnergyPerTick", Config.chargerEnergyPerTick, 0, Integer.MAX_VALUE);
                    chargerEnergyStorage = builder.defineInRange("chargerEnergyStorage", Config.chargerEnergyStorage, 0, Integer.MAX_VALUE);
                }
                builder.pop();

                builder.push("items");
                {
                    memoryEnergyPerMegabytePerTick = builder.defineInRange("memoryEnergyPerMegabytePerTick", Config.memoryEnergyPerMegabytePerTick, 0, Integer.MAX_VALUE);
                    hardDriveEnergyPerMegabytePerTick = builder.defineInRange("hardDriveEnergyPerMegabytePerTick", Config.hardDriveEnergyPerMegabytePerTick, 0, Integer.MAX_VALUE);
                    networkInterfaceEnergyPerTick = builder.defineInRange("networkInterfaceEnergyPerTick", Config.networkInterfaceEnergyPerTick, 0, Integer.MAX_VALUE);
                    redstoneInterfaceCardEnergyPerTick = builder.defineInRange("redstoneInterfaceCardEnergyPerTick", Config.redstoneInterfaceCardEnergyPerTick, 0, Integer.MAX_VALUE);
                    blockOperationsModuleEnergyPerTick = builder.defineInRange("blockOperationsModuleEnergyPerTick", Config.blockOperationsModuleEnergyPerTick, 0, Integer.MAX_VALUE);
                    inventoryOperationsModuleEnergyPerTick = builder.defineInRange("inventoryOperationsModuleEnergyPerTick", Config.inventoryOperationsModuleEnergyPerTick, 0, Integer.MAX_VALUE);
                }
                builder.pop();
            }
            builder.pop();

            builder.push("gameplay");
            {
                blockOperationsModuleToolLevel = builder.defineInRange("modules.blockOperations.toolLevel", Config.blockOperationsModuleToolLevel, 0, Integer.MAX_VALUE);
            }
            builder.pop();

            builder.push("admin");
            {
                fakePlayerUUID = builder.define("fakePlayerUUID", Config.fakePlayerUUID.toString());
            }
            builder.pop();
        }
    }
}
