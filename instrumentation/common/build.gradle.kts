val enabledPlatforms: String by project

architectury {
    common(enabledPlatforms.split(","))
}

dependencies {
    modImplementation(libs.fabric.loader)
    modApi(libs.architectury.api)
}
