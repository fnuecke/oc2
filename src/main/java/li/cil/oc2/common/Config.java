package li.cil.oc2.common;

import de.siphalor.tweed.config.ConfigEnvironment;
import de.siphalor.tweed.config.ConfigScope;
import de.siphalor.tweed.config.annotated.AConfigEntry;
import de.siphalor.tweed.config.annotated.ATweedConfig;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TieredItem;

import java.util.UUID;

@ATweedConfig(
        scope = ConfigScope.SMALLEST,
        environment = ConfigEnvironment.SYNCED,
        tailors = "tweed:cloth"
)
public final class Config {
    ///////////////////////////////////////////////////////////////////
    @AConfigEntry
    public static long maxAllocatedMemory = 512 * Constants.MEGABYTE;
    @AConfigEntry
    public static int maxMemorySize = 8 * Constants.MEGABYTE;
    @AConfigEntry
    public static int maxHardDriveSize = 8 * Constants.MEGABYTE;
    @AConfigEntry
    public static int maxFlashMemorySize = 4 * Constants.KILOBYTE;
    @AConfigEntry
    public static int maxFloppySize = 512 * Constants.KILOBYTE;
    @AConfigEntry
    public static double busCableEnergyPerTick = 0.1;
    @AConfigEntry
    public static double busInterfaceEnergyPerTick = 0.5;
    @AConfigEntry
    public static int computerEnergyPerTick = 10;
    @AConfigEntry
    public static int computerEnergyStorage = 2000;
    @AConfigEntry
    public static int robotEnergyPerTick = 5;
    @AConfigEntry
    public static int robotEnergyStorage = 750000;
    @AConfigEntry
    public static int chargerEnergyPerTick = 2500;
    @AConfigEntry
    public static int chargerEnergyStorage = 10000;
    @AConfigEntry
    public static double memoryEnergyPerMegabytePerTick = 0.5;
    @AConfigEntry
    public static double hardDriveEnergyPerMegabytePerTick = 1;
    @AConfigEntry
    public static int redstoneInterfaceCardEnergyPerTick = 1;
    @AConfigEntry
    public static int networkInterfaceEnergyPerTick = 1;
    @AConfigEntry
    public static int fileImportExportCardEnergyPerTick = 1;
    @AConfigEntry
    public static int blockOperationsModuleEnergyPerTick = 2;
    @AConfigEntry
    public static int inventoryOperationsModuleEnergyPerTick = 1;
    @AConfigEntry
    public static int blockOperationsModuleToolLevel = ((TieredItem)Items.DIAMOND_PICKAXE).getTier().getLevel();

    @AConfigEntry
    public static UUID fakePlayerUUID = UUID.fromString("e39dd9a7-514f-4a2d-aa5e-b6030621416d");
}
