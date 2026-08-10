import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File

fun Project.localLibJar(dir: String, pattern: String): File? {
    val directory = rootProject.file(dir)
    if (!directory.isDirectory) return null
    return fileTree(directory).matching {
        include(pattern)
        exclude("*-sources.jar", "*-javadoc.jar", "*-jmh.jar")
    }.files.maxByOrNull(File::lastModified)
}

fun Project.bundledLibs(): List<Any> {
    val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    fun lib(dir: String, pattern: String, alias: String): Any =
        localLibJar(dir, pattern)?.let { files(it) }
            ?: catalog.findLibrary(alias).orElseThrow().get().toString()

    return listOf(
        lib("../ceres/build/libs", "ceres-*.jar", "ceres"),
        lib("../sedna/build/libs", "sedna-*.jar", "sedna"),
        lib("../buildroot/build/libs", "sedna-buildroot-*.jar", "sedna-buildroot"),
    )
}
