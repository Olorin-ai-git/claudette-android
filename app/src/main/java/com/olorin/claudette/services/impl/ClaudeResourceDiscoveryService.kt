package com.olorin.claudette.services.impl

import com.olorin.claudette.models.ClaudeResource
import com.olorin.claudette.models.ClaudeResourceType
import com.olorin.claudette.models.RemoteFileEntry
import com.olorin.claudette.services.interfaces.ClaudeResourceDiscoveryServiceInterface
import com.olorin.claudette.services.interfaces.RemoteFileBrowserServiceInterface
import timber.log.Timber

class ClaudeResourceDiscoveryService(
    private val fileBrowserService: RemoteFileBrowserServiceInterface
) : ClaudeResourceDiscoveryServiceInterface {

    override suspend fun discover(projectPath: String, username: String): List<ClaudeResource> {
        val discovered = mutableListOf<ClaudeResource>()

        val homeDir = try {
            fileBrowserService.getHomeDirectory(username)
        } catch (e: Exception) {
            Timber.d("Could not resolve home directory, using default: %s", e.message)
            "/home/$username"
        }

        // Commands: flat .md files + subdirectories with colon notation
        val commandPaths = listOf(
            "$projectPath/.claude/commands",
            "$homeDir/.claude/commands"
        )
        for (dirPath in commandPaths) {
            val files = collectCommandFiles(dirPath, dirPath)
            Timber.i("Found %d commands in %s", files.size, dirPath)

            for ((entry, qualifiedName) in files) {
                if (discovered.any { it.filePath == entry.path }) continue

                var resource = parseResource(entry, qualifiedName, ClaudeResourceType.COMMAND)
                if (resource.isUserInvocable) {
                    resource = ClaudeResource(
                        id = resource.id,
                        name = resource.name,
                        type = ClaudeResourceType.SKILL,
                        description = resource.description,
                        isUserInvocable = true,
                        filePath = resource.filePath
                    )
                }
                discovered.add(resource)
            }
        }

        // Skills: each subdirectory is a skill, SKILL.md is the entry point
        val skillPaths = listOf(
            "$projectPath/.claude/skills",
            "$homeDir/.claude/skills"
        )
        for (dirPath in skillPaths) {
            val skills = collectBundledResources(dirPath, ClaudeResourceType.SKILL)
            Timber.i("Found %d skills in %s", skills.size, dirPath)
            for (resource in skills) {
                if (discovered.none { it.filePath == resource.filePath }) {
                    discovered.add(resource)
                }
            }
        }

        // Agents: same bundle layout as skills
        val agentPaths = listOf(
            "$projectPath/.claude/agents",
            "$homeDir/.claude/agents"
        )
        for (dirPath in agentPaths) {
            val agents = collectBundledResources(dirPath, ClaudeResourceType.AGENT)
            Timber.i("Found %d agents in %s", agents.size, dirPath)
            for (resource in agents) {
                if (discovered.none { it.filePath == resource.filePath }) {
                    discovered.add(resource)
                }
            }
        }

        Timber.i("Discovered %d Claude resources total", discovered.size)
        return discovered
    }

    /**
     * Recursively collects .md files under a commands directory.
     * Subdirectories produce colon-separated names (e.g. tools/fix.md -> tools:fix.md).
     */
    private suspend fun collectCommandFiles(
        dirPath: String,
        rootPath: String
    ): List<Pair<RemoteFileEntry, String>> {
        val results = mutableListOf<Pair<RemoteFileEntry, String>>()

        val entries = try {
            fileBrowserService.listDirectory(dirPath)
        } catch (e: Exception) {
            return results
        }

        for (entry in entries) {
            if (entry.isDirectory) {
                val nested = collectCommandFiles(entry.path, rootPath)
                results.addAll(nested)
            } else if (entry.name.endsWith(".md")) {
                val relative = entry.path.removePrefix(rootPath).removePrefix("/")
                val qualifiedName = relative.replace("/", ":")
                results.add(entry to qualifiedName)
            }
        }

        return results
    }

    /**
     * Discovers bundled resources where each immediate subdirectory is a resource.
     * Also picks up bare .md files directly in the root.
     */
    private suspend fun collectBundledResources(
        dirPath: String,
        type: ClaudeResourceType
    ): List<ClaudeResource> {
        val results = mutableListOf<ClaudeResource>()

        val entries = try {
            fileBrowserService.listDirectory(dirPath)
        } catch (e: Exception) {
            return results
        }

        for (entry in entries) {
            if (entry.isDirectory) {
                val resource = discoverBundleEntryPoint(entry.name, entry.path, type)
                if (resource != null) {
                    results.add(resource)
                }
            } else if (entry.name.endsWith(".md")) {
                val resource = parseResource(entry, entry.name, type)
                results.add(resource)
            }
        }

        return results
    }

    /**
     * Given a skill/agent bundle directory, finds the entry point .md file
     * and returns a ClaudeResource named after the directory.
     */
    private suspend fun discoverBundleEntryPoint(
        dirName: String,
        dirPath: String,
        type: ClaudeResourceType
    ): ClaudeResource? {
        val entries = try {
            fileBrowserService.listDirectory(dirPath)
        } catch (e: Exception) {
            return null
        }

        val mdFiles = entries.filter { !it.isDirectory && it.name.endsWith(".md") }
        val entryPoint = mdFiles.firstOrNull { it.name.uppercase() == "SKILL.MD" }
            ?: mdFiles.firstOrNull()
            ?: return null

        return parseResource(entryPoint, "$dirName.md", type)
    }

    private suspend fun parseResource(
        entry: RemoteFileEntry,
        qualifiedName: String,
        type: ClaudeResourceType
    ): ClaudeResource {
        var description: String? = null
        var isUserInvocable = false

        try {
            val data = fileBrowserService.readFile(entry.path)
            val content = data.toString(Charsets.UTF_8)
            val parsed = parseFrontmatter(content)
            description = parsed.first
            isUserInvocable = parsed.second
        } catch (e: Exception) {
            Timber.d("Could not read resource file: %s", entry.path)
        }

        return ClaudeResource(
            id = entry.path,
            name = qualifiedName,
            type = type,
            description = description,
            isUserInvocable = isUserInvocable,
            filePath = entry.path
        )
    }

    private fun parseFrontmatter(content: String): Pair<String?, Boolean> {
        if (!content.startsWith("---")) {
            return null to false
        }

        val lines = content.split("\n")
        var inFrontmatter = false
        var description: String? = null
        var isUserInvocable = false

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed == "---") {
                if (inFrontmatter) break
                inFrontmatter = true
                continue
            }

            if (inFrontmatter) {
                if (trimmed.startsWith("description:")) {
                    description = trimmed
                        .removePrefix("description:")
                        .trim()
                        .removeSurrounding("\"")
                        .removeSurrounding("'")
                }
                if (trimmed.contains("user-invocable") && trimmed.contains("true")) {
                    isUserInvocable = true
                }
            }
        }

        return description to isUserInvocable
    }
}
