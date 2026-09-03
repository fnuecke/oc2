import org.gradle.api.GradleException
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
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

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "utf-8"
        options.release.set(21)
        options.compilerArgs.addAll(
            listOf(
                "-Xlint:all,-processing,-serial,-classfile,-this-escape",
                "-Xmaxwarns", "1000",
            )
        )
    }
}

fun Project.configurePmd(vararg additionalExcludes: String) {
    extensions.configure<PmdExtension> {
        toolVersion = "7.26.0"
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
        isConsoleOutput = true
        isIgnoreFailures = false
    }

    tasks.withType<Pmd>().configureEach {
        exclude("**/mixin/**", *additionalExcludes)
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
    }
}

fun Project.embedLicenses(vararg additionalLicenses: String) {
    val modId = property("modId") as String

    tasks.withType<Jar>().matching { it.name == "jar" || it.name == "shadowJar" }.configureEach {
        from(rootProject.file("LICENSE")) {
            rename { "${it}_${modId}" }
        }
        for (license in additionalLicenses) {
            from(rootProject.file(license))
        }
    }
}

fun Project.configureIdeaExcludes() {
    extensions.configure<IdeaModel> {
        module {
            for (exclude in arrayOf("out", "logs", "run")) {
                excludeDirs.add(file(exclude))
            }
        }
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

abstract class ArchitecturyTransformLock : BuildService<BuildServiceParameters.None>

fun Project.serializeArchitecturyTransforms() {
    val lock = gradle.sharedServices.registerIfAbsent(
        "architecturyTransformLock",
        ArchitecturyTransformLock::class
    ) { maxParallelUsages.set(1) }

    subprojects {
        tasks.matching { it.name.startsWith("transformProduction") }.configureEach { usesService(lock) }
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

fun Project.registerApiJarTask(minecraftVersion: String, apiPackagePath: String? = null) {
    val modId = property("modId") as String
    val modVersion = property("modVersion") as String
    val enabledPlatforms = property("enabledPlatforms") as String
    val modules = listOf("common") + enabledPlatforms.split(',')
    val includePattern = "${apiPackagePath ?: "li/cil/${modId}/api"}/**"

    tasks.register<Jar>("apiJar") {
        group = "build"
        description = "Assembles a jar of the public API classes of every module."
        archiveBaseName.set("${modId}-MC${minecraftVersion}")
        archiveVersion.set("${modVersion}+${gitRef()}")
        archiveClassifier.set("api")

        for (name in modules) {
            val module = project(":$name")
            dependsOn("${module.path}:classes")
            from(Callable { module.the<SourceSetContainer>()["main"].output })
        }

        include(includePattern)
    }

    tasks.register<Jar>("apiSourcesJar") {
        group = "build"
        description = "Assembles a jar of the public API sources of every module."
        archiveBaseName.set("${modId}-MC${minecraftVersion}")
        archiveVersion.set("${modVersion}+${gitRef()}")
        archiveClassifier.set("api-sources")

        for (name in modules) {
            val module = project(":$name")
            from(Callable { module.the<SourceSetContainer>()["main"].allSource })
        }

        include(includePattern)
    }
}

fun Project.configureMavenPublishing(minecraftVersion: String, projectUrl: String) {
    val modId = property("modId") as String
    val modVersion = property("modVersion") as String
    val mavenGroup = property("mavenGroup") as String
    val enabledPlatforms = property("enabledPlatforms") as String
    val mavenRepoDir = findProperty("mavenRepoDir")?.toString()

    val addStaticMavenRepo: PublishingExtension.() -> Unit = {
        if (!mavenRepoDir.isNullOrBlank()) {
            repositories.maven {
                name = "StaticMavenRepo"
                url = rootProject.file(mavenRepoDir).toURI()
            }
        }
    }

    val configurePom: MavenPublication.() -> Unit = {
        pom {
            name.set(modId)
            url.set(projectUrl)
            licenses {
                license {
                    name.set("MIT")
                    url.set("https://opensource.org/license/mit/")
                }
            }
            scm {
                url.set(projectUrl)
            }
        }
    }

    for (platform in enabledPlatforms.split(',')) {
        project(":$platform") {
            apply(plugin = "maven-publish")

            extensions.configure<JavaPluginExtension> {
                withSourcesJar()
            }

            tasks.named<Jar>("sourcesJar") {
                from(Callable { project(":common").the<SourceSetContainer>()["main"].allSource })
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }

            extensions.configure<PublishingExtension> {
                publications.create<MavenPublication>("mod") {
                    groupId = mavenGroup
                    artifactId = "${modId}-${minecraftVersion}-${project.name}"
                    version = modVersion
                    artifact(tasks.named("remapJar"))
                    artifact(tasks.named("remapSourcesJar"))
                    configurePom()
                }
                addStaticMavenRepo()
            }
        }
    }

    if ("apiJar" in tasks.names) {
        apply(plugin = "maven-publish")
        extensions.configure<PublishingExtension> {
            publications.create<MavenPublication>("api") {
                groupId = mavenGroup
                artifactId = "${modId}-${minecraftVersion}-api"
                version = modVersion
                artifact(tasks.named("apiJar")) { classifier = null }
                artifact(tasks.named("apiSourcesJar")) { classifier = "sources" }
                configurePom()
            }
            addStaticMavenRepo()
        }
    }
}
