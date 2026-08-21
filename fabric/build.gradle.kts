val modId: String by project
val minecraftVersion: String = libs.versions.minecraft.get()
val fabricLoaderVersion: String = libs.versions.fabric.loader.get()
val fabricApiVersion: String = libs.versions.fabric.api.get()
val architecturyVersion: String = libs.versions.architectury.get()
val forgeConfigPortVersion: String = libs.versions.fabric.forgeConfigPort.get()
val manualVersion: String = markdownManualVersion(libs.versions.manual.get())

val gameTestRuntime: Configuration by configurations.creating
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")
val devOnlyMods: Configuration by configurations.creating
val devOnlyModNames = provider { devOnlyMods.resolvedConfiguration.resolvedArtifacts.map { it.moduleVersion.id.name } }

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)

    runs {
        named("client") { runDir = "run/client" }
        named("server") { runDir = "run/server" }

        create("gameTest") {
            server()
            runDir = "run/gametest"
            vmArg("-Dfabric-api.gametest")
            vmArg("-Dfabric-api.gametest.report-file=${gameTestResultsDir.get().asFile.absolutePath}/fabric-game-tests.xml")
            vmArg("-ea")
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
    val reportFile = gameTestResultsDir.map { it.file("fabric-game-tests.xml") }
    outputs.upToDateWhen { false }
    doLast {
        normalizeGameTestReport(reportFile.get().asFile)
    }
}

tasks.named<JavaExec>("runGameTest") {
    dependsOn(cleanGameTestResults)
    classpath += gameTestRuntime
    classpath = classpath.filter { file -> devOnlyModNames.get().none { file.name.startsWith("${it}-") } }
    finalizedBy(fixGameTestReport)
    doFirst {
        gameTestBlobDirectories.forEach { it.deleteRecursively() }
    }
}

repositories {
    exclusiveContent {
        forRepository { maven("https://maven.fabricmc.net/") }
        filter { includeGroup("teamreborn") }
    }
    exclusiveContent {
        forRepository { maven("https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") }
        filter { includeGroup("fuzs.forgeconfigapiport") }
    }
}

configurations.named("modRuntimeOnly") { extendsFrom(devOnlyMods) }

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.fabric.api)
    modApi(libs.fabric.architectury)

    include(modApi(libs.fabric.energy.get().toString()) {
        exclude(group = "net.fabricmc.fabric-api")
    })

    modImplementation(libs.fabric.forgeConfigPort)

    bundledLibs().forEach {
        "shadowBundle"(it)
        implementation(it)
    }

    runtimeOnly(project(path = ":instrumentation-fabric", configuration = "namedElements")) { isTransitive = false }

    // Not used by mod, just for dev convenience.
    devOnlyMods(libs.jei.fabric)

    gameTestRuntime(project(path = ":gametest-fabric", configuration = "namedElements")) { isTransitive = false }

    "common"(project(path = ":instrumentation-common", configuration = "namedElements")) { isTransitive = false }

    if (useLocalMarkdownManual) {
        modImplementation(files(markdownManualJar("fabric", "markdown_manual-MC*-fabric-*.jar")))
    } else {
        modImplementation(libs.fabric.manual)
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "minecraftVersion" to minecraftVersion,
            "loaderVersion" to fabricLoaderVersion,
            "fabricApiVersion" to fabricApiVersion,
            "architecturyVersion" to architecturyVersion,
            "forgeConfigPortVersion" to forgeConfigPortVersion,
            "manualVersion" to manualVersion
        )
        inputs.properties(properties)
        filesMatching("fabric.mod.json") {
            expand(properties)
        }
    }

    remapJar {
        injectAccessWidener.set(true)
    }
}
