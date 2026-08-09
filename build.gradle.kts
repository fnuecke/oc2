import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.RemapJarTask

plugins {
    java
    alias(libs.plugins.architectury)
    alias(libs.plugins.loom) apply false
    alias(libs.plugins.shadow) apply false
    alias(libs.plugins.spotless)
}

val modId: String by project
val modVersion: String by project
val mavenGroup: String by project
val enabledPlatforms: String by project
val minecraftVersion: String = libs.versions.minecraft.get()

fun getGitRef(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "pmd")
    apply(plugin = rootProject.libs.plugins.architectury.get().pluginId)
    apply(plugin = rootProject.libs.plugins.loom.get().pluginId)

    version = "${modVersion}+${getGitRef()}"
    group = mavenGroup
    base.archivesName.set("${modId}-MC${minecraftVersion}-${project.name}")

    architectury {
        minecraft = minecraftVersion
    }

    configure<LoomGradleExtensionAPI> {
        silentMojangMappingsLicense()
    }

    repositories {
        exclusiveContent {
            forRepository { maven("https://maven.parchmentmc.org") }
            filter { includeGroupByRegex("org\\.parchmentmc.*") }
        }
        exclusiveContent {
            forRepository { maven("https://api.modrinth.com/maven") }
            filter { includeGroup("maven.modrinth") }
        }
        exclusiveContent {
            forRepository { maven("https://maven.blamejared.com") }
            filter { includeGroup("mezz.jei") }
        }
        // Only needed when the ceres / sedna / buildroot sibling checkouts are absent.
        if (project.hasProperty("gpr.user") && project.hasProperty("gpr.key")) {
            for ((repo, group) in listOf(
                "fnuecke/ceres" to "li.cil.ceres",
                "fnuecke/sedna" to "li.cil.sedna",
                "fnuecke/buildroot" to "li.cil.sedna"
            )) {
                maven("https://maven.pkg.github.com/$repo") {
                    credentials {
                        username = project.property("gpr.user") as String
                        password = project.property("gpr.key") as String
                    }
                    content { includeGroup(group) }
                }
            }
        }
    }

    dependencies {
        "minecraft"(rootProject.libs.minecraft)
        val loom = project.extensions.getByName<LoomGradleExtensionAPI>("loom")
        "mappings"(loom.layered {
            officialMojangMappings()
            parchment("org.parchmentmc.data:parchment-${rootProject.libs.versions.parchment.minecraft.get()}:${rootProject.libs.versions.parchment.mappings.get()}@zip")
        })
        "compileOnly"("com.google.code.findbugs:jsr305:3.0.2")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    configure<PmdExtension> {
        toolVersion = "7.26.0"
        ruleSets = emptyList()
        ruleSetFiles = rootProject.files("config/pmd/ruleset.xml")
        isConsoleOutput = true
        isIgnoreFailures = false
    }

    tasks.withType<Pmd>().configureEach {
        // jcodec is vendored third-party source; mixins are not idiomatic Java.
        exclude("**/mixin/**", "**/jcodec/**")
        reports {
            xml.required.set(false)
            html.required.set(true)
        }
    }

    tasks {
        jar {
            from(rootProject.file("LICENSE")) {
                rename { "${it}_${modId}" }
            }
            from(rootProject.file("LICENSE-JCODEC"))
        }

        withType<JavaCompile>().configureEach {
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

    idea {
        module {
            for (exclude in arrayOf("out", "logs", "run")) {
                excludeDirs.add(file(exclude))
            }
        }
    }
}

val projectConfigurations = mapOf(
    "fabric" to "Fabric",
    "neoforge" to "NeoForge"
)

for (platform in enabledPlatforms.split(',')) {
    project(":$platform") {
        apply(plugin = rootProject.libs.plugins.shadow.get().pluginId)

        architectury {
            platformSetupLoomIde()
            loader(platform)
        }

        val common: Configuration by configurations.creating
        val shadowBundle: Configuration by configurations.creating

        configurations {
            common.isCanBeResolved = true
            common.isCanBeConsumed = false

            compileClasspath.get().extendsFrom(common)
            runtimeClasspath.get().extendsFrom(common)
            getByName("development${projectConfigurations[platform]}").extendsFrom(common)

            shadowBundle.isCanBeResolved = true
            shadowBundle.isCanBeConsumed = false
        }

        dependencies {
            common(project(path = ":common", configuration = "namedElements")) { isTransitive = false }
            shadowBundle(
                project(
                    path = ":common",
                    configuration = "transformProduction${projectConfigurations[platform]}"
                )
            ) { isTransitive = false }
        }

        tasks {
            withType<ShadowJar> {
                exclude("architectury.common.json")
                configurations = listOf(shadowBundle)
                archiveClassifier.set("dev-shadow")
            }

            withType<RemapJarTask> {
                val shadowJarTask = getByName<ShadowJar>("shadowJar")
                inputFile.set(shadowJarTask.archiveFile)
                dependsOn(shadowJarTask)
                archiveClassifier.set(null as String?)
            }

            jar {
                archiveClassifier.set("dev")
            }
        }

        (components["java"] as AdhocComponentWithVariants)
            .withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) {
                skip()
            }
    }
}

tasks.register("lint") {
    group = "verification"
    description = "Runs Spotless and PMD across all modules."
    dependsOn("spotlessCheck")
    dependsOn(subprojects.map { "${it.path}:pmdMain" })
}

spotless {
    java {
        target("*/src/*/java/li/cil/**/*.java")
        targetExclude("*/src/*/java/li/cil/oc2/jcodec/**/*.java")

        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
        indentWithSpaces()
        importOrder("", "javax|java", "\\#")
    }
}
