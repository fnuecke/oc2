repositories {
    maven("https://maven.neoforged.net/releases")
}

dependencies {
    neoForge(libs.neoforge.platform)
    modImplementation(libs.neoforge.architectury)

    compileOnly(project(path = ":common", configuration = "namedElements"))
}

tasks.jar {
    val bundle = configurations["bundle"]
    dependsOn(bundle)
    from(bundle.elements.map { files -> files.map { zipTree(it) } }) {
        exclude("architectury.common.json", "META-INF/MANIFEST.MF")
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
