package com.example.memscan.engine

import android.content.Context
import com.example.memscan.backend.RootBackend
import com.example.memscan.model.AppDataFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class AppDataFileManager(private val context: Context) {

    // Simulated cache store for mock mode
    private val mockFiles = mutableMapOf<String, String>(
        "/data/data/com.google.android.apps.youtube.creator/shared_prefs/youtube_creator_analytics.xml" to
            """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <int name="cached_total_views" value="12450" />
    <string name="cached_views_display">12.5K</string>
    <int name="cached_subscribers" value="427" />
    <string name="cached_revenue_display">$31.42</string>
    <long name="last_analytics_sync_timestamp" value="1725684000000" />
    <boolean name="analytics_offline_mode" value="true" />
</map>""".trimIndent(),

        "/data/data/com.google.android.apps.youtube.creator/shared_prefs/studio_account_config.xml" to
            """<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="active_channel_id">UC_creator_channel_101</string>
    <string name="active_channel_title">Tech Insights Studio</string>
    <boolean name="cache_valid" value="true" />
</map>""".trimIndent(),

        "/data/data/com.google.android.apps.youtube.creator/cache/analytics_summary.json" to
            """{
  "channelId": "UC_creator_channel_101",
  "views28d": 12450,
  "subscribers28d": 427,
  "watchTimeHours": 183.7,
  "estimatedRevenueUsd": 31.42,
  "isLiveCached": true
}""".trimIndent()
    )

    suspend fun listFiles(packageName: String, subPath: String = ""): List<AppDataFile> = withContext(Dispatchers.IO) {
        val basePath = "/data/data/$packageName"
        val fullPath = if (subPath.isEmpty()) basePath else "$basePath/$subPath"

        // Check if root is available and path exists
        if (RootBackend.checkSuBinary()) {
            val cmd = "ls -la \"$fullPath\""
            val output = RootBackend.executePrivilegedCommand(cmd)
            if (output.isSuccess && output.getOrNull()?.isNotBlank() == true) {
                val lines = output.getOrNull()!!.lines()
                val list = mutableListOf<AppDataFile>()
                for (line in lines) {
                    val parts = line.trim().split("\\s+".toRegex())
                    if (parts.size >= 8) {
                        val perms = parts[0]
                        val size = parts[4].toLongOrNull() ?: 0L
                        val name = parts.drop(7).joinToString(" ")
                        if (name == "." || name == "..") continue
                        list.add(
                            AppDataFile(
                                path = "$fullPath/$name",
                                name = name,
                                isDirectory = perms.startsWith("d"),
                                sizeBytes = size,
                                lastModified = System.currentTimeMillis(),
                                permissions = perms
                            )
                        )
                    }
                }
                if (list.isNotEmpty()) return@withContext list
            }
        }

        // Fallback simulated list for testing
        val mockResults = mutableListOf<AppDataFile>()
        if (subPath.isEmpty()) {
            mockResults.add(AppDataFile("$basePath/shared_prefs", "shared_prefs", true, 4096, System.currentTimeMillis(), "drwx------"))
            mockResults.add(AppDataFile("$basePath/cache", "cache", true, 4096, System.currentTimeMillis(), "drwx------"))
            mockResults.add(AppDataFile("$basePath/databases", "databases", true, 4096, System.currentTimeMillis(), "drwx------"))
            mockResults.add(AppDataFile("$basePath/files", "files", true, 4096, System.currentTimeMillis(), "drwx------"))
        } else if (subPath.contains("shared_prefs")) {
            mockResults.add(AppDataFile("$basePath/shared_prefs/youtube_creator_analytics.xml", "youtube_creator_analytics.xml", false, 480, System.currentTimeMillis(), "-rw-rw----"))
            mockResults.add(AppDataFile("$basePath/shared_prefs/studio_account_config.xml", "studio_account_config.xml", false, 320, System.currentTimeMillis(), "-rw-rw----"))
        } else if (subPath.contains("cache")) {
            mockResults.add(AppDataFile("$basePath/cache/analytics_summary.json", "analytics_summary.json", false, 240, System.currentTimeMillis(), "-rw-rw----"))
        }

        mockResults
    }

    suspend fun readFileText(filePath: String): String? = withContext(Dispatchers.IO) {
        if (RootBackend.checkSuBinary()) {
            val cmd = "cat \"$filePath\""
            val output = RootBackend.executePrivilegedCommand(cmd)
            if (output.isSuccess) {
                return@withContext output.getOrNull()
            }
        }
        mockFiles[filePath]
    }

    suspend fun writeFileText(filePath: String, newContent: String): Boolean = withContext(Dispatchers.IO) {
        if (RootBackend.checkSuBinary()) {
            // Write to private app cache first, then copy via su
            val temp = File(context.cacheDir, "studio_edit.tmp")
            try {
                temp.writeText(newContent)
                val cmd = "cat \"${temp.absolutePath}\" > \"$filePath\" && chmod 660 \"$filePath\""
                val result = RootBackend.executePrivilegedCommand(cmd)
                return@withContext result.isSuccess
            } catch (e: Exception) {
                return@withContext false
            } finally {
                temp.delete()
            }
        }

        mockFiles[filePath] = newContent
        true
    }

    suspend fun searchFiles(packageName: String, query: String): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val basePath = "/data/data/$packageName"
        val results = mutableListOf<Pair<String, String>>()

        if (RootBackend.checkSuBinary()) {
            val cmd = "grep -rn \"$query\" \"$basePath\" 2>/dev/null | head -n 25"
            val out = RootBackend.executePrivilegedCommand(cmd)
            if (out.isSuccess && out.getOrNull()?.isNotBlank() == true) {
                for (line in out.getOrNull()!!.lines()) {
                    val split = line.split(":", limit = 3)
                    if (split.size >= 3) {
                        results.add(Pair(split[0], split[2].trim()))
                    }
                }
                if (results.isNotEmpty()) return@withContext results
            }
        }

        // Mock search
        for ((path, content) in mockFiles) {
            if (path.startsWith(basePath) && content.contains(query, ignoreCase = true)) {
                for (line in content.lines()) {
                    if (line.contains(query, ignoreCase = true)) {
                        results.add(Pair(path, line.trim()))
                    }
                }
            }
        }

        results
    }

    suspend fun forceStopApp(packageName: String): Boolean = withContext(Dispatchers.IO) {
        if (RootBackend.checkSuBinary()) {
            val res = RootBackend.executePrivilegedCommand("am force-stop \"$packageName\"")
            return@withContext res.isSuccess
        }
        true
    }
}
