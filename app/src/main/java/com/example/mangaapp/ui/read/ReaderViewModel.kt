package com.example.mangaapp.ui.read

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mangaapp.repository.MangaRepository
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ReaderViewModel : ViewModel() {

    private val _pages = MutableLiveData<List<String>>()
    val pages: LiveData<List<String>> = _pages

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // ===== 4.3 — LƯU TRẠNG THÁI QUA XOAY MÀN HÌNH =====
    // ViewModel SỐNG SÓT qua config change (xoay màn hình) nhưng KHÔNG sống sót
    // qua process death (app bị OS kill khi thiếu RAM) — slide 11, 17.
    // => Dùng để khôi phục NGAY khi xoay màn hình, không dùng để khôi phục
    //    sau khi app bị kill (việc đó là của SharedPreferences, xử lý ở Fragment).

    /** Vị trí trang đang đọc (vị trí cuộn của RecyclerView) trước khi Activity/Fragment bị tạo lại */
    var lastScrollPosition: Int = 0

    /**
     * Số chương đang đọc trước khi Fragment bị tạo lại do xoay màn hình.
     * -1 = chưa có giá trị nào được lưu (lần đầu mở Fragment).
     * Bắt buộc phải lưu ở đây vì `chapterNumber` trong Fragment chỉ được
     * khởi tạo lại từ `arguments` mỗi khi Fragment recreate — mà `arguments`
     * luôn giữ chương ban đầu lúc mở truyện (vd chương 1), không phải chương
     * người dùng đang đọc thực tế (vd chương 3) → nếu không lưu ở ViewModel,
     * xoay màn hình sẽ nhảy về đúng chương ban đầu thay vì chương đang đọc.
     */
    var currentChapterNumber: Int = -1

    /** Cỡ chữ hiện tại — giữ qua xoay màn hình thay vì để biến thường trong Fragment bị reset */
    var fontSize: Int = 16

    /**
     * Vị trí trang cần scroll tới sau khi Fragment được recreate (xoay màn hình).
     * Fragment ghi vào đây ngay sau khi ViewModel khởi tạo (onViewCreated).
     * Observer đọc một lần rồi reset về -1.
     * Dùng ViewModel thay vì Fragment flag để tránh race condition với Firestore async:
     * Fragment flag có thể bị tắt bởi observer lần 1 trước khi loadChapter() Firestore
     * callback về — ViewModel field thì không bị ảnh hưởng bởi lifecycle Fragment.
     */
    var scrollToRestore: Int = -1

    // ===== 4.4 — XỬ LÝ NỀN: PRELOAD ẢNH TRANG KẾ TIẾP =====
    // Tại sao cần Executor ở đây mà không cần ở MangaRepository/ReadingHistoryRepository?
    // -> Firestore SDK tự async hóa network I/O, nhưng việc DECODE + SCALE bitmap
    //    (Bilinear resize trong PageAdapter) là tác vụ CPU-bound thật sự.
    //    Nếu để Glide tự lo từng trang khi cuộn tới thì luôn có độ trễ hiển thị.
    //    Preload trước 1-2 trang kế tiếp bằng thread riêng giúp ảnh đã có sẵn
    //    trong cache khi người dùng cuộn tới — không làm điều này trên UI Thread
    //    vì sẽ cạnh tranh tài nguyên với việc render trang đang xem, gây giật/lag.
    private val preloadExecutor: ExecutorService = Executors.newSingleThreadExecutor()

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

    /**
     * Preload (Glide.preload) trước vài URL ảnh kế tiếp để giảm giật khi cuộn nhanh.
     * Chạy trên background thread vì decode/scale ảnh là tác vụ nặng CPU,
     * không được làm trên UI Thread (giống nguyên tắc slide 38, 43).
     *
     * @param context cần ApplicationContext (không phải Activity context) để tránh leak
     *                 khi thread chạy lâu hơn vòng đời Fragment — slide 49 "Giữ Context sai cách"
     */
    fun preloadPages(context: android.content.Context, urls: List<String>) {
        if (urls.isEmpty()) return
        val appContext = context.applicationContext
        preloadExecutor.execute {
            urls.forEach { url ->
                try {
                    com.bumptech.glide.Glide.with(appContext)
                        .load(url)
                        .preload()
                } catch (_: Exception) {
                    // Preload là tối ưu hóa, không phải tác vụ bắt buộc
                    // => lỗi ở đây không cần báo người dùng, chỉ bỏ qua
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Bắt buộc shutdown, nếu không thread pool vẫn sống sau khi ViewModel mất
        // => leak tài nguyên (giống nguyên tắc slide 44, 49: "Executor không shutdown")
        preloadExecutor.shutdown()
    }
}