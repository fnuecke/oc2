repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    neoForge(libs.neoforge.platform)
    modImplementation(libs.neoforge.architectury)

    compileOnly(project(path = ":common", configuration = "namedElements"))
}
