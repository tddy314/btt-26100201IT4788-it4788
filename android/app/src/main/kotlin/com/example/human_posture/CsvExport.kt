package com.example.human_posture

import java.io.Writer

object CsvExport {
    private fun cell(value: Any?) = "\"" + value.toString().let { if (value == null) "" else it }.replace("\"", "\"\"") + "\""
    fun write(writer: Writer, results: List<FrameResult>, total: Int, config: EvaluationConfig) {
        fun row(vararg values: Any?) { writer.write(values.joinToString(",") { cell(it) } + "\r\n") }
        writer.write("\uFEFF")
        row("protocol", config.label)
        row("unit", config.unit, "mapping_confirmed", config.mappingConfirmed, "calibration_confirmed", config.metricCalibrationConfirmed)
        row("frame_rule", "offset + ((Y-1)*5+Z-1)*stride", "offset", config.frameOffset, "stride", config.frameStride)
        row("gt_axes", config.axisOrder, "signs", config.signs, "prediction_mm_per_processed_pixel", config.pixelToMm, "gt_to_mm", config.gtToMm)
        row("joint_order", JointMapping.names.joinToString(";"), "confidence", config.threshold)
        row("timing", "read/decode/EXIF + input preparation + ML Kit await; excludes metrics/drawing/cache; includes first inference")
        row("processed", results.size, "total", total, "evaluated", results.count { it.errors != null })
        val evaluated = results.mapNotNull { it.errors }
        val times = results.mapNotNull { it.loadToPoseMs }
        row("mean_mpjpe", evaluated.map { it.mpjpe }.average().takeIf { it.isFinite() },
            "mean_pa_mpjpe", evaluated.map { it.paMpjpe }.average().takeIf { it.isFinite() },
            "mean_load_to_pose_ms", times.average().takeIf { it.isFinite() })
        row("file", "path", "clip", "Y", "Z", "gt_file", "gt_index_zero_based", "width", "height", "load_to_pose_ms", "inference_ms",
            "mpjpe", "pa_mpjpe", "unit", "first_inference", "status", "landmarks_type_x_y_z_confidence")
        for (r in results) row(r.input.file.name, r.input.file.path, r.input.frame.clip, r.input.frame.second,
            r.input.frame.withinSecond, r.gtPath, r.gtIndex, r.width, r.height, r.loadToPoseMs, r.inferenceMs,
            r.errors?.mpjpe, r.errors?.paMpjpe, config.unit, r.firstInference, r.message,
            r.landmarks.joinToString(";") { "${it.type}:${it.p.x}:${it.p.y}:${it.p.z}:${it.confidence}" })
    }
}
