package com.aura.launcher

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

/**
 * FileResult — ek mile hue file ki jaankari.
 */
data class FileResult(
    val name: String,
    val uri: Uri,
    val mimeType: String,
    val type: FileType
)

enum class FileType { IMAGE, VIDEO, AUDIO, DOCUMENT }

/**
 * FileSearch — phone ki files (photos, videos, music, docs) dhoondhta hai.
 *
 * MediaStore use karta hai — ye Android ka safe tareeka hai jo
 * "all files access" ke bina kaam karta hai. Koi API/internet nahi.
 *
 * Universal search: drawer mein app ke saath files bhi dikhti hain.
 */
object FileSearch {

    /** Permission mili hai ya nahi (Android version ke hisaab se). */
    fun hasPermission(context: Context): Boolean {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_AUDIO
            )
        } else {
            arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        return perms.any {
            context.checkSelfPermission(it) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Naam se files dhoondho. Max 'limit' results (jaldi rahe).
     */
    fun search(context: Context, query: String, limit: Int = 15): List<FileResult> {
        if (query.isBlank() || query.length < 2) return emptyList()
        if (!hasPermission(context)) return emptyList()

        val results = mutableListOf<FileResult>()
        val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$query%")

        // Images, Videos, Audio — sab MediaStore se
        queryStore(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            FileType.IMAGE, "image/*", selection, args, limit, results)
        queryStore(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            FileType.VIDEO, "video/*", selection, args, limit, results)
        queryStore(context, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            FileType.AUDIO, "audio/*", selection, args, limit, results)

        return results.take(limit)
    }

    private fun queryStore(
        context: Context,
        baseUri: Uri,
        type: FileType,
        mime: String,
        selection: String,
        args: Array<String>,
        limit: Int,
        out: MutableList<FileResult>
    ) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME
        )
        runCatching {
            context.contentResolver.query(
                baseUri, projection, selection, args,
                "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                var count = 0
                while (cursor.moveToNext() && count < limit) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol) ?: continue
                    val uri = Uri.withAppendedPath(baseUri, id.toString())
                    out.add(FileResult(name, uri, mime, type))
                    count++
                }
            }
        }
    }

    /** File ko uski default app mein kholo. */
    fun openFile(context: Context, file: FileResult) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(file.uri, file.mimeType)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        }.onFailure {
            android.widget.Toast.makeText(
                context, "File khulne wali app nahi mili",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
}
