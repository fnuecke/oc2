# Energy Storage

## High-level API
Device name: `energy_storage`

Provided by any energy storage connected through a [bus interface](../block/bus_interface.md), such as a [charger](../block/charger.md) or the batteries of other mods. Amounts are in the energy unit of the mod, the same one [robots](../item/robot.md) use.

With several storages connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.

### Methods

`canExtractEnergy():boolean`
Gets whether energy can be taken out of the storage.
- Returns whether the storage allows extracting energy.

`canReceiveEnergy():boolean`
Gets whether energy can be put into the storage.
- Returns whether the storage allows receiving energy.

`getEnergyStored():number`
Gets how much energy is currently stored.
- Returns the stored amount of energy.

`getMaxEnergyStored():number`
Gets how much energy can be stored at most.
- Returns the capacity of the storage.
