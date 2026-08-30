import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File

private fun Project.localLibDir(propertyName: String): File? {
    val configured = (findProperty(propertyName) as String?)?.takeIf { it.isNotBlank() } ?: return null
    val dir = rootProject.file(configured)
    check(dir.isDirectory) { "[$propertyName] is set to [$configured], which is not a directory." }
    return dir
}

private fun Project.localLibJar(dir: File, pattern: String): File =
    fileTree(dir).matching {
        include(pattern)
        exclude("*-sources.jar", "*-javadoc.jar", "*-jmh.jar")
    }.files.maxByOrNull(File::lastModified)
        ?: error("No jar matching [$pattern] in [$dir]; build that project first.")

fun Project.bundledLibs(): List<Any> {
    val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    fun lib(propertyName: String, pattern: String, alias: String): Any {
        val dir = localLibDir(propertyName)
            ?: return catalog.findLibrary(alias).orElseThrow().get().toString()
        val jar = localLibJar(dir.resolve("build/libs"), pattern)
        logger.lifecycle("Substituting [$alias] from local [$jar]")
        return files(jar)
    }

    return listOf(
        lib("ceresDir", "ceres-*.jar", "ceres"),
        lib("sednaDir", "sedna-*.jar", "sedna"),
        lib("buildrootDir", "sedna-buildroot-*.jar", "sedna-buildroot"),
        lib("sednaCpmDir", "sedna-cpm-*.jar", "sedna-cpm"),
    )
}
