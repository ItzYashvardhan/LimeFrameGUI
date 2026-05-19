package net.justlime.limeframegui.util

import net.justlime.limeframegui.api.LimeFrameAPI
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.net.URLDecoder
import java.util.jar.JarFile

/**
 * Cracks open the plugin JAR and extracts all files within a specific folder.
 */
fun extractDefaultsFromJar(plugin: JavaPlugin, internalPath: String, replace: Boolean =  false) {
    // 1. Get the physical path to the compiled .jar file
    val sourcePath = plugin.javaClass.protectionDomain.codeSource.location.path
    val decodedPath = URLDecoder.decode(sourcePath, "UTF-8")
    
    val jarFile = JarFile(decodedPath)
    
    // 2. Loop through every single file compiled inside the JAR
    val entries = jarFile.entries()
    while (entries.hasMoreElements()) {
        val entry = entries.nextElement()
        val entryName = entry.name

        // 3. If the internal file is inside our target folder (e.g., "gui/")
        if (entryName.startsWith("$internalPath/") && !entry.isDirectory) {
            val targetFile = File(plugin.dataFolder, entryName)
            
            // 4. Extract it ONLY if it doesn't already exist on the server
            if (replace || !targetFile.exists()) {
                targetFile.parentFile?.mkdirs() // Ensure sub-folders exist
                
                plugin.getResource(entryName)?.use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                if (LimeFrameAPI.debugging) plugin.logger.info("[LimeFrameGUI] Extracted default file: $entryName")
            }
        }
    }
    jarFile.close()
}