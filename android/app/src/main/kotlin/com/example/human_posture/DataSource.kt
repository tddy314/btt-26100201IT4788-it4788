package com.example.human_posture

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class SourceFile(val uri: Uri, val name: String, val scope: String = "", val path: String = name)
data class ImageInput(val file: SourceFile, val frame: FrameName)
data class ImportedData(val images: List<ImageInput>, val gt: List<SourceFile>, val ignored: Int, val duplicates: Int)

object DataSource {
    fun name(resolver: ContentResolver, uri: Uri): String =
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

    suspend fun scan(resolver: ContentResolver, tree: Uri, onProgress: (Int) -> Unit): List<SourceFile> {
        val files = mutableListOf<SourceFile>()
        val visited = mutableSetOf<String>()
        var count = 0
        suspend fun walk(id: String, parent: String, depth: Int) {
            currentCoroutineContext().ensureActive()
            require(depth <= 32) { "Thư mục quá sâu" }
            if (!visited.add(id)) return
            val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
            val children = mutableListOf<Triple<String, String, String>>()
            resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE), null, null, null)?.use { c ->
                while (c.moveToNext()) children.add(Triple(c.getString(0), c.getString(1), c.getString(2)))
            }
            for ((childId, childName, mime) in children.sortedBy { it.second }) {
                currentCoroutineContext().ensureActive()
                val path = if (parent.isEmpty()) childName else "$parent/$childName"
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) walk(childId, path, depth + 1)
                else {
                    count++
                    if (childName.endsWith(".jpg", true) || childName.endsWith(".npy", true))
                        files.add(SourceFile(DocumentsContract.buildDocumentUriUsingTree(tree, childId), childName, parent, path))
                    if (count % 100 == 0) onProgress(count)
                }
            }
        }
        walk(DocumentsContract.getTreeDocumentId(tree), "", 0)
        return files
    }

    fun prepare(files: List<SourceFile>): ImportedData {
        val jpg = files.filter { it.name.endsWith(".jpg", true) }
        val parsed = jpg.mapNotNull { f -> FrameName.parse(f.name)?.let { ImageInput(f, it) } }
        val deduped = parsed.sortedWith(compareBy<ImageInput> { it.frame.duplicate }.thenBy { it.file.path })
            .distinctBy { it.file.scope + "/" + it.frame.key }
            .sortedWith(compareBy<ImageInput> { it.file.scope }.thenBy { it.frame.clip }
                .thenBy { it.frame.index })
        return ImportedData(deduped, files.filter { it.name.endsWith(".npy", true) },
            jpg.size - parsed.size, parsed.size - deduped.size)
    }

    fun match(image: ImageInput, groundTruth: List<SourceFile>): SourceFile? {
        val candidates = groundTruth.filter { FrameName.canonicalGtName(it.name).equals(image.frame.gtName, true) }
        val siblings = candidates.filter { it.scope == image.file.scope }
        fun unique(files: List<SourceFile>): SourceFile? {
            val originals = files.filter { it.name.equals(image.frame.gtName, true) }
            if (originals.size == 1) return originals.single()
            require(files.size <= 1) { "Ground Truth trùng tên: hãy nhập theo thư mục để phân biệt S1/S2/S3" }
            return files.singleOrNull()
        }
        if (siblings.isNotEmpty()) return unique(siblings)
        require(siblings.isEmpty()) { "Nhiều Ground Truth cùng thư mục" }
        // A named folder with missing GT must never borrow another subject's labels.
        if (image.file.scope.isNotEmpty() && candidates.any { it.scope.isNotEmpty() }) return null
        return unique(candidates)
    }
}
