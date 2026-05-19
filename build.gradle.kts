
plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "8.3.6"
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
    compileOnly(libs.spigot.api)
    compileOnly(libs.authlib)
    compileOnly(libs.kotlin.stdlib)
    compileOnly(libs.kotlin.reflect)
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

// === TEST SERVER CONFIGURATION ===
val testServerDir = file("server")

tasks.shadowJar {
    minimize()
    archiveFileName.set("LimeFrameGUI.jar")
}

// The Copy Task
tasks.register<Copy>("copyToServer") {
    group = "LimeFrameGUI" // Groups it neatly in the IntelliJ Gradle panel
    description = "Copies the compiled plugin to the local test server's plugins folder."
    dependsOn("shadowJar")

    // Dynamically grabs the exact output file from shadowJar (inside build/libs/)
    from(tasks.shadowJar.flatMap { it.archiveFile })
    into(testServerDir.resolve("plugins"))
}

// The Run Task
tasks.register<Exec>("runServer") {
    group = "LimeFrameGUI"
    description = "Builds, copies, and starts the test server directly in the IDE."
    dependsOn("copyToServer")

    workingDir = testServerDir

    commandLine("java", "-Xmx2G", "-Xms2G", "-jar", "paper-26.1.2-60.jar", "nogui")
    standardInput = System.`in`
}