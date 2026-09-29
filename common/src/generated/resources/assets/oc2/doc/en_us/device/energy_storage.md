# Energy Storage

## High-level API
Device name: `energy_storage`

Provided by any energy storage connected through a [bus interface](../block/bus_interface.md), such as a [charger](../block/charger.md).

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

## Mid-level API
Device name: `ENERGY`

Amounts are four bytes, low byte first. Amounts past `0xFFFFFFFF` read as `0xFFFFFFFF`.

### Methods

`1 getEnergyStored`
Gets how much energy is currently stored.
- Returns four bytes, the stored amount of energy.

`2 getMaxEnergyStored`
Gets how much energy can be stored at most.
- Returns four bytes, the capacity of the storage.

`3 canExtractEnergy`
Gets whether energy can be taken out of the storage.
- Returns one byte, `1` if the storage allows extracting energy, `0` otherwise.

`4 canReceiveEnergy`
Gets whether energy can be put into the storage.
- Returns one byte, `1` if the storage allows receiving energy, `0` otherwise.
