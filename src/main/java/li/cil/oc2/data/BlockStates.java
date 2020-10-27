package li.cil.oc2.data;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.API;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.item.Item;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ExistingFileHelper;
import net.minecraftforge.fml.RegistryObject;

public class BlockStates extends BlockStateProvider {
    public BlockStates(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(OpenComputers.COMPUTER_BLOCK, OpenComputers.COMPUTER_ITEM);
        horizontalBlock(OpenComputers.REDSTONE_INTERFACE_BLOCK, OpenComputers.REDSTONE_INTERFACE_ITEM);
        horizontalBlock(OpenComputers.SCREEN_BLOCK, OpenComputers.SCREEN_ITEM);
    }

    private void horizontalBlock(final RegistryObject<Block> block, final RegistryObject<Item> item) {
        horizontalBlock(block.get(), models().getBuilder(block.getId().getPath()));
        itemModels().getBuilder(item.getId().getPath()).parent(models().getExistingFile(block.getId()));
    }
}
