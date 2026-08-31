pluginManagement {
    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.architectury.dev") }
            filter {
                includeGroup("architectury-plugin")
                includeGroupByRegex("dev\\.architectury.*")
                includeGroup("com.mojang")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.fabricmc.net") }
            filter {
                includeGroupByRegex("net\\.fabricmc.*")
                includeGroup("fabric-loom")
            }
        }
        exclusiveContent {
            forRepository { maven("https://maven.minecraftforge.net") }
            filter {
                includeGroupByRegex("net\\.minecraftforge.*")
                includeGroup("de.oceanlabs.mcp")
            }
        }
        gradlePluginPortal()
    }
}

fun substituteLocal(propertyName: String, libraryName: String) {
    val configured = providers.gradleProperty(propertyName).orNull?.takeIf { it.isNotBlank() } ?: return
    val path = rootDir.resolve(configured)
    require(path.isDirectory) { "[$propertyName] is set to [$configured], which is not a directory." }
    println("Substituting [$libraryName] from local [${path.canonicalPath}]")
    includeBuild(path) {
        dependencySubstitution {
            substitute(module(libraryName)).using(project(":"))
        }
    }
}

substituteLocal("ceresDir", "li.cil.ceres:ceres")
substituteLocal("sednaDir", "li.cil.sedna:sedna")
substituteLocal("buildrootDir", "li.cil.sedna:sedna-buildroot")
substituteLocal("sednaCpmDir", "li.cil.sedna:sedna-cpm")
substituteLocal("vox2mcDir", "li.cil.vox2mc:vox2mc")

include("common")

val enabledPlatforms: String by settings
for (enabledPlatform in enabledPlatforms.split(",")) {
    include(enabledPlatform)
}

for (module in listOf("common") + enabledPlatforms.split(",")) {
    include("instrumentation-$module")
    project(":instrumentation-$module").projectDir = file("instrumentation/$module")

    include("gametest-$module")
    project(":gametest-$module").projectDir = file("gametest/$module")
}

val modId: String by settings
rootProject.name = modId
