repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    neoForge(libs.neoforge.platform)
    modImplementation(libs.neoforge.architectury)

    modImplementation("net.neoforged:testframework:${libs.versions.neoforge.platform.get()}") {
        isTransitive = false
    }

    compileOnly(project(path = ":common", configuration = "namedElements"))
    compileOnly(project(path = ":neoforge", configuration = "namedElements"))
}
