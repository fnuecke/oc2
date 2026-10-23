/* SPDX-License-Identifier: MIT */

package li.cil.oc2.data.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public final class DataGenerators implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(final FabricDataGenerator generator) {
        final var pack = generator.createPack();
        pack.addProvider(ModModelProvider::new);
    }
}
