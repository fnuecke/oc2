val enabledPlatforms: String by project
val modId: String by project

architectury {
    common(enabledPlatforms.split(","))
}

loom {
    accessWidenerPath.set(file("src/main/resources/${modId}.accesswidener"))
}

val ceresJar = localLibJar("../ceres/build/libs", "ceres-*.jar")
val sednaJar = localLibJar("../sedna/build/libs", "sedna-*.jar")
val buildrootJar = localLibJar("../buildroot/build/libs", "sedna-buildroot-*.jar")

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)

    if (ceresJar != null) api(files(ceresJar)) else api(libs.ceres)
    if (sednaJar != null) api(files(sednaJar)) else api(libs.sedna)
    if (buildrootJar != null) api(files(buildrootJar)) else api(libs.sedna.buildroot)

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
    }

    processResources {
        dependsOn("packageScripts")
    }

    register<Jar>("apiJar") {
        from(sourceSets.main.get().allSource)
        from(sourceSets.main.get().output)
        archiveClassifier.set("api")
        include("li/cil/${modId}/api/**")
    }

    jar {
        dependsOn("apiJar")
    }

    test {
        useJUnitPlatform()
    }
}
