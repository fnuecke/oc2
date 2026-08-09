import org.gradle.api.Project
import java.io.File

fun Project.localLibJar(dir: String, pattern: String): File? {
    val directory = rootProject.file(dir)
    if (!directory.isDirectory) return null
    return fileTree(directory).matching {
        include(pattern)
        exclude("*-sources.jar", "*-javadoc.jar", "*-jmh.jar")
    }.files.maxByOrNull(File::lastModified)
}
