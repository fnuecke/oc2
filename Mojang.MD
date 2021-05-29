**Last Modified**: 02/17/2021

At the time of this writing, the current license applied to the Official Mojang mapping files is:

> (c) 2020 Microsoft Corporation. These mappings are provided "as-is"
> and you bear the risk of using them. You may copy and use the mappings
> for development purposes, but you may not redistribute the mappings
> complete and unmodified. Microsoft makes no warranties, express or
> implied, with respect to the mappings provided here.  Use and
> modification of this document or the source code (in any form) of
> Minecraft: Java Edition is governed by the Minecraft End User License

Obtained From: [1.16.5's client.txt](https://launcher.mojang.com/v1/objects/374c6b789574afbdc901371207155661e0509e17/client.txt)

The first concern, which affects all developers most directly but is a lesser risk, is that the license terms for the official mappings don’t have any explicit provision for distributing mod code referencing Minecraft code under the official names. Since those references would have been derived from those mappings, directly or indirectly, they could still be considered derivative works used outside development, which means they may not be explicitly allowed, and so they must be covered by the fallback, the EULA, which states that mods can only distribute Mojang code/assets if they are not “substantial portions”.

The bigger concern, which affects projects like MCPConfig, is that in order to do our job, the data we create is derived in part from the official mappings, in order to patch the vanilla code, and make it usable as a platform for mods to run on. This usage of the data may not be considered development use, and so it’s the most at risk to be excluded by the license terms. Whether or not the EULA allows this use, is not clear.

We understand that the people at Mojang want this to be allowed, but the license does not clearly back their words, and puts us in this difficult position. However, we have almost a decade worth of working experience with the people at Mojang. To the best of our knowledge we are in good standing and they explicitly wish to allow us to use this data for our purposes. As such, we have decided to move forward with the license in it's current form.

We advise that everyone using this project, it's data, or anything that has something to do with the Official Mojang mappings be aware of the license they are under. You can make your own decision on how you want to proceed.