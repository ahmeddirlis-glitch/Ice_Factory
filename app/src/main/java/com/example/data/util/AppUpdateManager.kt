package com.example.data.util

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.FileProvider
import com.example.data.model.AppReleaseConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

sealed interface AppUpdateDownloadState {
    data object Idle : AppUpdateDownloadState
    data class Downloading(val progressPercent: Int) : AppUpdateDownloadState
    data class ReadyToInstall(val apkFile: File) : AppUpdateDownloadState
    data object NeedsInstallPermission : AppUpdateDownloadState
    data class Failed(val message: String) : AppUpdateDownloadState
}

object AppUpdateManager {

    const val GITHUB_OWNER = "ahmeddirlis-glitch"
    const val GITHUB_REPO = "Ice_Factory"
    const val GITHUB_LATEST_VERSION_JSON_URL =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest/download/version.json"
    const val GITHUB_LATEST_RELEASE_API_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
    const val DEFAULT_APK_DOWNLOAD_URL =
        "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest/download/brohi-ice-factory-latest.apk"

    fun isValidApkUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("https://", ignoreCase = true) && trimmed.length > 8
    }

    fun extractBuildCodeFromTag(tagName: String, body: String = ""): Int {
        val tag = tagName.trim()
        // Supports tags like v1.1.5-b7, v1.1.5+7, v1.1.5b7
        Regex("""(?:-b|\+|b)(\d+)$""", RegexOption.IGNORE_CASE).find(tag)?.let { m ->
            m.groupValues[1].toIntOrNull()?.let { if (it > 0) return it }
        }
        Regex("""Build\s*#?\s*(\d+)""", RegexOption.IGNORE_CASE).find("$tag $body")?.let { m ->
            m.groupValues[1].toIntOrNull()?.let { if (it > 0) return it }
        }
        // Fallback: parse numeric semver like v1.2.3 -> 10203
        val nums = Regex("""\d+""").findAll(tag).mapNotNull { it.value.toIntOrNull() }.toList()
        if (nums.size >= 2) {
            val major = nums[0].coerceAtMost(99)
            val minor = nums[1].coerceAtMost(99)
            val patch = nums.getOrElse(2) { 0 }.coerceAtMost(99)
            return major * 10000 + minor * 100 + patch
        }
        return nums.firstOrNull()?.coerceAtLeast(1) ?: 0
    }

    fun parseVersionJsonToConfig(rawJson: String): AppReleaseConfig? {
        return runCatching {
            val obj = JSONObject(rawJson)
            val code = obj.optInt("latestVersionCode", 0)
            if (code <= 0) return null
            val name = obj.optString("latestVersionName", "").trim().ifBlank { "1.$code" }
            val url = obj.optString("apkDownloadUrl", "").trim().ifBlank { DEFAULT_APK_DOWNLOAD_URL }
            AppReleaseConfig(
                id = "github_latest",
                latestVersionCode = code,
                latestVersionName = name,
                releaseNotes = obj.optString("releaseNotes", "").trim(),
                releaseNotesUrdu = obj.optString("releaseNotesUrdu", "").trim(),
                apkDownloadUrl = url,
                mandatory = obj.optBoolean("mandatory", false),
                publishedBy = obj.optString("publishedBy", "GitHub Auto-Release").trim(),
                publishedAtIso = obj.optString("publishedAtIso", "").trim()
            )
        }.getOrNull()
    }

    fun parseGitHubApiReleaseToConfig(rawJson: String): AppReleaseConfig? {
        return runCatching {
            val obj = JSONObject(rawJson)
            if (obj.optBoolean("draft", false)) return null
            val tagName = obj.optString("tag_name", "").trim()
            val releaseName = obj.optString("name", "").trim()
            val body = obj.optString("body", "").trim()
            val code = extractBuildCodeFromTag(tagName, "$releaseName\n$body")
            if (code <= 0) return null

            val cleanVersionName = tagName
                .removePrefix("v")
                .removePrefix("V")
                .replace(Regex("""(?:-b|\+)\d+$"""), "")
                .ifBlank { releaseName.ifBlank { "1.$code" } }

            var apkUrl = ""
            val assets = obj.optJSONArray("assets")
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    val assetName = asset.optString("name", "")
                    val dlUrl = asset.optString("browser_download_url", "").trim()
                    if (assetName.endsWith(".apk", ignoreCase = true) && isValidApkUrl(dlUrl)) {
                        apkUrl = dlUrl
                        break
                    }
                }
            }
            if (apkUrl.isBlank()) apkUrl = DEFAULT_APK_DOWNLOAD_URL

            val publishedAt = obj.optString("published_at", "").take(10)
            AppReleaseConfig(
                id = "github_latest",
                latestVersionCode = code,
                latestVersionName = cleanVersionName,
                releaseNotes = body.ifBlank { "GitHub release $tagName" },
                releaseNotesUrdu = "نیا ورژن v$cleanVersionName (Build $code) GitHub سے دستیاب ہے۔",
                apkDownloadUrl = apkUrl,
                mandatory = body.contains("[mandatory]", ignoreCase = true),
                publishedBy = "GitHub Releases",
                publishedAtIso = publishedAt
            )
        }.getOrNull()
    }

    fun selectLatestRelease(
        githubRelease: AppReleaseConfig?,
        firestoreRelease: AppReleaseConfig?
    ): AppReleaseConfig? {
        return when {
            githubRelease == null -> firestoreRelease
            firestoreRelease == null -> githubRelease
            githubRelease.latestVersionCode >= firestoreRelease.latestVersionCode -> githubRelease
            else -> firestoreRelease
        }
    }

    suspend fun fetchLatestReleaseFromGitHub(): Result<AppReleaseConfig?> = withContext(Dispatchers.IO) {
        // 1. Primary path: CDN-backed version.json from Latest Release (no API rate limit)
        val versionJsonOutcome = fetchUrlText(GITHUB_LATEST_VERSION_JSON_URL, isGitHubApi = false)
        versionJsonOutcome.getOrNull()?.let { raw ->
            parseVersionJsonToConfig(raw)?.let { cfg ->
                return@withContext Result.success(cfg)
            }
        }

        // 2. Fallback path: GitHub REST Latest Release API
        val apiOutcome = fetchUrlText(GITHUB_LATEST_RELEASE_API_URL, isGitHubApi = true)
        return@withContext apiOutcome.fold(
            onSuccess = { raw ->
                if (raw == null) {
                    Result.success(null) // 404 = repo has no release published yet
                } else {
                    Result.success(parseGitHubApiReleaseToConfig(raw))
                }
            },
            onFailure = { err ->
                Result.failure(err)
            }
        )
    }

    private fun fetchUrlText(urlString: String, isGitHubApi: Boolean): Result<String?> {
        var conn: HttpURLConnection? = null
        return runCatching {
            val url = URL(urlString)
            conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "BrohiIceFactory-Android")
                if (isGitHubApi) {
                    setRequestProperty("Accept", "application/vnd.github+json")
                }
            }
            val code = conn!!.responseCode
            when {
                code in 200..299 -> conn!!.inputStream.bufferedReader().use { it.readText() }
                code == 404 -> null
                else -> throw IllegalStateException("GitHub responded with HTTP $code")
            }
        }.also {
            conn?.disconnect()
        }
    }

    fun canInstallPackages(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    fun openInstallUnknownAppsSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }

    fun downloadAndPrepareApk(
        context: Context,
        downloadUrl: String,
        versionName: String
    ): Flow<AppUpdateDownloadState> = flow {
        val cleanUrl = downloadUrl.trim()
        if (!isValidApkUrl(cleanUrl)) {
            emit(
                AppUpdateDownloadState.Failed(
                    "Invalid APK URL. Please ensure a valid HTTPS APK link is published."
                )
            )
            return@flow
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (dm == null) {
            emit(AppUpdateDownloadState.Failed("DownloadManager is unavailable on this device."))
            return@flow
        }

        val fileName = "brohi_ice_factory_v${versionName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}.apk"
        val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val targetFile = if (downloadsDir != null) File(downloadsDir, fileName) else null
        if (targetFile != null && targetFile.exists()) {
            targetFile.delete()
        }

        val request = DownloadManager.Request(Uri.parse(cleanUrl))
            .setTitle("Brohi Ice Factory Update v$versionName")
            .setDescription("Downloading latest app release...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setMimeType("application/vnd.android.package-archive")
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        if (targetFile != null) {
            request.setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )
        }

        val downloadId = dm.enqueue(request)
        emit(AppUpdateDownloadState.Downloading(0))

        var finished = false
        while (!finished) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            var cursor: Cursor? = null
            try {
                cursor = dm.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val downloadedIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                    val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)

                    val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_FAILED
                    val downloaded = if (downloadedIdx >= 0) cursor.getLong(downloadedIdx) else 0L
                    val total = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L
                    val reason = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else 0

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {
                            finished = true
                            val file = targetFile
                            if (file == null || !file.exists()) {
                                emit(AppUpdateDownloadState.Failed("Downloaded APK file could not be located."))
                            } else if (!canInstallPackages(context)) {
                                emit(AppUpdateDownloadState.NeedsInstallPermission)
                            } else {
                                emit(AppUpdateDownloadState.ReadyToInstall(file))
                            }
                        }
                        DownloadManager.STATUS_FAILED -> {
                            finished = true
                            emit(AppUpdateDownloadState.Failed("APK download failed (code $reason)."))
                        }
                        else -> {
                            val pct = if (total > 0) ((downloaded * 100L) / total).toInt().coerceIn(0, 99) else 0
                            emit(AppUpdateDownloadState.Downloading(pct))
                        }
                    }
                }
            } finally {
                cursor?.close()
            }
            if (!finished) {
                delay(450L)
            }
        }
    }.flowOn(Dispatchers.IO)

    fun launchApkInstaller(context: Context, apkFile: File): Result<Unit> = runCatching {
        if (!canInstallPackages(context)) {
            openInstallUnknownAppsSettings(context)
            throw IllegalStateException("Please allow 'Install unknown apps' permission, then tap Install again.")
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(installIntent)
    }
}
