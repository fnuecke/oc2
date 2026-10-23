import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import java.io.File

private val Project.tis3dDir: String
    get() = property("tis3dDir") as String

private val Project.tis3dMinecraftVersion: String
    get() = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")
        .findVersion("minecraft").orElseThrow().requiredVersion

val Project.useLocalTis3d: Boolean
    get() = tis3dDir.isNotBlank() && rootProject.file(tis3dDir).isDirectory

fun Project.tis3dApiJar(): File {
    val pattern = "tis3d-MC$tis3dMinecraftVersion-*-api.jar"
    val dir = rootProject.file("$tis3dDir/build/libs")
    return fileTree(dir).matching { include(pattern) }.files.maxByOrNull(File::lastModified)
        ?: error("No jar matching '$pattern' in $dir; build TIS-3D for that Minecraft version first.")
}

fun Project.tis3dVersion(publishedVersion: String): String =
    if (useLocalTis3d) "0.0.0" else publishedVersion
