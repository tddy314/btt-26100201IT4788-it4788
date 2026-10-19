package com.example.human_posture

/** Null manual keys = automatic; an empty set = manual mode with nothing selected. */
object BatchSelection {
    fun <T> choose(images: List<T>, count: Int?, manualKeys: Set<String>?, onlyWithGt: Boolean,
        key: (T) -> String, hasGt: (T) -> Boolean): List<T> {
        if (manualKeys == null && count != null)
            require(count > 0 && count % 5 == 0) { "Số ảnh phải là bội số dương của 5." }
        val available = if (manualKeys == null) images else images.filter { key(it) in manualKeys }
        require(manualKeys == null || available.size == manualKeys.size) { "Lựa chọn chứa ảnh không còn trong thư mục. Hãy chọn lại." }
        val candidates = if (onlyWithGt) available.filter(hasGt) else available
        require(manualKeys == null || candidates.size == available.size) {
            "Có ảnh đã chọn thiếu hoặc không ghép được GT. Bỏ chọn các ảnh đó hoặc tắt bộ lọc chỉ ảnh có GT."
        }
        require(manualKeys != null || count == null || candidates.size >= count) {
            "Chỉ có ${candidates.size} ảnh phù hợp; hãy giảm số ảnh."
        }
        val chosen = if (manualKeys != null || count == null) candidates else candidates.take(count)
        require(chosen.isNotEmpty() && chosen.size % 5 == 0) {
            "Số ảnh phải lớn hơn 0 và chia hết cho 5 (hiện ${chosen.size})."
        }
        return chosen
    }
}
