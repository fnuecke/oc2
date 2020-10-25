package li.cil.oc2.data;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.api.API;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ExistingFileHelper;

public class BlockStates extends BlockStateProvider {
    public BlockStates(final DataGenerator generator, final ExistingFileHelper existingFileHelper) {
        super(generator, API.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(OpenComputers.COMPUTER_BLOCK.get(), models().getBuilder(OpenComputers.COMPUTER_BLOCK.getId().getPath()));

        itemModels().getBuilder(OpenComputers.COMPUTER_ITEM.getId().getPath())
                .parent(models().getExistingFile(OpenComputers.COMPUTER_BLOCK.getId()));
    }
}
