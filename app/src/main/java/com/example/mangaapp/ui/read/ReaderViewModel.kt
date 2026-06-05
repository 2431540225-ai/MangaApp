package com.example.mangaapp.ui.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mangaapp.repository.MangaRepository

class ReaderViewModel : ViewModel() {

    private val _pages = MutableLiveData<List<String>>()
    val pages: LiveData<List<String>> = _pages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadChapterFromFirestore(storyId: String, chapterId: String) {
        _isLoading.value = true
        _error.value = null

        MangaRepository.getChapterPagesFromFirestore(
            storyId = storyId,
            chapterId = chapterId,
            onSuccess = { pageUrls ->
                _pages.postValue(pageUrls)
                _isLoading.postValue(false)
            },
            onError = { exception ->
                _error.postValue(exception.message)
                _isLoading.postValue(false)
            }
        )
    }
}