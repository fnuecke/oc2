import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension

// Sibling checkouts are wired in as included builds by settings.gradle.kts.
fun Project.bundledLibs(): List<String> {
    val catalog = extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

    fun lib(alias: String): String = catalog.findLibrary(alias).orElseThrow().get().toString()

    return listOf(
        lib("ceres"),
        lib("sedna"),
        lib("sedna-buildroot"),
        lib("sedna-cpm"),
    )
}
