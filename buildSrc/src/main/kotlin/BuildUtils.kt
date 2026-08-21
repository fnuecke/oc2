import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the
import java.util.concurrent.Callable

fun Project.gitRef(): String =
    providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()

fun Project.configureJava() {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

fun Project.configurePmd() {
    extensions.configure<PmdExtension> {
        toolVersion = "7.26.0"
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
        isConsoleOutput = true
        isIgnoreFailures = false
    }
}

fun Project.registerLintTask() {
    tasks.register("lint") {
        group = "verification"
        description = "Runs Spotless and PMD across all modules."
        dependsOn("spotlessCheck")
        dependsOn(subprojects.map { "${it.path}:pmdMain" })
    }
}

fun Project.registerGameTestTask() {
    val enabledPlatforms = property("enabledPlatforms") as String
    tasks.register("gameTest") {
        group = "verification"
        description = "Runs the game tests on all enabled platforms."
        dependsOn(enabledPlatforms.split(',').map { platform ->
            when (platform) {
                "fabric" -> ":fabric:runGameTest"
                "neoforge" -> ":neoforge:runGameTestServer"
                else -> throw GradleException("No game test run configured for platform '${platform}'.")
            }
        })
    }
}

fun Project.registerApiJarTask(minecraftVersion: String) {
    val modId = property("modId") as String
    val modVersion = property("modVersion") as String
    val enabledPlatforms = property("enabledPlatforms") as String

    tasks.register<Jar>("apiJar") {
        group = "build"
        description = "Assembles a jar of the public API of every module."
        archiveBaseName.set("${modId}-MC${minecraftVersion}")
        archiveVersion.set("${modVersion}+${gitRef()}")
        archiveClassifier.set("api")

        for (name in listOf("common") + enabledPlatforms.split(',')) {
            val module = project(":$name")
            dependsOn("${module.path}:classes")
            from(Callable { module.the<SourceSetContainer>()["main"].allSource })
            from(Callable { module.the<SourceSetContainer>()["main"].output })
        }

        include("li/cil/${modId}/api/**")
    }
}
