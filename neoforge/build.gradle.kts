val modId: String by project
val minecraftVersion: String = libs.versions.minecraft.get()
val neoforgeVersion: String = libs.versions.neoforge.platform.get()
val neoforgeLoaderVersion: String = libs.versions.neoforge.loader.get()
val architecturyVersion: String = libs.versions.architectury.get()
val manualVersion: String = markdownManualVersion(libs.versions.manual.get())

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    runs {
        named("client") { runDir = "run/client" }
        named("server") { runDir = "run/server" }

        create("gameTestServer") {
            server()
            runDir = "run/gametest"
            property("neoforge.gameTestServer", "true")
            property("neoforge.enabledGameTestNamespaces", "oc2gametest")
            vmArg("-ea")
        }

        create("data") {
            data()
            programArgs("--all")
            programArgs("--mod", modId)
            programArgs("--output", project(":common").file("src/generated/resources").absolutePath)
            programArgs("--existing", project(":common").file("src/main/resources").absolutePath)
            programArgs("--existing", file("src/main/resources").absolutePath)
        }
    }
}

val gameTestBlobDirectories = listOf(
    file("run/gametest/world/oc2-blobs"),
    file("run/gametest/world/oc2-blobs-trash"),
)

tasks.named("runGameTestServer") {
    doFirst {
        gameTestBlobDirectories.forEach { it.deleteRecursively() }
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    neoForge(libs.neoforge.platform)
    modImplementation(libs.neoforge.architectury)

    bundledLibs().forEach {
        "shadowBundle"(it)
        forgeRuntimeLibrary(it)
    }

    runtimeOnly(project(":instrumentation-neoforge"))
    runtimeOnly(project(":gametest-neoforge"))

    if (useLocalMarkdownManual) {
        modImplementation(files(markdownManualJar("neoforge", "markdown_manual-MC*-neoforge-*.jar")))
    } else {
        modImplementation(libs.neoforge.manual)
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraftVersion" to minecraftVersion,
            "loaderVersion" to neoforgeLoaderVersion,
            "neoforgeVersion" to neoforgeVersion,
            "architecturyVersion" to architecturyVersion,
            "manualVersion" to manualVersion
        )
        inputs.properties(properties)
        filesMatching("META-INF/neoforge.mods.toml") {
            expand(properties)
        }
    }

    remapJar {
        atAccessWideners.add("${modId}.accesswidener")
    }
}
