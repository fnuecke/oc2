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

subprojects {
    apply(plugin = "java")
    apply(plugin = "pmd")
    apply(plugin = rootProject.libs.plugins.architectury.get().pluginId)
    apply(plugin = rootProject.libs.plugins.loom.get().pluginId)

    version = "${modVersion}+${gitRef()}"
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
        exclusiveContent {
            forRepository { maven("https://fnuecke.github.io/maven") }
            filter { includeModule("li.cil.sedna", "sedna-buildroot") }
        }
        mavenCentral()
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

    configureJava()

    configurePmd()

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

                from(rootProject.file("LICENSE")) {
                    rename { "${it}_${modId}" }
                }
                from(rootProject.file("LICENSE-JCODEC"))
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

for (extraModule in listOf("instrumentation", "gametest")) {
    for (platform in enabledPlatforms.split(',')) {
        project(":$extraModule-$platform") {
            architectury {
                platformSetupLoomIde()
                loader(platform)
            }

            val common: Configuration by configurations.creating
            val bundle: Configuration by configurations.creating

            configurations {
                common.isCanBeResolved = true
                common.isCanBeConsumed = false

                compileClasspath.get().extendsFrom(common)
                runtimeClasspath.get().extendsFrom(common)
                getByName("development${projectConfigurations[platform]}").extendsFrom(common)

                bundle.isCanBeResolved = true
                bundle.isCanBeConsumed = false
            }

            dependencies {
                common(project(path = ":$extraModule-common", configuration = "namedElements")) { isTransitive = false }
                bundle(
                    project(
                        path = ":$extraModule-common",
                        configuration = "transformProduction${projectConfigurations[platform]}"
                    )
                ) { isTransitive = false }
            }

            tasks.jar {
                val bundleFiles = configurations["bundle"]
                dependsOn(bundleFiles)
                from(bundleFiles.elements.map { files -> files.map { zipTree(it) } }) {
                    exclude("architectury.common.json", "META-INF/MANIFEST.MF")
                }
                duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            }
        }
    }
}

tasks.named("build") {
    dependsOn("apiJar")
}

spotless {
    java {
        target("**/src/*/java/li/cil/**/*.java")
        targetExclude("**/src/*/java/li/cil/oc2/jcodec/**/*.java")

        endWithNewline()
        trimTrailingWhitespace()
        removeUnusedImports()
        indentWithSpaces()
        importOrder("", "javax|java", "\\#")
    }
}

registerGameTestTask()
registerLintTask()
registerApiJarTask(minecraftVersion)
