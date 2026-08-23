package com.moyu.reader.data

import android.content.ContentResolver
import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import com.moyu.reader.data.preferences.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

data class LocalFont(val name: String, val path: String, val size: Long)

class FontRepository(
    private val context: Context,
    private val settings: SettingsRepository,
) {
    private val directory = File(context.filesDir, "fonts").apply { mkdirs() }
    private val _fonts = MutableStateFlow(scan())
    val fonts: StateFlow<List<LocalFont>> = _fonts.asStateFlow()

    suspend fun import(uri: Uri): Result<LocalFont> = withContext(Dispatchers.IO) {
        runCatching {
            val original = displayName(context.contentResolver, uri)
            val extension = original.substringAfterLast('.', "").lowercase()
            require(extension in setOf("ttf", "otf")) { "只支持 TTF 与 OTF 字体" }
            val target = File(directory, "${UUID.randomUUID()}.$extension")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output, 64 * 1024) }
            } ?: error("系统没有授予读取权限")
            require(target.length() in 1..MAX_FONT_BYTES) { "字体文件为空或超过 32 MB" }
            require(runCatching { Typeface.createFromFile(target) }.getOrNull() != null) { "字体文件已损坏" }
            File(target.parentFile, "${target.nameWithoutExtension}.name").writeText(original.substringBeforeLast('.'))
            val font = LocalFont(original.substringBeforeLast('.'), target.absolutePath, target.length())
            _fonts.value = scan()
            font
        }
    }

    suspend fun delete(font: LocalFont, activePath: String?) = withContext(Dispatchers.IO) {
        if (font.path == activePath) settings.setCustomFont(null)
        val file = File(font.path)
        File(file.parentFile, "${file.nameWithoutExtension}.name").delete()
        file.delete()
        _fonts.value = scan()
    }

    private fun scan(): List<LocalFont> = directory.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in setOf("ttf", "otf") }
        ?.map { file ->
            val name = File(file.parentFile, "${file.nameWithoutExtension}.name").takeIf(File::isFile)?.readText()?.trim().orEmpty()
            LocalFont(name.ifBlank { file.nameWithoutExtension }, file.absolutePath, file.length())
        }
        ?.sortedBy { it.name.lowercase() }
        .orEmpty()

    private fun displayName(resolver: ContentResolver, uri: Uri): String {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
        return "font.ttf"
    }

    companion object { private const val MAX_FONT_BYTES = 32L * 1024 * 1024 }
}
