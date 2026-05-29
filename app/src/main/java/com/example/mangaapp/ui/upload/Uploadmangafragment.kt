package com.example.mangaapp.ui.upload

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.UserSession
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class UploadMangaFragment : Fragment() {

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageButton
    private lateinit var etTitle: EditText
    private lateinit var etAuthorName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etGenres: EditText
    private lateinit var rgCategory: RadioGroup
    private lateinit var rbManga: RadioButton
    private lateinit var rbNovel: RadioButton

    // Cover image
    private lateinit var ivCoverPreview: ImageView
    private lateinit var btnPickCover: Button
    private lateinit var tvCoverStatus: TextView

    // Chapter
    private lateinit var etChapterNumber: EditText
    private lateinit var etChapterTitle: EditText
    private lateinit var switchFreeChapter: Switch
    private lateinit var layoutCoinPrice: LinearLayout
    private lateinit var etCoinPrice: EditText

    // Pages (manga)
    private lateinit var layoutPagePicker: LinearLayout
    private lateinit var btnPickPages: Button
    private lateinit var tvPagesStatus: TextView

    // Novel content
    private lateinit var layoutNovelContent: LinearLayout
    private lateinit var etNovelContent: EditText

    // Submit
    private lateinit var btnUpload: Button
    private lateinit var progressUpload: ProgressBar
    private lateinit var tvProgressDetail: TextView
    private lateinit var tvError: TextView

    // ─── State ───────────────────────────────────────────────────────────────
    private var coverImageUri: Uri? = null
    private var pageImageUris: List<Uri> = emptyList()
    private val storage = FirebaseStorage.getInstance()

    // ─── Image pickers — dùng GetContent / GetMultipleContents ───────────────

    /** Chọn 1 ảnh bìa — hoạt động đúng trên cả máy thật lẫn emulator */
    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coverImageUri = uri
            tvCoverStatus.text = "✅ Đã chọn ảnh bìa"
            tvCoverStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            Glide.with(this).load(uri).centerCrop().into(ivCoverPreview)
            ivCoverPreview.visibility = View.VISIBLE
        }
    }

    /** Chọn nhiều ảnh trang (truyện tranh) — hoạt động đúng trên cả máy thật lẫn emulator */
    private val pickPagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pageImageUris = uris
            tvPagesStatus.text = "✅ Đã chọn ${uris.size} trang"
            tvPagesStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
        }
    }

    // ─── Lifecycle ───────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_upload_manga, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!UserSession.isLoggedIn || UserSession.currentUser?.isAuthor == false) {
            Toast.makeText(requireContext(), "Bạn không có quyền đăng truyện", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        initViews(view)
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.example.mangaapp.MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? com.example.mangaapp.MainActivity)?.showBottomNav()
    }

    // ─── Init ────────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        btnBack            = view.findViewById(R.id.btn_back_upload)
        etTitle            = view.findViewById(R.id.et_upload_title)
        etAuthorName       = view.findViewById(R.id.et_upload_author)
        etDescription      = view.findViewById(R.id.et_upload_description)
        etGenres           = view.findViewById(R.id.et_upload_genres)
        rgCategory         = view.findViewById(R.id.rg_upload_category)
        rbManga            = view.findViewById(R.id.rb_upload_manga)
        rbNovel            = view.findViewById(R.id.rb_upload_novel)

        ivCoverPreview     = view.findViewById(R.id.iv_cover_preview)
        btnPickCover       = view.findViewById(R.id.btn_pick_cover)
        tvCoverStatus      = view.findViewById(R.id.tv_cover_status)

        etChapterNumber    = view.findViewById(R.id.et_upload_chapter_number)
        etChapterTitle     = view.findViewById(R.id.et_upload_chapter_title)
        switchFreeChapter  = view.findViewById(R.id.switch_free_chapter)
        layoutCoinPrice    = view.findViewById(R.id.layout_coin_price)
        etCoinPrice        = view.findViewById(R.id.et_coin_price)

        layoutPagePicker   = view.findViewById(R.id.layout_page_picker)
        btnPickPages       = view.findViewById(R.id.btn_pick_pages)
        tvPagesStatus      = view.findViewById(R.id.tv_pages_status)

        layoutNovelContent = view.findViewById(R.id.layout_novel_content)
        etNovelContent     = view.findViewById(R.id.et_novel_content)

        btnUpload          = view.findViewById(R.id.btn_upload_submit)
        progressUpload     = view.findViewById(R.id.progress_upload)
        tvProgressDetail   = view.findViewById(R.id.tv_progress_detail)
        tvError            = view.findViewById(R.id.tv_upload_error)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupListeners() {
        // Đổi loại truyện → hiện input phù hợp
        rgCategory.setOnCheckedChangeListener { _, checkedId ->
            val isManga = checkedId == R.id.rb_upload_manga
            layoutPagePicker.visibility   = if (isManga) View.VISIBLE else View.GONE
            layoutNovelContent.visibility = if (!isManga) View.VISIBLE else View.GONE
        }

        // Switch miễn phí / trả phí
        switchFreeChapter.setOnCheckedChangeListener { _, isChecked ->
            layoutCoinPrice.visibility = if (isChecked) View.GONE else View.VISIBLE
            switchFreeChapter.text     = if (isChecked) "Chương miễn phí" else "Chương trả phí"
        }

        // Chọn ảnh bìa — dùng GetContent("image/*")
        btnPickCover.setOnClickListener {
            pickCoverLauncher.launch("image/*")
        }

        // Chọn ảnh trang (nhiều) — dùng GetMultipleContents("image/*")
        btnPickPages.setOnClickListener {
            pickPagesLauncher.launch("image/*")
        }

        btnUpload.setOnClickListener { validateAndUpload() }
    }

    // ─── Validate & Upload ───────────────────────────────────────────────────

    private fun validateAndUpload() {
        tvError.visibility = View.GONE
        val title        = etTitle.text.toString().trim()
        val authorName   = etAuthorName.text.toString().trim()
        val description  = etDescription.text.toString().trim()
        val genresRaw    = etGenres.text.toString().trim()
        val isManga      = rbManga.isChecked
        val isFree       = switchFreeChapter.isChecked
        val coinPrice    = etCoinPrice.text.toString().toIntOrNull() ?: 0
        val chapterNum   = etChapterNumber.text.toString().toIntOrNull()
        val chapterTitle = etChapterTitle.text.toString().trim()
        val novelContent = if (!isManga) etNovelContent.text.toString().trim() else ""

        when {
            title.isEmpty()              -> { showError("Vui lòng nhập tên truyện"); return }
            authorName.isEmpty()         -> { showError("Vui lòng nhập tên tác giả"); return }
            description.isEmpty()        -> { showError("Vui lòng nhập mô tả"); return }
            coverImageUri == null        -> { showError("Vui lòng chọn ảnh bìa"); return }
            chapterNum == null           -> { showError("Số chương không hợp lệ"); return }
            !isFree && coinPrice <= 0    -> { showError("Vui lòng nhập giá coin hợp lệ (> 0)"); return }
            isManga && pageImageUris.isEmpty() -> { showError("Vui lòng chọn ít nhất 1 ảnh trang"); return }
            !isManga && novelContent.isEmpty() -> { showError("Vui lòng nhập nội dung chương"); return }
        }

        val genres   = genresRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val category = if (isManga) MangaCategory.TRUYEN_TRANH else MangaCategory.TIEU_THUYET

        setLoading(true)

        // Bước 1: Upload ảnh bìa lên Storage
        updateProgress("Đang upload ảnh bìa...")
        uploadImageToStorage(
            uri      = coverImageUri!!,
            path     = "covers/${UUID.randomUUID()}.jpg",
            onSuccess = { coverUrl ->
                if (!isAdded) return@uploadImageToStorage

                if (isManga && pageImageUris.isNotEmpty()) {
                    // Bước 2a: Upload tất cả ảnh trang
                    uploadPageImages(pageImageUris, chapterNum!!) { pageUrls ->
                        if (!isAdded) return@uploadPageImages
                        // Bước 3: Tạo truyện + chapter
                        createStory(
                            title, authorName, description, coverUrl, genres, category,
                            chapterNum, chapterTitle, novelContent, pageUrls,
                            isFree, coinPrice
                        )
                    }
                } else {
                    // Bước 2b: Tiểu thuyết, không cần upload ảnh trang
                    createStory(
                        title, authorName, description, coverUrl, genres, category,
                        chapterNum!!, chapterTitle, novelContent, emptyList(),
                        isFree, coinPrice
                    )
                }
            },
            onError = { e ->
                if (!isAdded) return@uploadImageToStorage
                setLoading(false)
                showError("Upload ảnh bìa thất bại: ${e.message}")
            }
        )
    }

    /**
     * Upload từng ảnh trang theo thứ tự, gom URL vào list rồi callback.
     */
    private fun uploadPageImages(
        uris: List<Uri>,
        chapterNum: Int,
        onDone: (List<String>) -> Unit
    ) {
        val urls = mutableListOf<String>()
        var index = 0

        fun uploadNext() {
            if (index >= uris.size) { onDone(urls); return }
            updateProgress("Đang upload trang ${index + 1}/${uris.size}...")
            uploadImageToStorage(
                uri     = uris[index],
                path    = "pages/chapter_$chapterNum/${UUID.randomUUID()}.jpg",
                onSuccess = { url ->
                    urls.add(url)
                    index++
                    uploadNext()
                },
                onError = { e ->
                    if (!isAdded) return@uploadImageToStorage
                    setLoading(false)
                    showError("Upload trang ${index + 1} thất bại: ${e.message}")
                }
            )
        }
        uploadNext()
    }

    /**
     * Upload một ảnh lên Firebase Storage và trả về download URL.
     */
    private fun uploadImageToStorage(
        uri: Uri,
        path: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val ref = storage.reference.child(path)
        ref.putFile(uri)
            .continueWithTask { task ->
                if (!task.isSuccessful) throw task.exception!!
                ref.downloadUrl
            }
            .addOnSuccessListener { downloadUri -> onSuccess(downloadUri.toString()) }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Tạo document truyện (slug làm ID) + chapter đầu tiên trên Firestore.
     */
    private fun createStory(
        title: String, author: String, description: String,
        coverUrl: String, genres: List<String>, category: MangaCategory,
        chapterNum: Int, chapterTitle: String, novelContent: String,
        pageUrls: List<String>, isFree: Boolean, coinPrice: Int
    ) {
        updateProgress("Đang lưu thông tin truyện...")
        MangaRepository.uploadManga(
            title       = title,
            author      = author,
            description = description,
            coverUrl    = coverUrl,
            genres      = genres,
            category    = category,
            onSuccess   = { storyId ->
                updateProgress("Đang lưu chapter...")
                MangaRepository.uploadChapter(
                    storyFirestoreId = storyId,
                    chapterNumber    = chapterNum,
                    title            = chapterTitle,
                    contentText      = novelContent,
                    pageUrls         = pageUrls,
                    isFree           = isFree,
                    coinPrice        = coinPrice,
                    onSuccess = {
                        if (!isAdded) return@uploadChapter
                        setLoading(false)
                        Toast.makeText(
                            requireContext(),
                            "Đăng truyện thành công! ✅  (ID: $storyId)",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    },
                    onError = { e ->
                        if (!isAdded) return@uploadChapter
                        setLoading(false)
                        showError("Lưu chapter thất bại: ${e.message}")
                    }
                )
            },
            onError = { e ->
                if (!isAdded) return@uploadManga
                setLoading(false)
                showError("Lưu truyện thất bại: ${e.message}")
            }
        )
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        progressUpload.visibility   = if (loading) View.VISIBLE else View.GONE
        tvProgressDetail.visibility = if (loading) View.VISIBLE else View.GONE
        btnUpload.isEnabled         = !loading
        btnUpload.text              = if (loading) "Đang đăng..." else "Đăng truyện"
        btnPickCover.isEnabled      = !loading
        btnPickPages.isEnabled      = !loading
    }

    private fun updateProgress(msg: String) {
        if (isAdded) tvProgressDetail.text = msg
    }

    private fun showError(msg: String) {
        tvError.text       = "⚠️ $msg"
        tvError.visibility = View.VISIBLE
    }
}