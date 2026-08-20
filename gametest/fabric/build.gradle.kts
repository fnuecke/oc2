dependencies {
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.architectury)
    modImplementation(libs.fabric.energy)

    compileOnly(project(path = ":common", configuration = "namedElements"))
    compileOnly(project(path = ":fabric", configuration = "namedElements"))
}
