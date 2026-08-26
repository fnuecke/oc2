import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File

private val Project.markdownManualDir: String
    get() = property("markdownManualDir") as String

private val Project.markdownManualMinecraftVersion: String
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        .findVersion("minecraft").orElseThrow().requiredVersion

val Project.useLocalMarkdownManual: Boolean
    get() = markdownManualDir.isNotBlank() && rootProject.file(markdownManualDir).isDirectory

fun Project.markdownManualJar(module: String, suffix: String): File {
    val pattern = "markdown_manual-MC$markdownManualMinecraftVersion-$suffix"
    val dir = rootProject.file("$markdownManualDir/$module/build/libs")
    return fileTree(dir).matching {
        include(pattern)
        exclude("*-dev-shadow.jar", "*-sources.jar")
    }.files.maxByOrNull(File::lastModified)
        ?: error("No jar matching '$pattern' in $dir; build MarkdownManual for that Minecraft version first.")
}

fun Project.markdownManualVersion(publishedVersion: String): String =
    if (useLocalMarkdownManual) "0.0.0" else publishedVersion
