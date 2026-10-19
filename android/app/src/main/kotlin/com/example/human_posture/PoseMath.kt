package com.example.human_posture

import org.ejml.data.DMatrixRMaj
import org.ejml.dense.row.factory.DecompositionFactory_DDRM
import kotlin.math.sqrt

data class Vec3(val x: Double, val y: Double, val z: Double) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Double) = Vec3(x * s, y * s, z * s)
    fun norm() = sqrt(x * x + y * y + z * z)
    fun values() = doubleArrayOf(x, y, z)
    fun finite() = x.isFinite() && y.isFinite() && z.isFinite()
}

data class Landmark(val type: Int, val p: Vec3, val confidence: Float)

/** Provisional H36M-17 mapping. Nose and ear midpoint are proxies, not anatomical equivalents. */
object JointMapping {
    val names = listOf("pelvis", "right_hip", "right_knee", "right_ankle", "left_hip",
        "left_knee", "left_ankle", "spine", "thorax", "neck_nose_proxy", "head_ears_proxy",
        "left_shoulder", "left_elbow", "left_wrist", "right_shoulder", "right_elbow", "right_wrist")
    val edges = listOf(0 to 1, 1 to 2, 2 to 3, 0 to 4, 4 to 5, 5 to 6,
        0 to 7, 7 to 8, 8 to 9, 9 to 10, 8 to 11, 11 to 12, 12 to 13,
        8 to 14, 14 to 15, 15 to 16)
    fun map(points: List<Landmark>, threshold: Float): List<Vec3> {
        val byId = points.associateBy { it.type }
        fun p(id: Int): Vec3 {
            val l = requireNotNull(byId[id]) { "Thiếu landmark $id" }
            require(l.confidence >= threshold && l.p.finite()) { "Landmark $id không đủ tin cậy" }
            return l.p
        }
        val pelvis = (p(23) + p(24)) * 0.5
        val thorax = (p(11) + p(12)) * 0.5
        return listOf(pelvis, p(24), p(26), p(28), p(23), p(25), p(27),
            (pelvis + thorax) * 0.5, thorax, p(0), (p(7) + p(8)) * 0.5,
            p(11), p(13), p(15), p(12), p(14), p(16))
    }
}

data class EvaluationConfig(
    val normalized: Boolean = true,
    val pixelToMm: Double = 1.0,
    val gtToMm: Double = 1.0,
    val axisOrder: String = "XYZ",
    val signs: Vec3 = Vec3(1.0, 1.0, 1.0),
    val threshold: Float = 0.5f,
    val frameOffset: Int = 0,
    val frameStride: Int = 1,
    val mappingConfirmed: Boolean = false,
    val metricCalibrationConfirmed: Boolean = false,
) {
    val unit get() = if (normalized) "chuẩn hóa" else "mm"
    val label get() = if (normalized) "Root + RMS độc lập; mapping H36M-17 giả định" else "Root + scale cố định; cấu hình do người dùng xác nhận"
    fun validate() {
        require(axisOrder.length == 3 && axisOrder.toSet() == setOf('X', 'Y', 'Z')) { "Hệ trục phải là hoán vị XYZ" }
        require(pixelToMm.isFinite() && pixelToMm > 0 && gtToMm.isFinite() && gtToMm > 0) { "Hệ số phải dương và hữu hạn" }
        require(signs.values().all { it == 1.0 || it == -1.0 }) { "Dấu trục phải là +1 hoặc -1" }
        require(threshold in 0f..1f && frameStride in 1..10000 && frameOffset in 0..100000) { "Cấu hình frame/confidence không hợp lệ" }
        require(normalized || (mappingConfirmed && metricCalibrationConfirmed)) { "Cần xác nhận mapping, hệ trục và hiệu chuẩn trước khi báo mm" }
    }
}

data class Errors(val mpjpe: Double, val paMpjpe: Double)
data class JointComparison(val name: String, val prediction: Vec3, val groundTruth: Vec3,
    val alignedPrediction: Vec3, val error: Double, val alignedError: Double)
data class Comparison(val errors: Errors, val joints: List<JointComparison>)

object PoseMath {
    fun rootRelative(p: List<Vec3>) = p.map { it - p.first() }
    fun normalized(p: List<Vec3>): List<Vec3> {
        val centered = rootRelative(p)
        val rms = sqrt(centered.sumOf { val n = it.norm(); n * n } / centered.size)
        require(rms > 1e-10 && rms.isFinite()) { "Pose suy biến" }
        return centered.map { it * (1 / rms) }
    }
    fun mpjpe(p: List<Vec3>, g: List<Vec3>): Double {
        require(p.size == g.size && p.size >= 3 && (p + g).all { it.finite() })
        return p.indices.sumOf { (p[it] - g[it]).norm() } / p.size
    }

    /** Fits s R p + t to GT. Enforces det(R)=+1; reflections are forbidden. */
    fun align(p: List<Vec3>, g: List<Vec3>): List<Vec3> {
        require(p.size == g.size && p.size >= 3 && (p + g).all { it.finite() })
        val pc = p.reduce(Vec3::plus) * (1.0 / p.size)
        val gc = g.reduce(Vec3::plus) * (1.0 / g.size)
        val x = p.map { it - pc }; val y = g.map { it - gc }
        val variance = x.sumOf { val n = it.norm(); n * n }
        require(variance > 1e-12) { "Pose suy biến" }
        val h = DMatrixRMaj(3, 3)
        for (k in x.indices) for (i in 0..2) for (j in 0..2)
            h.add(i, j, x[k].values()[i] * y[k].values()[j])
        val svd = DecompositionFactory_DDRM.svd(3, 3, true, true, false)
        require(svd.decompose(h)) { "SVD thất bại" }
        val u = svd.getU(null, false); val v = svd.getV(null, false)
        val singular = svd.singularValues
        val flipIndex = (0..2).minBy { singular[it] }
        fun rotation(last: Double) = Array(3) { i -> DoubleArray(3) { j ->
            (0..2).sumOf { k -> v.get(i, k) * (if (k == flipIndex) last else 1.0) * u.get(j, k) }
        } }
        fun det(a: Array<DoubleArray>) = a[0][0] * (a[1][1]*a[2][2]-a[1][2]*a[2][1]) -
            a[0][1] * (a[1][0]*a[2][2]-a[1][2]*a[2][0]) + a[0][2] * (a[1][0]*a[2][1]-a[1][1]*a[2][0])
        val sign = if (det(rotation(1.0)) < 0) -1.0 else 1.0
        val r = rotation(sign)
        val scale = (0..2).sumOf { singular[it] * (if (it == flipIndex) sign else 1.0) } / variance
        return x.map { q ->
            val a = q.values()
            Vec3((0..2).sumOf { r[0][it]*a[it] }, (0..2).sumOf { r[1][it]*a[it] },
                (0..2).sumOf { r[2][it]*a[it] }) * scale + gc
        }
    }

    fun evaluate(pred: List<Vec3>, gt: List<Vec3>, config: EvaluationConfig): Errors = compare(pred, gt, config).errors

    fun compare(pred: List<Vec3>, gt: List<Vec3>, config: EvaluationConfig): Comparison {
        config.validate()
        require(pred.size == 17 && gt.size == 17)
        val transformed = gt.map { q ->
            val a = q.values(); val s = config.signs.values()
            val b = config.axisOrder.mapIndexed { i, c -> a["XYZ".indexOf(c)] * s[i] }
            Vec3(b[0], b[1], b[2])
        }
        val p = if (config.normalized) normalized(pred) else rootRelative(pred).map { it * config.pixelToMm }
        val g = if (config.normalized) normalized(transformed) else rootRelative(transformed).map { it * config.gtToMm }
        val aligned = align(p, g)
        return Comparison(Errors(mpjpe(p, g), mpjpe(aligned, g)), p.indices.map { i ->
            JointComparison(JointMapping.names[i], p[i], g[i], aligned[i],
                (p[i] - g[i]).norm(), (aligned[i] - g[i]).norm())
        })
    }
}
