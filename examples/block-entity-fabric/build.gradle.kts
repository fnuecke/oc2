plugins {
    id("fabric-loom") version "1.17.21"
}

version = "1.0.0"
group = "com.example.blockentity"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    maven("https://fnuecke.github.io/maven") {
        content {
            includeGroup("li.cil.oc2")
            includeGroup("li.cil.markdown_manual")
        }
    }
    maven("https://maven.architectury.dev") {
        content { includeGroup("dev.architectury") }
    }
    maven("https://maven.fabricmc.net") {
        content { includeGroup("teamreborn") }
    }
    maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
        content { includeGroup("fuzs.forgeconfigapiport") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.21.11")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:0.19.3")
    modImplementation("net.fabricmc.fabric-api:fabric-api:0.141.6+1.21.11")

    modImplementation("li.cil.oc2:oc2-1.21.11-fabric:0.7.1") { isTransitive = false }

    modLocalRuntime("dev.architectury:architectury-fabric:19.0.1")
    modLocalRuntime("fuzs.forgeconfigapiport:forgeconfigapiport-fabric:21.11.1")
    modLocalRuntime("teamreborn:energy:4.2.0") { isTransitive = false }
    modLocalRuntime("li.cil.markdown_manual:markdown_manual-1.21.11-fabric:1.2.7")
}
