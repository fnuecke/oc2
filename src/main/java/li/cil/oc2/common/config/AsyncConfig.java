package li.cil.oc2.common.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class AsyncConfig {
    public static final ServerConfig SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;

    static {
        final Pair<ServerConfig, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(ServerConfig::new);
        SERVER_SPEC = specPair.getRight();
        SERVER = specPair.getLeft();
    }

    public static class ServerConfig {
        public final ForgeConfigSpec.BooleanValue enableSuperDebug;
        public final ForgeConfigSpec.BooleanValue runAsyncTests;
        public final ForgeConfigSpec.BooleanValue asyncStorageOperations;

        ServerConfig(final ForgeConfigSpec.Builder builder) {
            builder.push("async");
            
            enableSuperDebug = builder
                .comment("Enable super debug mode for async operations. This will log stack traces for all async operations.")
                .translation("config.oc2.async.super_debug")
                .define("superDebug", false);
                
            runAsyncTests = builder
                .comment("Run async operation tests during server startup. Enable this to verify async functionality.")
                .define("runAsyncTests", true);
                
            asyncStorageOperations = builder
                .comment("Enables asynchronous storage operations for better performance")
                .translation("config.oc2.async.storage_operations")
                .define("storageOperations", true);
                
            builder.pop();
        }
    }

    // Prevent instantiation
    private AsyncConfig() {}
}
