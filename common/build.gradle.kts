val enabledPlatforms: String by project
val modId: String by project

architectury {
    common(enabledPlatforms.split(","))
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

loom {
    accessWidenerPath.set(file("src/main/resources/${modId}.accesswidener"))
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)

    bundledLibs().forEach { api(it) }

    if (useLocalMarkdownManual) {
        compileOnly(files(markdownManualJar("common", "markdown_manual-MC*-common-*-api.jar")))
    } else {
        modApi(libs.fabric.manual)
    }

    compileOnly(libs.jei.common.api)

    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

tasks {
    register<Zip>("packageScripts") {
        archiveFileName.set("scripts.zip")
        destinationDirectory.set(layout.buildDirectory.dir("resources/main/data/${modId}/file_systems"))
        from("src/main/scripts")
        exclude("**/__pycache__/**")
    }

    processResources {
        dependsOn("packageScripts")
    }

    test {
        useJUnitPlatform()
    }
}
