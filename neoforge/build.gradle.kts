val modId: String by project
val minecraftVersion: String = libs.versions.minecraft.get()
val neoforgeVersion: String = libs.versions.neoforge.platform.get()
val neoforgeLoaderVersion: String = libs.versions.neoforge.loader.get()
val architecturyVersion: String = libs.versions.architectury.get()
val manualVersion: String = markdownManualVersion(libs.versions.manual.get())
val gameTestResultsDir = layout.buildDirectory.dir("test-results/gameTest")

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
            property("oc2.gameTest.junitDir", gameTestResultsDir.get().asFile.absolutePath)
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

val cleanGameTestResults = tasks.register<Delete>("cleanGameTestResults") {
    description = "Deletes game test results from previous runs."
    delete(gameTestResultsDir)
}

val fixGameTestReport = tasks.register("fixGameTestReport") {
    val reportFile = gameTestResultsDir.map { it.file("neoforge-game-tests.xml") }
    outputs.upToDateWhen { false }
    onlyIf { reportFile.get().asFile.exists() }
    doLast {
        val file = reportFile.get().asFile
        val document = javax.xml.parsers.DocumentBuilderFactory.newInstance()
            .newDocumentBuilder().parse(file)
        val root = document.documentElement
        var changed = false

        if (root.tagName == "testsuite" && root.getElementsByTagName("testsuite").length > 0) {
            document.renameNode(root, null, "testsuites")
            changed = true
        }

        val suites = document.getElementsByTagName("testsuite")
        for (i in 0 until suites.length) {
            val suite = suites.item(i) as org.w3c.dom.Element
            if (!suite.hasAttribute("name")) {
                suite.setAttribute("name", "gameTest")
                changed = true
            }
        }

        if (changed) {
            javax.xml.transform.TransformerFactory.newInstance().newTransformer()
                .transform(
                    javax.xml.transform.dom.DOMSource(document),
                    javax.xml.transform.stream.StreamResult(file)
                )
        }
    }
}

tasks.named("runGameTestServer") {
    dependsOn(cleanGameTestResults)
    finalizedBy(fixGameTestReport)
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
