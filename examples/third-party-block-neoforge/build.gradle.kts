plugins {
    `java-library`
    id("net.neoforged.moddev") version "2.0.147"
}

version = "1.0.0"
group = "com.example.thirdpartyblock"

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

repositories {
    maven("https://fnuecke.github.io/maven") {
        content {
            includeGroup("li.cil.oc2")
            includeGroup("li.cil.markdown_manual")
        }
    }
    maven("https://maven.architectury.dev") {
        content { includeGroup("dev.architectury") }
    }
}

neoForge {
    version = "21.11.45"

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
            programArgument("--nogui")
        }
    }

    mods {
        register("oc2_example_third_party_block") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    implementation("li.cil.oc2:oc2-1.21.11-neoforge:0.7.1") { isTransitive = false }

    runtimeOnly("dev.architectury:architectury-neoforge:19.0.1")
    runtimeOnly("li.cil.markdown_manual:markdown_manual-1.21.11-neoforge:1.2.7")
}
