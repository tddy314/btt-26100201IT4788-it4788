package com.example.human_posture

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LabState(
    val images: List<ImageInput> = emptyList(),
    val gt: List<SourceFile> = emptyList(),
    val results: List<FrameResult> = emptyList(),
    val importing: Boolean = false,
    val running: Boolean = false,
    val stopping: Boolean = false,
    val total: Int = 0,
    val notice: String = "Chọn thư mục dữ liệu để nạp ảnh và Ground Truth.",
    val manualSelection: Boolean = false,
    val selectedImages: Set<String> = emptySet(),
    val config: EvaluationConfig = EvaluationConfig(),
    val runConfig: EvaluationConfig = EvaluationConfig(),
)

class PoseViewModel(application: Application) : AndroidViewModel(application) {
    private val mutable = MutableStateFlow(LabState())
    val state = mutable.asStateFlow()
    private val resolver get() = getApplication<Application>().contentResolver
    private var work: Job? = null

    fun config(value: EvaluationConfig) { if (!state.value.running && !state.value.importing) mutable.update { it.copy(config = value) } }
    fun notice(message: String) { mutable.update { it.copy(notice = message) } }

    fun selectionMode(manual: Boolean) {
        if (!state.value.running && !state.value.importing) mutable.update { it.copy(manualSelection = manual) }
    }

    fun selectImages(keys: Set<String>) {
        if (!state.value.running && !state.value.importing) mutable.update { s ->
            s.copy(selectedImages = keys.intersect(s.images.map { it.file.uri.toString() }.toSet()))
        }
    }

    fun reset() {
        if (state.value.running || state.value.importing) return
        mutable.update { LabState(config = it.config, runConfig = it.config,
            notice = "Đã reset. Chọn thư mục để bắt đầu lần thử mới.") }
    }

    private fun retain(uri: Uri) {
        try { resolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        catch (_: SecurityException) { /* Some providers grant only temporary access. */ }
    }

    fun importTree(uri: Uri) = import {
        retain(uri)
        DataSource.prepare(DataSource.scan(resolver, uri) { count -> notice("Đang quét: $count tệp…") })
    }

    private fun import(block: suspend () -> ImportedData) {
        if (state.value.running || state.value.importing) return
        mutable.update { it.copy(importing = true, notice = "Đang đọc danh sách tệp…") }
        work = viewModelScope.launch {
            try {
                val data = withContext(Dispatchers.IO) { block() }
                mutable.update { it.copy(images = data.images, gt = data.gt, results = emptyList(), total = 0, selectedImages = emptySet(),
                    notice = "${data.images.size} ảnh gốc • ${data.gt.size} NPY. Bỏ ${data.ignored} ảnh pose/sai tên; ${data.duplicates} mục trùng frame (ưu tiên tên không hậu tố).") }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice("Không nhập được dữ liệu: ${e.message}") }
            finally { mutable.update { it.copy(importing = false) } }
        }
    }

    fun stop() { mutable.update { it.copy(stopping = true, notice = "Sẽ dừng sau ảnh đang xử lý.") } }

    fun start(count: Int?, onlyWithGt: Boolean) {
        val snapshot = state.value
        if (snapshot.running || snapshot.importing) return
        val config = snapshot.config
        try { config.validate() } catch (e: Exception) { notice(e.message.orEmpty()); return }
        val chosen = try {
            BatchSelection.choose(snapshot.images, count, snapshot.selectedImages.takeIf { snapshot.manualSelection }, onlyWithGt,
                key = { it.file.uri.toString() }, hasGt = { image ->
                    runCatching { DataSource.match(image, snapshot.gt) != null }.getOrDefault(false)
                })
        } catch (e: IllegalArgumentException) { notice(e.message.orEmpty()); return }
        mutable.update { it.copy(running = true, stopping = false, results = emptyList(), total = chosen.size,
            runConfig = config, notice = "Đang xử lý ${chosen.size} ảnh. Kết quả ${config.unit}.") }
        work = viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Bounded cache: at most eight GT clips, independent of dataset size.
                    val cache = object : LinkedHashMap<String, GroundTruth>(8, .75f, true) {
                        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, GroundTruth>?) = size > 8
                    }
                    PoseEngine(getApplication()).use { engine ->
                        for ((i, image) in chosen.withIndex()) {
                            if (state.value.stopping) break
                            var result = try { engine.process(image, i == 0) }
                            catch (e: CancellationException) { throw e }
                            catch (e: Exception) { FrameResult(image, message = "Lỗi nhận diện: ${e.message}", firstInference = i == 0) }
                            if (result.landmarks.isNotEmpty()) {
                                try {
                                    val gtFile = DataSource.match(image, snapshot.gt)
                                    if (gtFile == null) result = result.copy(message = "Đã nhận diện • thiếu Ground Truth")
                                    else {
                                        val key = gtFile.uri.toString()
                                        val gt = cache.getOrPut(key) {
                                            requireNotNull(resolver.openInputStream(gtFile.uri)) { "Không mở được NPY" }.use(NpyReader::read)
                                        }
                                        val index = config.frameOffset + image.frame.index * config.frameStride
                                        result = result.copy(gtPath = gtFile.path, gtIndex = index)
                                        require(index in gt.frames.indices) { "Frame GT $index vượt giới hạn ${gt.frames.size}" }
                                        val comparison = PoseMath.compare(JointMapping.map(result.landmarks, config.threshold), gt.frames[index], config)
                                        result = result.copy(errors = comparison.errors, comparison = comparison,
                                            message = "Đã so sánh với Ground Truth")
                                    }
                                } catch (e: CancellationException) { throw e }
                                catch (e: Exception) { result = result.copy(message = "Có pose • chưa đánh giá được: ${e.message}") }
                            }
                            val completed = result
                            mutable.update { it.copy(results = it.results + completed) }
                        }
                    }
                }
                notice(if (state.value.stopping) "Đã dừng. Có thể xuất các frame đã hoàn tất." else "Hoàn tất. Có thể xem ảnh và xuất CSV.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice("Không thể tiếp tục: ${e.message}") }
            finally { mutable.update { it.copy(running = false, stopping = false) } }
        }
    }

    fun export(uri: Uri) {
        val snapshot = state.value
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireNotNull(resolver.openOutputStream(uri, "wt")) { "Không mở được tệp xuất" }.bufferedWriter(Charsets.UTF_8).use {
                        CsvExport.write(it, snapshot.results, snapshot.total, snapshot.runConfig)
                    }
                }
                notice("Đã xuất CSV: ${snapshot.results.size} frame.")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { notice("Xuất CSV thất bại: ${e.message}") }
        }
    }
}
