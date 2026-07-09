package com.example.mangaapp.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mangaapp.models.Manga
import com.example.mangaapp.repository.MangaRepository

/**
 * ViewModel cho HomeFragment.
 *  - Giữ dữ liệu đã tải từ Firebase trong bộ nhớ
 *  - Khi xoay màn hình / chuyển app rồi quay lại → Fragment tự hủy
 *    nhưng ViewModel vẫn còn sống → bind lại UI mà KHÔNG gọi Firebase lại
 * Vòng đời:
 *  Fragment bị recreate (xoay màn) → ViewModel vẫn giữ data
 *  Activity bị destroy hẳn (thoát app) → ViewModel.onCleared() mới bị gọi
 */
class HomeViewModel : ViewModel() {

    // ── Featured manga (Banner) ────
    private val _featuredList = MutableLiveData<List<Manga>>()
    val featuredList: LiveData<List<Manga>> = _featuredList

    // ── Latest manga ───
    private val _latestList = MutableLiveData<List<Manga>>()
    val latestList: LiveData<List<Manga>> = _latestList

    // ── Ranking manga ────
    private val _rankingList = MutableLiveData<List<Manga>>()
    val rankingList: LiveData<List<Manga>> = _rankingList

    // ── Loading / Error state ────
    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    /**
     * Kiểm tra xem data đã được tải lần đầu chưa.
     * Nếu rồi thì không gọi Firebase lại (tránh gọi thừa khi xoay màn hình).
     */
    val isDataLoaded: Boolean
        get() = _featuredList.value != null &&
                _latestList.value  != null &&
                _rankingList.value != null
    // ── Load functions ────
    /**
     * Load toàn bộ data cho Home.
     * Chỉ thực sự gọi Firebase nếu chưa có data (lần đầu mở màn hình).
     * Xoay màn hình → hàm này được gọi lại nhưng sẽ return sớm vì isDataLoaded = true.
     */
    fun loadHomeData() {
        // Đã có data rồi → không gọi lại Firebase
        if (isDataLoaded) return

        _isLoading.value = true
        _error.value = null

        loadFeatured()
        loadLatest()
        loadRanking()
    }

    private fun loadFeatured() {
        MangaRepository.getFeaturedManga(
            onSuccess = { list ->
                _featuredList.value = list
                checkLoadingComplete()
            },
            onError = { e ->
                _error.value = "Không tải được banner: ${e.message}"
                checkLoadingComplete()
            }
        )
    }

    private fun loadLatest() {
        MangaRepository.getLatestManga(
            onSuccess = { list ->
                _latestList.value = list
                checkLoadingComplete()
            },
            onError = {
                _latestList.value = emptyList()
                checkLoadingComplete()
            }
        )
    }

    private fun loadRanking() {
        MangaRepository.getRankingManga(
            onSuccess = { list ->
                _rankingList.value = list
                checkLoadingComplete()
            },
            onError = {
                _rankingList.value = emptyList()
                checkLoadingComplete()
            }
        )
    }

    /**
     * Tắt loading khi cả 3 request đã xong (dù thành công hay lỗi).
     */
    private fun checkLoadingComplete() {
        if (_featuredList.value != null &&
            _latestList.value  != null &&
            _rankingList.value != null
        ) {
            _isLoading.value = false
        }
    }

    /**
     * Buộc load lại data từ Firebase (dùng khi user kéo refresh).
     */
    fun refresh() {
        _featuredList.value = null
        _latestList.value   = null
        _rankingList.value  = null
        loadHomeData()
    }
}