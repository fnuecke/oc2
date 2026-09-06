import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

private val blockModels = listOf(
    "cable_base",
    "cable_link",
    "cable_plug",
    "cable_straight",
    "cable_support",
    "charger",
    "computer",
    "disk_drive",
    "flash_drive",
    "internet_gateway",
    "keyboard",
    "network_connector",
    "network_hub",
    "projector",
    "redstone_interface",
)

private val renderTypeOverrides = mapOf(
    "cable_link" to "minecraft:solid",
    "cable_plug" to "minecraft:solid",
)

private val loaderOverrides = mapOf(
    "cable_base" to "oc2:bus_cable",
)

abstract class GenerateBlockModels : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val sources: ConfigurableFileCollection

    @get:Classpath
    abstract val converter: ConfigurableFileCollection

    @get:Input
    abstract val modId: Property<String>

    @get:Input
    abstract val renderTypes: MapProperty<String, String>

    @get:Input
    abstract val loaders: MapProperty<String, String>

    @get:Nested
    abstract val launcher: Property<JavaLauncher>

    @get:OutputFiles
    abstract val generatedFiles: ConfigurableFileCollection

    @get:Internal
    abstract val assetsDirectory: DirectoryProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun generate() {
        val assets = assetsDirectory.get().asFile
        sources.files
            .groupBy { renderTypes.get()[it.nameWithoutExtension] }
            .forEach { (renderType, files) -> convert(assets, renderType, files) }

        loaders.get().forEach { (model, loader) ->
            val file = File(assets, "${modId.get()}/models/block/${model}.json")
            val json = file.readText()
            require(json.startsWith("{")) { "[$file] does not hold a JSON object." }
            file.writeText("""{"loader":"${loader}",""" + json.removePrefix("{"))
        }
    }

    private fun convert(assets: File, renderType: String?, sources: List<File>) {
        execOperations.javaexec {
            executable = launcher.get().executablePath.asFile.absolutePath
            classpath = converter
            mainClass.set("li.cil.vox2mc.MainKt")
            args("--quiet", "--modid", modId.get(), "--output", assets.absolutePath)
            if (renderType != null) {
                args("--render-type", renderType)
            }
            args(sources.map { it.absolutePath })
        }
    }
}

fun Project.registerBlockModelsTask() {
    val unknownOverrides = (renderTypeOverrides.keys + loaderOverrides.keys) - blockModels.toSet()
    require(unknownOverrides.isEmpty()) {
        "Block model overrides for models that are not shipped: ${unknownOverrides.joinToString()}."
    }

    val modId = property("modId") as String
    val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
    val converter = configurations.create("vox2mc")
    dependencies.add(converter.name, catalog.findLibrary("vox2mc").orElseThrow().get().toString())

    val sourceDirectory = rootProject.layout.projectDirectory.dir("assets/block")
    val assets = layout.projectDirectory.dir("src/main/resources/assets")
    val toolchains = extensions.getByType<JavaToolchainService>()

    val generateBlockModels = tasks.register<GenerateBlockModels>("generateBlockModels") {
        group = "build"
        description = "Regenerates the block models and their textures from their MagicaVoxel sources."

        this.modId.set(modId)
        sources.setFrom(blockModels.map { sourceDirectory.file("${it}.vox") })
        this.converter.setFrom(converter)
        renderTypes.set(renderTypeOverrides)
        loaders.set(loaderOverrides)
        launcher.set(toolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
        assetsDirectory.set(assets)
        generatedFiles.setFrom(
            blockModels.map { assets.file("${modId}/models/block/${it}.json") } +
                blockModels.map { assets.file("${modId}/textures/block/${it}.png") }
        )
    }

    val main = extensions.getByType<SourceSetContainer>().getByName("main")
    main.resources.setSrcDirs(main.resources.srcDirs.map { files(it).builtBy(generateBlockModels) })
}
