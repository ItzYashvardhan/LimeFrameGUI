package net.justlime.limeframegui.util

import org.bukkit.configuration.InvalidConfigurationException
import org.yaml.snakeyaml.error.MarkedYAMLException
import java.io.File
import java.util.logging.Logger
import kotlin.math.max
import kotlin.math.min

object ConfigErrorHandler {

    fun printFriendlyError(logger: Logger, file: File, exception: Exception) {
        val cause = if (exception is InvalidConfigurationException) exception.cause else exception

        if (cause is MarkedYAMLException) {
            // FIX: Prioritize contextMark (where the typo started) over problemMark (where it crashed)
            val mark = cause.contextMark ?: cause.problemMark

            if (mark != null) {
                val lineNumber = mark.line
                val column = mark.column

                logger.severe("--------------------------------------------------")
                logger.severe("[LimeFrameGUI] YAML Syntax Error in: ${file.name}")

                // Print both the context and the problem description if available
                val reason = if (cause.context != null) "${cause.context} -> ${cause.problem}" else cause.problem
                logger.severe("Reason: $reason (Line ${lineNumber + 1}, Column ${column + 1})")
                logger.severe("")

                try {
                    val lines = file.readLines()
                    // Expand the view slightly so we can see what's below it
                    val start = max(0, lineNumber - 2)
                    val end = min(lines.size - 1, lineNumber + 3)

                    for (i in start..end) {
                        val prefix = if (i == lineNumber) "  > " else "    "
                        logger.severe("$prefix${i + 1} | ${lines[i]}")

                        if (i == lineNumber) {
                            val pointer = " ".repeat(column) + "^"
                            logger.severe("      $pointer")
                        }
                    }
                } catch (ioe: Exception) {
                    logger.severe("  [Could not read file to display context]")
                }
                logger.severe("--------------------------------------------------")
            } else {
                logger.severe("[LimeFrameGUI] YAML Error in ${file.name}: ${cause.message}")
            }
        } else {
            logger.severe("[LimeFrameGUI] Internal Error while compiling ${file.name}: ${exception.message}")
        }
    }
}