package com.example.human_posture

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ImageSelectionDialog(state: LabState, onDismiss: () -> Unit, onSave: (Set<String>) -> Unit) {
    var draft by remember { mutableStateOf(state.selectedImages) }
    val gtStatus = remember(state.images, state.gt) {
        state.images.associate { image -> image.file.uri.toString() to runCatching {
            if (DataSource.match(image, state.gt) != null) "Có GT" else "Thiếu GT"
        }.getOrElse { "GT trùng tên" } }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.92f).padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(12.dp)) {
                Text("Chọn ảnh thủ công", style = MaterialTheme.typography.titleLarge)
                Text("Đã chọn ${draft.size} / ${state.images.size} ảnh. Batch cần là bội số của 5.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { draft = state.images.map { it.file.uri.toString() }.toSet() }) { Text("Chọn tất cả") }
                    TextButton(onClick = { draft = emptySet() }) { Text("Bỏ chọn tất cả") }
                }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.images, key = { it.file.uri.toString() }) { image ->
                        val key = image.file.uri.toString()
                        fun toggle() { draft = if (key in draft) draft - key else draft + key }
                        Row(Modifier.fillMaxWidth().clickable(onClick = ::toggle).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(key in draft, onCheckedChange = { toggle() })
                            SelectionThumbnail(image)
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text(image.file.name, style = MaterialTheme.typography.bodyMedium)
                                Text("${gtStatus[key]} · ${image.file.scope}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Hủy / Quay lại") }
                    Button(onClick = { onSave(draft) }) { Text("Dùng ${draft.size} ảnh") }
                }
            }
        }
    }
}

@Composable private fun SelectionThumbnail(image: ImageInput) {
    val resolver = LocalContext.current.contentResolver
    val bitmap by produceState<ImageBitmap?>(null, image.file.uri) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resolver.openInputStream(image.file.uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
                resolver.openInputStream(image.file.uri)?.use { BitmapFactory.decodeStream(it, null,
                    BitmapFactory.Options().apply { inSampleSize = sample }) }?.asImageBitmap()
            }.getOrNull()
        }
    }
    Box(Modifier.size(80.dp, 55.dp), contentAlignment = Alignment.Center) {
        bitmap?.let { Image(it, contentDescription = image.file.name, modifier = Modifier.fillMaxSize()) }
            ?: Text("JPG", style = MaterialTheme.typography.labelSmall)
    }
}
