import org.gradle.api.Project
import java.io.File

private val Project.markdownManualDir: String
    get() = property("markdownManualDir") as String

val Project.useLocalMarkdownManual: Boolean
    get() = markdownManualDir.isNotBlank() && rootProject.file(markdownManualDir).isDirectory

fun Project.markdownManualJar(module: String, pattern: String): File {
    val dir = rootProject.file("$markdownManualDir/$module/build/libs")
    return fileTree(dir).matching {
        include(pattern)
        exclude("*-dev-shadow.jar", "*-sources.jar")
    }.files.maxByOrNull(File::lastModified)
        ?: error("No jar matching '$pattern' in $dir; build MarkdownManual first.")
}

fun Project.markdownManualVersion(publishedVersion: String): String =
    if (useLocalMarkdownManual) "0.0.0" else publishedVersion
