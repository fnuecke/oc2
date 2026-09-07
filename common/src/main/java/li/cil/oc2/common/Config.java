/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.common.config.*;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@WorldRestart
@Type(ConfigType.SERVER)
public final class Config {
    @Path("vm")
    public static long maxAllocatedMemory = 2L * 1024 * Constants.MEGABYTE;

    @Path("vm")
    @Min(0)
    @Comment({
        "Number of worker threads used to run virtual machines.",
        "Leave zero for automatic. Controls overall machine load. Server tick is not measurably",
        "impacted by virtual machines, this just controls overall maximum machine load."
    })
    public static int workerCount;

    @Path("vm")
    @Min(1)
    @Comment("Cycles a RISC-V computer may retire per second, i.e. its clock speed.")
    public static int riscvCycleBudgetPerSecond = 25_000_000;

    @Path("vm")
    @Min(1)
    @Comment("Cycles a Z80 computer may retire per second, i.e. its clock speed.")
    public static int z80CycleBudgetPerSecond = 4_000_000;

    @Path("energy.blocks")
    public static double busCableEnergyPerTick = 0.1;
    @Path("energy.blocks")
    public static double busInterfaceEnergyPerTick = 0.5;
    @Path("energy.blocks")
    public static int computerEnergyStorage = 2000;
    @Path("energy.blocks")
    public static int chargerEnergyPerTick = 2500;
    @Path("energy.blocks")
    public static int chargerEnergyStorage = 10000;
    @Path("energy.blocks")
    public static int projectorEnergyPerTick = 20;
    @Path("energy.blocks")
    public static int projectorEnergyStorage = 2000;
    @Path("energy.blocks")
    public static int internetGatewayEnergyPerPacket = 20;
    @Path("energy.blocks")
    public static int internetGatewayEnergyStorage = 2000;

    @Path("energy.entities")
    public static double robotCpuEnergyMultiplier = 0.5;
    @Path("energy.entities")
    public static int robotEnergyStorage = 750000;

    @Path("energy.items")
    public static int riscvCpuEnergyPerTick = 10;
    @Path("energy.items")
    public static int z80CpuEnergyPerTick = 2;
    @Path("energy.items")
    public static double memoryEnergyPerMegabytePerTick = 0.5;
    @Path("energy.items")
    public static double hardDriveEnergyPerMegabytePerTick = 1;
    @Path("energy.items")
    public static int redstoneInterfaceCardEnergyPerTick = 1;
    @Path("energy.items")
    public static int networkInterfaceEnergyPerTick = 1;
    @Path("energy.items")
    public static int fileImportExportCardEnergyPerTick = 1;
    @Path("energy.items")
    public static int soundCardEnergyPerTick = 1;
    @Path("energy.items")
    public static int blockOperationsModuleEnergyPerTick = 2;
    @Path("energy.items")
    public static int inventoryOperationsModuleEnergyPerTick = 1;
    @Path("energy.items")
    public static int networkTunnelEnergyPerTick = 2;

    @Path("gameplay")
    public static long soundCardCoolDownSeconds = 2;

    @Path("admin")
    public static UUID fakePlayerUUID = UUID.fromString("e39dd9a7-514f-4a2d-aa5e-b6030621416d");

    @Path("admin.storage")
    @Min(0)
    public static int maxBlobCount = 1024;
    @Path("admin.storage")
    @Min(0)
    public static int maxTrashedBlobCount = 64;
    @Path("admin.storage")
    @Min(0)
    public static int blobEvictionGraceHours = 24 * 7;
    @Path("admin.storage")
    @Min(0)
    public static int maxBlobCapacity = 16 * Constants.MEGABYTE;

    @Path("admin.network")
    public static int projectorAverageMaxBytesPerSecond = 160 * 1024;
    @Path("admin.virtual_network")
    public static int ethernetFrameTimeToLive = 12;
    @Path("admin.virtual_network")
    public static int hubEthernetFramesPerTick = 32;

    @Path("admin.internet")
    @Comment({
        "Whether computers may open connections to the outside world.",
        "Traffic leaves from this server's address, so anything a computer reaches sees the",
        "connection as coming from the server, not from the player. The address and port filters",
        "below decide what is reachable; turn this off to refuse everything."
    })
    public static boolean internetEnabled = true;

    @Path("admin.internet")
    @Comment({
        "Addresses computers may NOT reach, as address (1.2.3.4), CIDR block (10.0.0.0/8),",
        "inclusive range (1.2.3.4-1.2.3.9), local interface (@eth0 or @2), or host name.",
        "Host names are re-resolved periodically.",
        "Loopback, multicast, broadcast and the cloud instance metadata address are always denied.",
        "The defaults add the private and link-local ranges. They cannot cover this server's own",
        "public address, which behind NAT is on none of its interfaces: add it by hand if computers",
        "must not dial back into services this machine hosts."
    })
    @ItemType(String.class)
    public static List<String> internetDeniedHosts = new ArrayList<>(List.of(
        "10.0.0.0/8",
        "100.64.0.0/10",
        "169.254.0.0/16",
        "172.16.0.0/12",
        "192.0.0.0/24",
        "192.168.0.0/16",
        "198.18.0.0/15",
        "192.88.99.0/24"
    ));

    @Path("admin.internet")
    @Comment({
        "Addresses computers MAY reach, in the same syntax as the denied list.",
        "Empty allows everything that is not denied. With any entry present, only listed addresses",
        "are permitted, minus anything the denied list covers."
    })
    @ItemType(String.class)
    public static List<String> internetAllowedHosts = new ArrayList<>();

    @Path("admin.internet")
    @Comment({
        "Deny every subnet this server's own network interfaces sit on.",
        "Re-resolved periodically so interfaces brought up later are covered, too."
    })
    public static boolean internetDenyLocalSubnets = true;

    @Path("admin.internet")
    @Comment("Ports computers may NOT connect to, as single ports or inclusive from-to ranges.")
    @ItemType(String.class)
    public static List<String> internetDeniedPorts = new ArrayList<>(List.of(
        "25", "465", "587",
        "137-139", "445",
        "1900",
        "3389",
        "11211"
    ));

    @Path("admin.internet")
    @Comment("Concurrent connections across the whole server.")
    @Min(1)
    @Max(4096)
    public static int internetSessionsTotal = 128;

    @Path("admin.internet")
    @Comment("Maximum throughput for one gateway, in bytes per second, each way.")
    @Min(30280)
    public static int internetBytesPerSecond = 64 * 1024;

    @Path("admin.internet")
    @Comment("Maximum throughput across all gateways on the server, in bytes per second, each way.")
    @Min(30280)
    public static int internetBytesPerSecondTotal = 512 * 1024;

    public static int cpuEnergyPerTick(@Nullable final ArchitectureType architecture) {
        if (architecture == null) {
            return 0;
        }
        return switch (architecture) {
            case RISCV -> riscvCpuEnergyPerTick;
            case Z80 -> z80CpuEnergyPerTick;
        };
    }

    public static int robotCpuEnergyPerTick(@Nullable final ArchitectureType architecture) {
        return (int) Math.round(cpuEnergyPerTick(architecture) * robotCpuEnergyMultiplier);
    }

    public static boolean computersUseEnergy() {
        return computerEnergyStorage > 0;
    }

    public static boolean chargersUseEnergy() {
        return chargerEnergyPerTick > 0 && chargerEnergyStorage > 0;
    }

    public static boolean projectorsUseEnergy() {
        return projectorEnergyStorage > 0 && projectorEnergyPerTick > 0;
    }

    public static boolean robotsUseEnergy() {
        return robotEnergyStorage > 0;
    }

    public static boolean internetGatewaysUseEnergy() {
        return internetGatewayEnergyPerPacket > 0 && internetGatewayEnergyStorage > 0;
    }
}
