pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "oc2_example_block_entity"

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("li.cil.oc2:oc2-1.21.1-fabric")).using(project(":fabric"))
    }
}
