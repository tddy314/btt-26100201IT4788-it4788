package com.example.human_posture

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

private fun Double?.display(digits: Int = 2) = if (this == null || !isFinite()) "—" else String.format(Locale.US, "%.${digits}f", this)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PoseLab(model: PoseViewModel) {
    val state by model.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var settings by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf<FrameResult?>(null) }
    var comparison by remember { mutableStateOf<FrameResult?>(null) }
    var choosingImages by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    var countText by rememberSaveable { mutableStateOf("20") }
    var onlyWithGt by rememberSaveable { mutableStateOf(true) }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.notice) {
        if (state.notice != "Chọn thư mục dữ liệu để nạp ảnh và Ground Truth.") snackbar.showSnackbar(state.notice, withDismissAction = true)
    }
    val busy = state.running || state.importing
    val tree = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) model.importTree(uri) else model.notice("Đã hủy chọn thư mục. Dữ liệu và kết quả trước đó được giữ nguyên.")
    }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(model::export) }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("POSE LAB", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text("Android · ML Kit Accurate", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                    TextButton(onClick = { settings = true }, enabled = !busy) { Text("Cấu hình") }
                    TextButton(onClick = { confirmReset = true }, enabled = !busy) { Text("Reset") }
                }
                Statistics(state)
                TabRow(selectedTabIndex = tab) {
                    listOf("Dữ liệu", "Kết quả (${state.results.size})", "Phương pháp").forEachIndexed { i, title ->
                        Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (state.running) {
                        Button(onClick = model::stop, enabled = !state.stopping, modifier = Modifier.weight(1f)) { Text(if (state.stopping) "Đang dừng…" else "Dừng sau ảnh này") }
                    } else {
                        Button(onClick = {
                            val count = if (countText.isBlank()) null else countText.toIntOrNull()
                            if (!state.manualSelection && countText.isNotBlank() && (count == null || count <= 0 || count % 5 != 0)) {
                                model.notice("Nhập số ảnh là bội số dương của 5, hoặc để trống để chọn tất cả.")
                            } else { model.start(count, onlyWithGt); if (model.state.value.running) tab = 1 }
                        }, enabled = !busy && state.images.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Bắt đầu xử lý") }
                    }
                    OutlinedButton(onClick = { export.launch("pose-results.csv") }, enabled = !busy && state.results.isNotEmpty()) { Text("Xuất CSV") }
                }
            }
        },
    ) { padding ->
        when (tab) {
            0 -> LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF132F43))) {
                        Column(Modifier.padding(20.dp)) {
                            Text("Từ ảnh đến bộ xương", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("Chọn dữ liệu trên máy. Nhận diện, đo thời gian và đánh giá từng frame ngay trên Android.", color = Color(0xFFCADDE8))
                        }
                    }
                }
                item {
                    Panel("01  /  Nạp dữ liệu") {
                        Button(onClick = { tree.launch(null) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Chọn thư mục test_set") }
                        Text("Tự tìm ảnh JPG gốc và NPY trong thư mục. Để hủy trình chọn thư mục Android, dùng nút Quay lại (◀) trên thanh máy ảo hoặc cử chỉ quay lại; dữ liệu cũ vẫn được giữ.", style = MaterialTheme.typography.bodySmall)
                        Text("${state.images.size} ảnh gốc  •  ${state.gt.size} tệp NPY", fontWeight = FontWeight.SemiBold)
                    }
                }
                item {
                    Panel("02  /  Chọn batch") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = !state.manualSelection, onClick = { model.selectionMode(false) }, enabled = !busy)
                            Text("Tự động: lấy N ảnh đầu tiên")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = state.manualSelection, onClick = { model.selectionMode(true) }, enabled = !busy)
                            Text("Thủ công: tự chọn ảnh")
                        }
                        if (state.manualSelection) {
                            OutlinedButton(onClick = { choosingImages = true }, enabled = !busy && state.images.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                                Text("Chọn ảnh trong danh sách (${state.selectedImages.size} đã chọn)")
                            }
                            Text("Chọn tổng số ảnh là bội số của 5. Bấm Hủy/Quay lại trong danh sách để giữ lựa chọn trước đó.", style = MaterialTheme.typography.bodySmall)
                        } else {
                        OutlinedTextField(value = countText, onValueChange = { countText = it }, enabled = !busy,
                            label = { Text("Số ảnh đầu tiên (bội số của 5)") }, supportingText = { Text("Để trống để dùng tất cả ảnh phù hợp. Sắp theo thư mục, clip, frame.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(), singleLine = true)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = onlyWithGt, onCheckedChange = { onlyWithGt = it }, enabled = !busy)
                            Text("Chỉ chọn ảnh có Ground Truth", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text("Đánh giá: ${state.config.unit}", fontWeight = FontWeight.SemiBold)
                    }
                }
                item { Notice(state.notice, state.importing) }
                if (state.images.isNotEmpty()) {
                    item { Text("Ảnh đầu danh sách nhập", style = MaterialTheme.typography.titleMedium) }
                    items(state.images.take(10), key = { it.file.uri.toString() }) { image ->
                        Text("${image.file.path}\nFrame dự kiến: ${image.frame.index}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            1 -> LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item { Notice(state.notice, state.running) }
                if (state.results.isEmpty()) item {
                    Panel("Kết quả sẽ xuất hiện ở đây") { Text("Nạp ảnh ở tab Dữ liệu rồi bấm Bắt đầu xử lý. Chạm vào ảnh kết quả để phóng to.") }
                }
                items(state.results, key = { it.input.file.uri.toString() }) { result ->
                    ResultCard(result, state.runConfig, onClick = { expanded = result }, onCompare = { comparison = result })
                }
            }
            else -> Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                val evaluation = if (state.results.isNotEmpty() || state.running) state.runConfig else state.config
                Text("Cách đọc kết quả", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Đơn vị", fontWeight = FontWeight.Bold)
                Text(if (evaluation.normalized)
                    "Đang dùng tọa độ chuẩn hóa: lấy tâm hông làm gốc và điều chỉnh kích thước từng pose về cùng thang đo. Sai số thể hiện mức chênh lệch tương đối."
                    else "Đang dùng đơn vị mm theo hệ số hiệu chuẩn đã lưu trong cấu hình.")
                Text("MPJPE", fontWeight = FontWeight.Bold)
                Text("Khoảng cách trung bình giữa 17 cặp khớp dự đoán và Ground Truth sau khi đưa về cùng quy ước tọa độ. Giá trị càng gần 0, hai pose càng khớp nhau.")
                Text("PA-MPJPE", fontWeight = FontWeight.Bold)
                Text("Sai số trung bình sau khi căn chỉnh vị trí, hướng và kích thước pose dự đoán với Ground Truth.")
                Text("Thời gian xử lý", fontWeight = FontWeight.Bold)
                Text("Đo từ lúc bắt đầu đọc ảnh đến khi nhận diện hoàn tất, tính bằng ms. Dòng ML cho biết thời gian gọi và chờ ML Kit trả kết quả.")
                Text("Xem từng khớp", fontWeight = FontWeight.Bold)
                Text("Bấm Chi tiết so sánh GT dưới mỗi ảnh để xem tọa độ và sai lệch từng khớp. Thanh phía trên hiển thị trung bình sai số của các frame đánh giá được và tiến độ xử lý.")
            }
        }
    }
    if (settings) SettingsDialog(state.config, onDismiss = { settings = false }) { model.config(it); settings = false }
    if (choosingImages) ImageSelectionDialog(state, onDismiss = { choosingImages = false }) {
        model.selectImages(it); choosingImages = false
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false }, title = { Text("Reset phiên thử?") },
        text = { Text("Xóa danh sách đã nạp, lựa chọn ảnh, kết quả và thống kê để bắt đầu lần thử mới. Giữ cấu hình đánh giá. Ảnh, NPY và CSV đã lưu không bị xóa.") },
        confirmButton = { TextButton(onClick = {
            model.reset(); countText = "20"; onlyWithGt = true; expanded = null; comparison = null
            choosingImages = false; tab = 0; confirmReset = false
        }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Hủy") } })
    comparison?.let { ComparisonDialog(it, state.runConfig, onDismiss = { comparison = null }) }
    expanded?.let { r ->
        Dialog(onDismissRequest = { expanded = null }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxWidth().padding(12.dp), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(r.input.file.name, fontWeight = FontWeight.Bold)
                    Text("Dùng hai ngón tay để phóng to và di chuyển.", style = MaterialTheme.typography.bodySmall)
                    var scale by remember(r) { mutableFloatStateOf(1f) }
                    var pan by remember(r) { mutableStateOf(Offset.Zero) }
                    Box(Modifier.fillMaxWidth().heightIn(max = 500.dp).clip(RoundedCornerShape(12.dp)).pointerInput(r) {
                        detectTransformGestures { _, delta, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); pan = if (scale > 1f) pan + delta else Offset.Zero }
                    }) {
                        PosePreview(r, state.runConfig.threshold, Modifier.graphicsLayer { scaleX = scale; scaleY = scale; translationX = pan.x; translationY = pan.y })
                    }
                    TextButton(onClick = { expanded = null }, modifier = Modifier.align(Alignment.End)) { Text("Đóng") }
                }
            }
        }
    }
}

@Composable
private fun Statistics(state: LabState) {
    val errors = state.results.mapNotNull { it.errors }
    val times = state.results.mapNotNull { it.loadToPoseMs }
    val unit = if (state.results.isEmpty() && !state.running) state.config.unit else state.runConfig.unit
    Column(Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${state.results.size} / ${state.total} frame", fontWeight = FontWeight.Bold)
            Text("${errors.size} có sai số · $unit", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(progress = { if (state.total == 0) 0f else state.results.size.toFloat() / state.total }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("MPJPE TB", errors.map { it.mpjpe }.average().display(3))
            Stat("PA-MPJPE TB", errors.map { it.paMpjpe }.average().display(3))
            Stat("Thời gian TB", "${times.average().display(1)} ms")
        }
    }
}

@Composable private fun Stat(label: String, value: String) {
    Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary); Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
}

@Composable private fun Panel(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable private fun Notice(text: String, active: Boolean) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (active) LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(text, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable private fun ResultCard(result: FrameResult, config: EvaluationConfig, onClick: () -> Unit, onCompare: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        PosePreview(result, config.threshold, Modifier.clickable(onClick = onClick))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(result.input.file.name, fontWeight = FontWeight.Bold)
            Text(result.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            Text("${result.loadToPoseMs.display(1)} ms · ML ${result.inferenceMs.display(1)} ms${if (result.firstInference) " · lần đầu" else ""}", style = MaterialTheme.typography.bodyMedium)
            Text("MPJPE ${result.errors?.mpjpe.display(3)}  /  PA-MPJPE ${result.errors?.paMpjpe.display(3)} (${config.unit})", style = MaterialTheme.typography.bodyMedium)
            if (result.gtIndex != null) Text("GT frame ${result.gtIndex} · ${result.width}×${result.height}", style = MaterialTheme.typography.labelSmall)
            OutlinedButton(onClick = onCompare, modifier = Modifier.fillMaxWidth()) { Text("Chi tiết so sánh GT") }
        }
    }
}

@Composable private fun PosePreview(result: FrameResult, threshold: Float, modifier: Modifier = Modifier) {
    val bitmap by produceState<ImageBitmap?>(null, result.thumbnail) {
        value = withContext(Dispatchers.IO) { result.thumbnail?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() } }
    }
    val aspect = if (result.width > 0 && result.height > 0) result.width.toFloat() / result.height else 16f/9f
    Box(modifier.fillMaxWidth().aspectRatio(aspect).background(Color(0xFFE5EBEF))) {
        bitmap?.let { Image(it, contentDescription = "Ảnh ${result.input.file.name}", modifier = Modifier.fillMaxSize()) }
        if (result.width > 0 && result.height > 0) Canvas(Modifier.fillMaxSize()) {
            val byType = result.landmarks.filter { it.confidence >= threshold && it.p.finite() }.associateBy { it.type }
            fun position(l: Landmark) = Offset((l.p.x / result.width * size.width).toFloat(), (l.p.y / result.height * size.height).toFloat())
            val edges = listOf(11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16, 11 to 23, 12 to 24,
                23 to 24, 23 to 25, 25 to 27, 24 to 26, 26 to 28, 27 to 29, 29 to 31, 28 to 30, 30 to 32)
            for ((i,j) in edges) {
                val a = byType[i]; val b = byType[j]
                if (a != null && b != null) drawLine(if (i % 2 == 1) Color(0xFF2DE3C0) else Color(0xFFFFBF64), position(a), position(b), strokeWidth = 2.5.dp.toPx())
            }
            for (l in byType.values) {
                drawCircle(Color(0xFF15354A), 3.5.dp.toPx(), position(l))
                drawCircle(Color.White, 2.dp.toPx(), position(l))
            }
        }
    }
}

@Composable private fun SettingsDialog(config: EvaluationConfig, onDismiss: () -> Unit, onSave: (EvaluationConfig) -> Unit) {
    var normalized by remember { mutableStateOf(config.normalized) }
    var pixelScale by remember { mutableStateOf(config.pixelToMm.toString()) }
    var gtScale by remember { mutableStateOf(config.gtToMm.toString()) }
    var axes by remember { mutableStateOf(config.axisOrder) }
    var signs by remember { mutableStateOf(config.signs.values().joinToString(",") { it.toInt().toString() }) }
    var threshold by remember { mutableStateOf(config.threshold.toString()) }
    var offset by remember { mutableStateOf(config.frameOffset.toString()) }
    var stride by remember { mutableStateOf(config.frameStride.toString()) }
    var mapping by remember { mutableStateOf(config.mappingConfirmed) }
    var calibration by remember { mutableStateOf(config.metricCalibrationConfirmed) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Cấu hình đánh giá") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Mặc định cho kết quả thử nghiệm không đơn vị. Chế độ mm cần thông tin hiệu chuẩn đã xác minh.", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = normalized, onCheckedChange = { normalized = it }); Spacer(Modifier.width(8.dp)); Text("Chuẩn hóa độc lập")
            }
            SettingField("GT → trục dự đoán (XYZ, XZY…)", axes) { axes = it.uppercase(Locale.ROOT) }
            SettingField("Dấu trục đầu ra, ví dụ 1,-1,1", signs) { signs = it }
            SettingField("Ngưỡng landmark (0–1)", threshold) { threshold = it }
            SettingField("GT frame offset (≥0)", offset) { offset = it }
            SettingField("GT frame stride (≥1)", stride) { stride = it }
            if (!normalized) {
                SettingField("mm / pixel ảnh đã xử lý", pixelScale) { pixelScale = it }
                SettingField("mm / đơn vị Ground Truth", gtScale) { gtScale = it }
                Text("Một scale cố định không thay thế phép chiếu camera hoặc hiệu chuẩn độ sâu.", style = MaterialTheme.typography.bodySmall)
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(mapping, { mapping = it }); Text("Đã xác minh thứ tự khớp và quy tắc frame", style = MaterialTheme.typography.bodySmall) }
            if (!normalized) Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(calibration, { calibration = it }); Text("Đã xác minh hệ tọa độ, đơn vị và scale cố định", style = MaterialTheme.typography.bodySmall) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = {
        TextButton(onClick = {
            try {
                val s = signs.split(',').map { it.trim().toDouble() }; require(s.size == 3) { "Cần ba dấu trục" }
                val value = EvaluationConfig(normalized, pixelScale.toDouble(), gtScale.toDouble(), axes.trim(), Vec3(s[0],s[1],s[2]),
                    threshold.toFloat(), offset.toInt(), stride.toInt(), mapping, calibration)
                value.validate(); onSave(value)
            } catch (e: Exception) { error = e.message ?: "Cấu hình không hợp lệ" }
        }) { Text("Lưu") }
    }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable private fun SettingField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth())
}
