package com.example.mangaapp.ui.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Comment
import com.example.mangaapp.models.Manga
import com.example.mangaapp.repository.MangaRepository

/**
 * ViewModel cho DetailFragment.
 * Giữ toàn bộ data + trạng thái UI để không bị mất khi xoay màn hình.
 */
class DetailViewModel : ViewModel() {

    // ── Data ──────────────────────────────────────────────────────────────────
    private val _manga = MutableLiveData<Manga?>()
    val manga: LiveData<Manga?> = _manga

    private val _chapters = MutableLiveData<List<Chapter>>()
    val chapters: LiveData<List<Chapter>> = _chapters

    private val _comments = MutableLiveData<List<Comment>>()
    val comments: LiveData<List<Comment>> = _comments

    // ── UI State ──────────────────────────────────────────────────────────────
    /** true = đang load lần đầu */
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    /** Trạng thái mô tả đang expand hay collapse */
    var isDescExpanded: Boolean = false

    /** Trạng thái sort chapter */
    var isChapterReversed: Boolean = false

    /** Đánh dấu đã load xong để tránh gọi Firebase lại khi xoay */
    private var isDataLoaded = false

    // ── Load ──────────────────────────────────────────────────────────────────

    fun loadDetail(firestoreId: String) {
        // Nếu đã có data rồi → không fetch lại, giữ nguyên
        if (isDataLoaded && _manga.value != null) return

        _isLoading.value = true

        MangaRepository.getMangaById(
            firestoreId = firestoreId,
            onSuccess = { manga ->
                _manga.value = manga
                if (manga != null) {
                    loadChapters(firestoreId)
                } else {
                    _isLoading.value = false
                    _error.value = "Không tìm thấy thông tin truyện"
                }
            },
            onError = {
                _isLoading.value = false
                _error.value = "Không tải được thông tin truyện"
            }
        )
    }

    private fun loadChapters(firestoreId: String) {
        MangaRepository.getChaptersByMangaId(
            firestoreId = firestoreId,
            onSuccess = { chapters ->
                _chapters.value = chapters
                _isLoading.value = false
                isDataLoaded = true
            },
            onError = {
                _isLoading.value = false
                _error.value = "Không tải được danh sách chương"
                isDataLoaded = true
            }
        )
    }

    /** Gọi sau khi context có sẵn để load comments từ local storage */
    fun loadComments(firestoreId: String, context: android.content.Context) {
        MangaRepository.initComments(context)
        _comments.value = MangaRepository.getCommentsByFirestoreId(firestoreId)
    }

    /** Thêm comment mới vào list local */
    fun addComment(context: android.content.Context, comment: Comment, firestoreId: String) {
        MangaRepository.addComment(context, comment)
        _comments.value = MangaRepository.getCommentsByFirestoreId(firestoreId)
    }

    /** Trả về danh sách chapter theo sort hiện tại */
    fun getSortedChapters(): List<Chapter> {
        val list = _chapters.value ?: emptyList()
        return if (isChapterReversed) list.sortedBy { it.chapterNumber }
        else list.sortedByDescending { it.chapterNumber }
    }
}