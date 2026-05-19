package net.justlime.limeframegui.registry.component

import org.bukkit.configuration.ConfigurationSection

object LangRegistry : IRegistry {
    private val rawMessages = mutableMapOf<String, MutableMap<String, String>>()
    private val rawLists = mutableMapOf<String, MutableMap<String, List<String>>>()

    private val compiledMessages = mutableMapOf<String, MutableMap<String, String>>()
    private val compiledLists = mutableMapOf<String, MutableMap<String, List<String>>>()

    private val placeholderRegex = Regex("\\{([a-zA-Z0-9_.-]+)\\}")
    val langRegex = Regex("\\{lang[.:]([a-zA-Z0-9_.-]+)(?:\\|([^}]+))?\\}")

    var defaultLocale: String = "en_us"

    /**
     * Retrieves a localized string by key, falling back to the default locale if missing.
     * Injects inline arguments replacing {key} with the corresponding map value.
     */
    fun getString(key: String, locale: String? = null, args: Map<String, String> = emptyMap()): String? {
        val safeLocale = locale?.lowercase() ?: defaultLocale
        var text = compiledMessages[safeLocale]?.get(key) ?: compiledMessages[defaultLocale]?.get(key) ?: return null

        args.forEach { (k, v) ->
            text = text.replace("{$k}", v)
        }
        return text
    }

    /**
     * Retrieves a localized string list by key, falling back to the default locale if missing.
     * Injects inline arguments across every line within the list.
     */
    fun getList(key: String, locale: String? = null, args: Map<String, String> = emptyMap()): List<String> {
        val safeLocale = locale?.lowercase() ?: defaultLocale
        val list = compiledLists[safeLocale]?.get(key) ?: compiledLists[defaultLocale]?.get(key) ?: emptyList()

        if (args.isEmpty()) return list

        return list.map { line ->
            var resolvedLine = line
            args.forEach { (k, v) ->
                resolvedLine = resolvedLine.replace("{$k}", v)
            }
            resolvedLine
        }
    }

    /**
     * Loads configuration into the default locale, clearing existing data.
     */
    override fun load(section: ConfigurationSection) {
        loadLocale(section, defaultLocale, clearNamespace = true)
    }

    /**
     * Appends configuration into the default locale without clearing existing data.
     */
    override fun append(section: ConfigurationSection) {
        loadLocale(section, defaultLocale, clearNamespace = false)
    }

    override fun clear() {
        rawMessages.clear()
        rawLists.clear()
        compiledMessages.clear()
        compiledLists.clear()
    }

    /**
     * Compiles a language file into a specific locale namespace.
     * Processes nested maps and resolves internal placeholders immediately upon load.
     */
    fun loadLocale(section: ConfigurationSection, locale: String, clearNamespace: Boolean = false) {
        val safeLocale = locale.lowercase()

        if (clearNamespace) {
            rawMessages[safeLocale]?.clear()
            rawLists[safeLocale]?.clear()
            compiledMessages[safeLocale]?.clear()
            compiledLists[safeLocale]?.clear()
        }

        rawMessages.putIfAbsent(safeLocale, mutableMapOf())
        rawLists.putIfAbsent(safeLocale, mutableMapOf())
        compiledMessages.putIfAbsent(safeLocale, mutableMapOf())
        compiledLists.putIfAbsent(safeLocale, mutableMapOf())

        flattenLang(section, "", safeLocale)

        val currentRawMessages = rawMessages[safeLocale]!!
        val currentRawLists = rawLists[safeLocale]!!

        for (key in currentRawMessages.keys) {
            // 🌟 FIX 1: Apply global placeholders to compiled lang strings at load-time!
            val internallyResolved = resolveString(currentRawMessages[key]!!, safeLocale, mutableSetOf(key))
            compiledMessages[safeLocale]!![key] = PlaceholderRegistry.resolve(internallyResolved)
        }

        for (key in currentRawLists.keys) {
            // 🌟 FIX 2: Apply global placeholders to compiled lang lists at load-time!
            val internallyResolved = resolveList(currentRawLists[key]!!, safeLocale, mutableSetOf(key))
            compiledLists[safeLocale]!![key] = PlaceholderRegistry.resolve(internallyResolved)
        }
    }

    /**
     * Recursively traverses a YAML configuration to convert nested trees into dot-notation paths.
     */
    private fun flattenLang(section: ConfigurationSection, pathPrefix: String, locale: String) {
        for (key in section.getKeys(false)) {
            val currentPath = if (pathPrefix.isEmpty()) key else "$pathPrefix.$key"

            if (section.isConfigurationSection(key)) {
                flattenLang(section.getConfigurationSection(key)!!, currentPath, locale)
            } else if (section.isList(key)) {
                rawLists[locale]!![currentPath] = section.getStringList(key)
            } else {
                rawMessages[locale]!![currentPath] = section.getString(key) ?: ""
            }
        }
    }

    /**
     * Parses GUI configuration strings dynamically, resolving `{lang:key|args}` syntax
     * into compiled localized text at runtime.
     */
    fun resolveLangString(text: String, locale: String): String {
        // Direct Lang Mapping (e.g. `name: "lang.dialog_title"`)
        if (text.startsWith("lang.") || text.startsWith("lang:")) {
            val stripped = text.substring(5)
            val parts = stripped.split("|")
            val key = parts[0]
            val args = parseArgs(parts.drop(1))

            val result = getString(key, locale, args) ?: text
            return PlaceholderRegistry.resolve(result) // 🌟 FIX 3: Catch direct lang mapping
        }

        // Inline Lang Mapping (e.g. `name: "Welcome to {lang:title} GUI!"`)
        var resolvedText = text
        langRegex.findAll(text).forEach { match ->
            val fullMatch = match.value
            val key = match.groupValues[1]
            val argsRaw = match.groupValues[2]

            val args = if (argsRaw.isNotEmpty()) parseArgs(argsRaw.split("|")) else emptyMap()

            val replacement = LangRegistry.getString(key, locale, args) ?: fullMatch
            resolvedText = resolvedText.replace(fullMatch, replacement)
        }

        // This ensures `name: "{cmd.team} GUI"` gets resolved perfectly.
        return PlaceholderRegistry.resolve(resolvedText)
    }

    /**
     * Converts a raw string list of argument definitions (e.g., ["color=red"]) into a mapped dictionary.
     */
    private fun parseArgs(argStrings: List<String>): Map<String, String> {
        val map = mutableMapOf<String, String>()
        for (arg in argStrings) {
            val split = arg.split("=", limit = 2)
            if (split.size == 2) {
                map[split[0]] = split[1]
            }
        }
        return map
    }

    /**
     * Recursively compiles internal list placeholders during server startup, handling list injection.
     * Includes infinite-loop protection.
     */
    private fun resolveList(list: List<String>, locale: String, visited: MutableSet<String>): List<String> {
        val resolvedList = mutableListOf<String>()
        val localRawLists = rawLists[locale]!!

        for (line in list) {
            val trimmedLine = line.trim()
            val exactMatch = placeholderRegex.matchEntire(trimmedLine)

            if (exactMatch != null && localRawLists.containsKey(exactMatch.groupValues[1])) {
                val key = exactMatch.groupValues[1]

                if (visited.contains(key)) {
                    println("[LimeFrameGUI] Warning: Infinite list loop detected at lang key '$key' in locale '$locale'!")
                    resolvedList.add("<loop_error>")
                    continue
                }

                visited.add(key)
                val expanded = resolveList(localRawLists[key]!!, locale, visited)
                resolvedList.addAll(expanded)
                visited.remove(key)
            } else {
                resolvedList.add(resolveString(line, locale, mutableSetOf()))
            }
        }

        return resolvedList
    }

    /**
     * Recursively compiles internal string placeholders during server startup.
     * Includes infinite-loop protection.
     */
    private fun resolveString(text: String, locale: String, visited: MutableSet<String>): String {
        var resolvedText = text
        val localRawMessages = rawMessages[locale]!!

        placeholderRegex.findAll(text).forEach { match ->
            val fullMatch = match.value
            val key = match.groupValues[1]

            if (localRawMessages.containsKey(key)) {
                if (visited.contains(key)) {
                    println("[LimeFrameGUI] Warning: Infinite string loop detected at lang key '$key' in locale '$locale'!")
                    resolvedText = resolvedText.replace(fullMatch, "<loop_error>")
                    return@forEach
                }

                visited.add(key)
                val rawValue = localRawMessages[key]!!
                val compiledValue = resolveString(rawValue, locale, visited)
                visited.remove(key)

                resolvedText = resolvedText.replace(fullMatch, compiledValue)
            }
        }
        return resolvedText
    }
}