package com.example.human_posture

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GroundTruth(val frames: List<List<Vec3>>)

/** Numeric NPY v1/v2/v3, float32/float64, either byte order, C and Fortran layout. */
object NpyReader {
    fun read(input: InputStream): GroundTruth {
        fun bytes(n: Int): ByteArray {
            require(n in 0..64_000_000) { "Tệp NPY quá lớn" }
            val a = ByteArray(n); var offset = 0
            while (offset < n) {
                val count = input.read(a, offset, n - offset)
                require(count > 0) { "Tệp NPY bị cắt ngắn" }
                offset += count
            }
            return a
        }
        require(bytes(6).contentEquals(byteArrayOf(0x93.toByte(), 78, 85, 77, 80, 89))) { "Không phải tệp NPY" }
        val version = bytes(2); val major = version[0].toInt() and 255
        require(major in 1..3) { "Phiên bản NPY không hỗ trợ" }
        val lengthBytes = bytes(if (major == 1) 2 else 4)
        val lb = ByteBuffer.wrap(lengthBytes).order(ByteOrder.LITTLE_ENDIAN)
        val length = if (major == 1) lb.short.toInt() and 65535 else lb.int
        require(length in 1..65536) { "Header NPY không hợp lệ" }
        val header = bytes(length).toString(if (major == 3) Charsets.UTF_8 else Charsets.ISO_8859_1)
        val dtype = requireNotNull(Regex("['\"]descr['\"]\\s*:\\s*['\"]([^'\"]+)['\"]").find(header)) { "Thiếu dtype" }.groupValues[1]
        require(dtype in listOf("<f8", ">f8", "=f8", "<f4", ">f4", "=f4")) { "Chỉ hỗ trợ float32/float64, nhận $dtype" }
        val shapeText = requireNotNull(Regex("['\"]shape['\"]\\s*:\\s*\\(([^)]*)\\)").find(header)) { "Thiếu shape" }.groupValues[1]
        val shape = shapeText.split(',').map { it.trim() }.filter { it.isNotEmpty() }.map { it.toInt() }
        require(shape.size == 3 && shape[0] in 1..100000 && shape[1] == 17 && shape[2] == 3) { "Cần shape (F,17,3), nhận $shape" }
        val fortran = requireNotNull(Regex("['\"]fortran_order['\"]\\s*:\\s*(True|False)").find(header)) { "Thiếu fortran_order" }.groupValues[1] == "True"
        val size = if (dtype.endsWith("8")) 8 else 4
        val order = when (dtype[0]) { '>' -> ByteOrder.BIG_ENDIAN; '=' -> ByteOrder.nativeOrder(); else -> ByteOrder.LITTLE_ENDIAN }
        val buffer = ByteBuffer.wrap(bytes(shape[0] * 17 * 3 * size)).order(order)
        fun value(f: Int, j: Int, c: Int): Double {
            val offset = (if (fortran) f + shape[0] * (j + 17 * c) else (f * 17 + j) * 3 + c) * size
            val v = if (size == 8) buffer.getDouble(offset) else buffer.getFloat(offset).toDouble()
            require(v.isFinite()) { "Ground Truth có NaN/Infinity" }
            return v
        }
        return GroundTruth(List(shape[0]) { f -> List(17) { j -> Vec3(value(f,j,0),value(f,j,1),value(f,j,2)) } })
    }
}
