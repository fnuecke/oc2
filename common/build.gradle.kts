val enabledPlatforms = providers.gradleProperty("enabledPlatforms").get()
val modId = providers.gradleProperty("modId").get()

architectury {
    common(enabledPlatforms.split(","))
}

loom {
    accessWidenerPath.set(file("src/main/resources/${modId}.accesswidener"))
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)

    bundledLibs().forEach { api(it) }

    if (useLocalMarkdownManual) {
        compileOnly(files(markdownManualJar(".", "*-api.jar")))
    } else {
        compileOnly(libs.common.manual.api)
    }
    if (useLocalTis3d) {
        compileOnly(files(tis3dApiJar()))
    } else {
        compileOnly(libs.tis3d.api)
    }

    compileOnly(libs.jei.common.api)
    compileOnly(libs.cc.core.api)

    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
    testImplementation("org.mockito:mockito-core:5.11.0")
}

registerBlockModelsTask()

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
