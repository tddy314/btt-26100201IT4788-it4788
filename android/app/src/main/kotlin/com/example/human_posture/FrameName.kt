package com.example.human_posture

data class FrameName(val clip: String, val second: Int, val withinSecond: Int, val duplicate: Boolean) {
    val index get() = (second - 1) * 5 + withinSecond - 1
    val gtName get() = "framed_${clip}_h36m.npy"
    val key get() = "$clip:$second:$withinSecond"
    companion object {
        fun canonicalGtName(name: String) = name.replace(Regex("(?:\\(\\d+\\))+(?=\\.npy$)", RegexOption.IGNORE_CASE), "")
        private val pattern = Regex("^(.+)_(\\d+)_(\\d+)((?:\\(\\d+\\))*)\\.jpg$", RegexOption.IGNORE_CASE)
        fun parse(name: String): FrameName? {
            if (name.startsWith("pose_", ignoreCase = true)) return null
            val m = pattern.matchEntire(name) ?: return null
            val y = m.groupValues[2].toIntOrNull() ?: return null
            val z = m.groupValues[3].toIntOrNull() ?: return null
            if (y !in 1..20 || z !in 1..5) return null
            return FrameName(m.groupValues[1], y, z, m.groupValues[4].isNotEmpty())
        }
    }
}
