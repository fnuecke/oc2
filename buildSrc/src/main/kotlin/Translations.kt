import groovy.json.JsonGenerator
import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import java.io.File

// Translations for the mod are stored in the i18n/ dir, as gettext PO files. Subdirs
// are for different use-cases, e.g. Minecraft language files, in-game manual, ...

private val LINE_PREFIX = Regex("""^\s*(?:(?:#+|[-*]|\d+\.)\s+)?""")
private val CODE_ONLY = Regex("""`[^`]*`""")
private val LINK_TARGET = Regex("""(?<=])\(([^)]+)\)""")
private val PLACEHOLDER = Regex("""(?<=])\(\{(\d+)}\)""")
private val LANGUAGE_CODE = Regex("""[a-z]+_[a-z]+""")
private val JSON = JsonGenerator.Options().disableUnicodeEscaping().build()

abstract class ExtractTranslations : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:OutputFile
    abstract val template: RegularFileProperty

    @TaskAction
    fun extract() {
        writePo(template.get().asFile, modId.get(), entries())
    }

    protected abstract fun entries(): Collection<PoEntry>
}

abstract class GenerateTranslations : DefaultTask() {
    @get:Input
    abstract val modId: Property<String>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val translations: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val output = outputDirectory.get().asFile
        output.deleteRecursively()
        val assets = File(output, "assets/${modId.get()}")
        translations.files.sorted().forEach { po ->
            val language = po.nameWithoutExtension
            if (!LANGUAGE_CODE.matches(language)) {
                throw GradleException("[$po] Name is not a Minecraft language code, e.g. de_de.")
            }
            write(po, language, readPo(po).filter { it.isTranslated }, assets)
        }
    }

    protected abstract fun write(po: File, language: String, translations: List<PoEntry>, assets: File)
}

// Minecraft language files, one entry per key.

abstract class ExtractLangTranslations : ExtractTranslations() {
    override fun entries() = readJson(sources.singleFile).map { (key, text) -> PoEntry(key, text) }
}

abstract class GenerateLangTranslations : GenerateTranslations() {
    override fun write(po: File, language: String, translations: List<PoEntry>, assets: File) {
        val translated = translations.associate { it.context to it.translation }
        val json = readJson(sources.singleFile).keys.filter { it in translated }.associateWith { translated[it] }
        File(assets, "lang/${language}.json").write(JSON.toJson(json))
    }
}

// Manual pages, one entry per line, matching how Markdown Manual renders them.
// Link and image targets are replaced with numbered placeholders, filled in from the English line.

abstract class ExtractManualTranslations : ExtractTranslations() {
    override fun entries(): Collection<PoEntry> {
        val entries = LinkedHashMap<String, PoEntry>()
        manualPages(sources).forEach { (path, file) ->
            manualLines(file).forEach { line ->
                val entry = entries.getOrPut(line.text) { PoEntry(null, line.text) }
                if (path !in entry.locations) entry.locations += path
            }
        }
        return entries.values
    }
}

abstract class GenerateManualTranslations : GenerateTranslations() {
    override fun write(po: File, language: String, translations: List<PoEntry>, assets: File) {
        val translated = translations.associate { it.id to it.translation }
        manualPages(sources).forEach { (path, file) ->
            val content = file.readLines().toMutableList()
            val lines = manualLines(file).filter { it.text in translated }
            if (lines.isEmpty()) return@forEach
            lines.forEach { line ->
                val text = PLACEHOLDER.replace(translated.getValue(line.text)) {
                    val index = it.groupValues[1].toInt()
                    val target = line.targets.getOrNull(index - 1)
                        ?: throw GradleException("[$po] Translation of '${line.text}' uses unknown placeholder {$index}.")
                    "($target)"
                }
                content[line.index] = LINE_PREFIX.find(content[line.index])!!.value + text
            }
            File(assets, "doc/${language}/${path}").write(content.joinToString("\n", postfix = "\n"))
        }
    }
}

fun Project.registerLangTranslationTasks() {
    val modId = property("modId") as String
    registerTranslationTasks<ExtractLangTranslations, GenerateLangTranslations>(
        "lang",
        files("src/main/resources/assets/${modId}/lang/en_us.json"),
    )
}

fun Project.registerManualTranslationTasks() {
    val modId = property("modId") as String
    registerTranslationTasks<ExtractManualTranslations, GenerateManualTranslations>(
        "manual",
        files(listOf("src/main/resources", "src/generated/resources")
            .map { file("${it}/assets/${modId}/doc/en_us") }
            .filter { it.isDirectory }),
    )
}

class PoEntry(val context: String?, val id: String) {
    var translation = ""
    var isFuzzy = false
    var isObsolete = false
    val locations = mutableListOf<String>()

    val isTranslated get() = translation.isNotEmpty() && !isFuzzy && !isObsolete && id.isNotEmpty()
}

private inline fun <reified E : ExtractTranslations, reified G : GenerateTranslations> Project.registerTranslationTasks(
    component: String,
    sources: FileCollection,
) {
    val modId = property("modId") as String
    val directory = rootProject.layout.projectDirectory.dir("i18n/${component}")
    val taskName = component.replaceFirstChar { it.uppercase() }

    val extract = tasks.register<E>("extract${taskName}Translations") {
        group = "translations"
        description = "Writes the translation template i18n/${component}/${component}.pot from the English sources."
        this.modId.set(modId)
        this.sources.setFrom(sources)
        template.set(directory.file("${component}.pot"))
    }

    val generate = tasks.register<G>("generate${taskName}Translations") {
        group = "translations"
        description = "Generates game files from the translations in i18n/${component}/."
        this.modId.set(modId)
        this.sources.setFrom(sources)
        translations.setFrom(directory.asFileTree.matching { include("*.po") })
        outputDirectory.set(layout.buildDirectory.dir("generated/translations/${component}"))
    }

    if ("extractTranslations" !in tasks.names) {
        tasks.register("extractTranslations") {
            group = "translations"
            description = "Writes all translation templates in i18n/ from the English sources."
            dependsOn(tasks.withType<ExtractTranslations>())
        }
    }

    val main = extensions.getByType(SourceSetContainer::class.java).getByName("main")
    main.resources.srcDir(generate)
    tasks.named("processResources") { dependsOn(extract) }
}

private class ManualLine(val index: Int, val text: String, val targets: List<String>)

private fun manualPages(roots: FileCollection): List<Pair<String, File>> =
    roots.files.flatMap { root -> root.walk().filter { it.extension == "md" }.map { it.relativeTo(root).invariantSeparatorsPath to it } }
        .sortedBy { it.first }

private fun manualLines(file: File): List<ManualLine> =
    file.readLines().mapIndexedNotNull { index, line ->
        val text = line.substring(LINE_PREFIX.find(line)!!.range.last + 1).trimEnd()
        if (text.isEmpty() || CODE_ONLY.matches(text)) return@mapIndexedNotNull null
        var counter = 0
        val targets = LINK_TARGET.findAll(text).map { it.groupValues[1] }.toList()
        ManualLine(index, LINK_TARGET.replace(text) { "({${++counter}})" }, targets)
    }

@Suppress("UNCHECKED_CAST")
private fun readJson(file: File): Map<String, String> = JsonSlurper().parseText(file.readText()) as Map<String, String>

private fun File.write(text: String) {
    parentFile.mkdirs()
    writeText(text)
}

// --- PO file format, without plural forms. --- //

private val ESCAPES = mapOf('\\' to '\\', '"' to '"', '\n' to 'n', '\r' to 'r', '\t' to 't')
private val UNESCAPES = ESCAPES.entries.associate { (char, escaped) -> escaped to char }
private val ESCAPE_SEQUENCE = Regex("""\\(.?)""")

private fun writePo(file: File, modId: String, entries: Collection<PoEntry>) {
    val out = StringBuilder()
    out.append("# Translations for ${modId}.\n")
    out.append("msgid \"\"\nmsgstr \"\"\n")
    listOf(
        "Project-Id-Version: $modId",
        "Language: ",
        "MIME-Version: 1.0",
        "Content-Type: text/plain; charset=UTF-8",
        "Content-Transfer-Encoding: 8bit",
    ).forEach { out.append("\"${escape(it)}\\n\"\n") }
    entries.forEach { entry ->
        out.append('\n')
        entry.locations.forEach { out.append("#: $it\n") }
        entry.context?.let { out.append("msgctxt \"${escape(it)}\"\n") }
        out.append("msgid \"${escape(entry.id)}\"\n")
        out.append("msgstr \"\"\n")
    }
    file.write(out.toString())
}

private fun readPo(file: File): List<PoEntry> {
    val entries = mutableListOf<PoEntry>()
    val fields = mutableMapOf<String, StringBuilder>()
    var field: StringBuilder? = null
    var isFuzzy = false
    var isObsolete = false

    fun flush() {
        val id = fields["msgid"] ?: return
        val entry = PoEntry(fields["msgctxt"]?.toString(), id.toString())
        entry.translation = fields["msgstr"]?.toString().orEmpty()
        entry.isFuzzy = isFuzzy
        entry.isObsolete = isObsolete
        entries += entry
        fields.clear()
        field = null
        isFuzzy = false
        isObsolete = false
    }

    file.readLines().forEachIndexed { index, raw ->
        val location = "$file:${index + 1}"
        var line = raw.trim()
        if (line.startsWith("#~|")) return@forEachIndexed
        if (line.startsWith("#~")) {
            isObsolete = true
            line = line.removePrefix("#~").trim()
        }
        when {
            line.isEmpty() -> flush()
            line.startsWith("#,") -> isFuzzy = isFuzzy || line.removePrefix("#,").split(',').any { it.trim() == "fuzzy" }
            line.startsWith("#") -> {}
            line.startsWith("\"") -> field!!.append(unquote(line, location))
            else -> {
                val keyword = line.substringBefore(' ')
                if (keyword !in setOf("msgctxt", "msgid", "msgstr")) {
                    throw GradleException("[$location] Unsupported PO keyword '$keyword'.")
                }
                if (keyword == "msgctxt" || (keyword == "msgid" && "msgstr" in fields)) flush()
                field = StringBuilder(unquote(line.substringAfter(' ').trim(), location))
                fields[keyword] = field!!
            }
        }
    }
    flush()
    return entries
}

private fun escape(text: String): String = buildString {
    text.forEach { char -> ESCAPES[char]?.let { append('\\').append(it) } ?: append(char) }
}

private fun unquote(quoted: String, location: String): String {
    if (quoted.length < 2 || !quoted.startsWith('"') || !quoted.endsWith('"')) {
        throw GradleException("[$location] Malformed PO string.")
    }
    return ESCAPE_SEQUENCE.replace(quoted.substring(1, quoted.length - 1)) {
        UNESCAPES[it.groupValues[1].firstOrNull()]?.toString()
            ?: throw GradleException("[$location] Unsupported escape '${it.value}'.")
    }
}
