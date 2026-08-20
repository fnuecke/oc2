val enabledPlatforms: String by project

architectury {
    common(enabledPlatforms.split(","))
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)

    compileOnly(project(path = ":common", configuration = "namedElements"))
}

tasks {
    register<Zip>("packageTestScripts") {
        archiveFileName.set("tests.zip")
        destinationDirectory.set(layout.buildDirectory.dir("resources/main/data/oc2_gametest/file_systems"))
        from("src/main/scripts")
        exclude("**/__pycache__/**")
    }

    processResources {
        dependsOn("packageTestScripts")
    }
}
