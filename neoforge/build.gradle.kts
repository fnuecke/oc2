val modId: String by project
val minecraftVersion: String = libs.versions.minecraft.get()
val neoforgeVersion: String = libs.versions.neoforge.platform.get()
val neoforgeLoaderVersion: String = libs.versions.neoforge.loader.get()
val architecturyVersion: String = libs.versions.architectury.get()
val manualVersion: String = markdownManualVersion(libs.versions.manual.get())

val gameTestRuntime: Configuration by configurations.creating
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")
val devOnlyMods: Configuration by configurations.creating
val devOnlyModNames = provider { devOnlyMods.resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.name } }

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    runs {
        named("client") { runDirectory.set(file("run/client")) }
        named("server") { runDirectory.set(file("run/server")) }

        create("gameTestServer") {
            server()
            runDirectory.set(file("run/gametest"))
            systemProperties.put("neoforge.gameTestServer", "true")
            systemProperties.put("neoforge.enabledGameTestNamespaces", "oc2_gametest")
            systemProperties.put("oc2.gameTest.junitDir", gameTestResultsDir.get().asFile.absolutePath)
            jvmArguments.add("-ea")
        }

        create("data") {
            data()
            programArguments.add("--all")
            programArguments.addAll("--mod", modId)
            programArguments.addAll("--output", project(":common").file("src/generated/resources").absolutePath)
            programArguments.addAll("--existing", project(":common").file("src/main/resources").absolutePath)
            programArguments.addAll("--existing", file("src/main/resources").absolutePath)
        }
    }
}

val gameTestBlobDirectories = listOf(
    file("run/gametest/world/oc2-blobs"),
    file("run/gametest/world/oc2-blobs-trash"),
)

val cleanGameTestResults = tasks.register<Delete>("cleanGameTestResults") {
    description = "Deletes game test results and the scratch world from previous runs."
    delete(gameTestResultsDir)
    delete(layout.projectDirectory.dir("run/gametest/world"))
}

val fixGameTestReport = tasks.register("fixGameTestReport") {
    val reportFile = gameTestResultsDir.map { it.file("neoforge-game-tests.xml") }
    outputs.upToDateWhen { false }
    doLast {
        normalizeGameTestReport(reportFile.get().asFile)
    }
}

tasks.named<JavaExec>("runGameTestServer") {
    dependsOn(cleanGameTestResults)
    classpath += gameTestRuntime
    classpath = classpath.filter { file -> devOnlyModNames.get().none { file.name.startsWith("${it}-") } }
    finalizedBy(fixGameTestReport)
    doFirst {
        gameTestBlobDirectories.forEach { it.deleteRecursively() }
    }
}

repositories {
    maven("https://maven.neoforged.net/releases")
}

configurations.named("modRuntimeOnly") { extendsFrom(devOnlyMods) }

dependencies {
    neoForge(libs.neoforge.platform)
    modImplementation(libs.neoforge.architectury)

    // Allows `remapSourcesJar` to resolve `@ExpectPlatform` in the common sources it bundles.
    compileOnly(libs.architectury.injectables)

    // Not used by mod, just for dev convenience.
    devOnlyMods(libs.jei.neoforge)

    gameTestRuntime(project(":gametest-neoforge"))

    if (useLocalMarkdownManual) {
        modImplementation(files(markdownManualJar("neoforge", "neoforge-*.jar")))
    } else {
        modImplementation(libs.neoforge.manual)
    }

    bundledLibs().forEach {
        "shadowBundle"(it)
        forgeRuntimeLibrary(it)
    }

    runtimeOnly(project(":instrumentation-neoforge"))
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
