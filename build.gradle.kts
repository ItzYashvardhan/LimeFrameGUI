
plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "8.3.0"
    `maven-publish`
}

group = "net.justlime.limeframegui"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.glaremasters.me/repository/public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.viaversion.com") { name = "iaversion-repo" }
}
val targetJavaVersion = 8

kotlin {
    jvmToolchain(targetJavaVersion)
}

dependencies {
    compileOnly(libs.spigot)
    compileOnly(libs.authlib)
    compileOnly(libs.kotlin)
    compileOnly(libs.kotlin.reflection)
    compileOnly(libs.annotation)
    compileOnly(libs.adventure.text.minimessage)
    compileOnly(libs.adventure.text.serializer.legacy)
    compileOnly(libs.adventure.text.serializer.plain)
    compileOnly(libs.adventure.text.serializer.gson)
    compileOnly(libs.papi)
    compileOnly(libs.viaversion)
    compileOnly(libs.bstats)
    compileOnly(libs.folialib)
    compileOnly(libs.anvilgui)

}


tasks.shadowJar{
    minimize()
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group.toString()
            artifactId = "LimeFrameGUI"
            version = project.version.toString()
        }

    }

}

// === SHADOW COPY TASK ===
tasks.register<Copy>("shadowJarCopy") {
    group = "build"
    description = "Copy shadowJar jar to local test server"
    dependsOn("shadowJar")
    from(tasks.shadowJar.get().outputs.files.singleFile)
    into("E:/Minecraft/servers/Development/PaperMC-1.21.10/plugins")
}





