val enabledPlatforms: String by project

architectury {
    common(enabledPlatforms.split(","))
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)

    compileOnly(project(path = ":common", configuration = "namedElements"))

    testCompileOnly("com.google.code.findbugs:jsr305:3.0.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
