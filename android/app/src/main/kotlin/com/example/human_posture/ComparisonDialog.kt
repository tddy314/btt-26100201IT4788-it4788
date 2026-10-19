package com.example.human_posture

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.util.Locale

private fun Vec3.coordinates() = String.format(Locale.US, "(%.4f, %.4f, %.4f)", x, y, z)
private fun Double.metric() = String.format(Locale.US, "%.6f", this)

@Composable
fun ComparisonDialog(result: FrameResult, config: EvaluationConfig, onDismiss: () -> Unit) {
    val names = listOf("Chậu", "Hông phải", "Gối phải", "Cổ chân phải", "Hông trái", "Gối trái", "Cổ chân trái",
        "Cột sống", "Ngực", "Cổ (điểm thay thế)", "Đầu (điểm thay thế)", "Vai trái", "Khuỷu trái", "Cổ tay trái", "Vai phải", "Khuỷu phải", "Cổ tay phải")
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxWidth().fillMaxHeight(.9f).padding(12.dp), shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("So sánh Ground Truth", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Đóng") }
                }
                Text(result.input.file.name, fontWeight = FontWeight.Bold)
                val comparison = result.comparison
                if (comparison == null) {
                    Text("Chưa có so sánh cho ảnh này: ${result.message}", modifier = Modifier.padding(vertical = 16.dp))
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                        item {
                            Text("GT: ${result.gtPath}\nFrame: ${result.gtIndex} (bắt đầu từ 0)", style = MaterialTheme.typography.bodySmall)
                            Text("MPJPE: ${comparison.errors.mpjpe.metric()}\nPA-MPJPE: ${comparison.errors.paMpjpe.metric()}\nĐơn vị: ${config.unit}", fontWeight = FontWeight.SemiBold)
                            Text(if (config.normalized) "Tọa độ dưới đây đã trừ pelvis và chuẩn hóa RMS riêng cho mỗi pose; không phải tọa độ thô hoặc mm. GT đã đổi trục theo cấu hình."
                                else "Tọa độ dưới đây đã trừ pelvis, đổi trục GT và dùng hệ số cố định trong cấu hình (mm).", style = MaterialTheme.typography.bodySmall)
                            Text("Khoảng cách = √(Δx² + Δy² + Δz²). Trung bình 17 khoảng cách là MPJPE; trung bình sau căn chỉnh là PA-MPJPE.", style = MaterialTheme.typography.bodySmall)
                        }
                        itemsIndexed(comparison.joints) { index, joint ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text("$index · ${names[index]}", fontWeight = FontWeight.Bold)
                                    Text("Dự đoán: ${joint.prediction.coordinates()}")
                                    Text("GT: ${joint.groundTruth.coordinates()}")
                                    Text("Δ dự đoán − GT: ${(joint.prediction - joint.groundTruth).coordinates()}", style = MaterialTheme.typography.bodySmall)
                                    Text("Sai lệch: ${joint.error.metric()} ${config.unit}", fontWeight = FontWeight.SemiBold)
                                    Text("Dự đoán sau PA: ${joint.alignedPrediction.coordinates()}", style = MaterialTheme.typography.bodySmall)
                                    Text("Sai lệch sau PA: ${joint.alignedError.metric()} ${config.unit}")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
