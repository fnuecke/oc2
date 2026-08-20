val modId: String by project
val minecraftVersion: String = libs.versions.minecraft.get()
val fabricLoaderVersion: String = libs.versions.fabric.loader.get()
val fabricApiVersion: String = libs.versions.fabric.api.get()
val architecturyVersion: String = libs.versions.architectury.get()
val forgeConfigPortVersion: String = libs.versions.fabric.forgeConfigPort.get()
val manualVersion: String = markdownManualVersion(libs.versions.manual.get())
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")

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
            property("oc2.gameTest.junitDir", gameTestResultsDir.get().asFile.absolutePath)
            vmArg("-ea")
        }
    }
}

val gameTestBlobDirectories = listOf(
    file("run/gametest/world/oc2-blobs"),
    file("run/gametest/world/oc2-blobs-trash"),
)

val cleanGameTestResults = tasks.register<Delete>("cleanGameTestResults") {
    description = "Deletes game test results from previous runs."
    delete(gameTestResultsDir)
}

tasks.named("runGameTest") {
    dependsOn(cleanGameTestResults)
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
    runtimeOnly(project(path = ":gametest-fabric", configuration = "namedElements")) { isTransitive = false }

    "common"(project(path = ":instrumentation-common", configuration = "namedElements")) { isTransitive = false }
    "common"(project(path = ":gametest-common", configuration = "namedElements")) { isTransitive = false }

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
