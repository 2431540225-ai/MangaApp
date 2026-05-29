package com.example.mangaapp.ui.upload

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mangaapp.R
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.UserSession

/**
 * Màn hình tác giả đăng truyện mới.
 * Luồng:
 *  1. Nhập thông tin truyện (tên, mô tả, thể loại, cover URL)
 *  2. Chọn loại: Truyện tranh / Tiểu thuyết
 *  3. Chọn có thu phí chapter hay không + giá coin
 *  4. Nhập nội dung / URL trang
 *  5. Bấm Đăng → upload lên Firestore
 *
 * Lưu ý: upload ảnh thực tế cần Firebase Storage hoặc dịch vụ CDN.
 * Hiện tại nhận URL ảnh trực tiếp để đơn giản hóa cho demo.
 */
class UploadMangaFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var etTitle: EditText
    private lateinit var etAuthorName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etCoverUrl: EditText
    private lateinit var etGenres: EditText
    private lateinit var rgCategory: RadioGroup
    private lateinit var rbManga: RadioButton
    private lateinit var rbNovel: RadioButton

    // Chapter section
    private lateinit var etChapterNumber: EditText
    private lateinit var etChapterTitle: EditText
    private lateinit var switchFreeChapter: Switch
    private lateinit var layoutCoinPrice: LinearLayout
    private lateinit var etCoinPrice: EditText
    private lateinit var etPageUrls: EditText          // dành cho truyện tranh (mỗi dòng 1 URL)
    private lateinit var etNovelContent: EditText      // dành cho tiểu thuyết
    private lateinit var layoutPageUrls: LinearLayout
    private lateinit var layoutNovelContent: LinearLayout

    private lateinit var btnUpload: Button
    private lateinit var progressUpload: ProgressBar
    private lateinit var tvError: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_upload_manga, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Chỉ tác giả/admin mới vào được
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

    private fun initViews(view: View) {
        btnBack            = view.findViewById(R.id.btn_back_upload)
        etTitle            = view.findViewById(R.id.et_upload_title)
        etAuthorName       = view.findViewById(R.id.et_upload_author)
        etDescription      = view.findViewById(R.id.et_upload_description)
        etCoverUrl         = view.findViewById(R.id.et_upload_cover_url)
        etGenres           = view.findViewById(R.id.et_upload_genres)
        rgCategory         = view.findViewById(R.id.rg_upload_category)
        rbManga            = view.findViewById(R.id.rb_upload_manga)
        rbNovel            = view.findViewById(R.id.rb_upload_novel)
        etChapterNumber    = view.findViewById(R.id.et_upload_chapter_number)
        etChapterTitle     = view.findViewById(R.id.et_upload_chapter_title)
        switchFreeChapter  = view.findViewById(R.id.switch_free_chapter)
        layoutCoinPrice    = view.findViewById(R.id.layout_coin_price)
        etCoinPrice        = view.findViewById(R.id.et_coin_price)
        etPageUrls         = view.findViewById(R.id.et_page_urls)
        etNovelContent     = view.findViewById(R.id.et_novel_content)
        layoutPageUrls     = view.findViewById(R.id.layout_page_urls)
        layoutNovelContent = view.findViewById(R.id.layout_novel_content)
        btnUpload          = view.findViewById(R.id.btn_upload_submit)
        progressUpload     = view.findViewById(R.id.progress_upload)
        tvError            = view.findViewById(R.id.tv_upload_error)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupListeners() {
        // Đổi loại truyện → đổi input nội dung
        rgCategory.setOnCheckedChangeListener { _, checkedId ->
            val isManga = checkedId == R.id.rb_upload_manga
            layoutPageUrls.visibility    = if (isManga)  View.VISIBLE else View.GONE
            layoutNovelContent.visibility = if (!isManga) View.VISIBLE else View.GONE
        }

        // Switch miễn phí / trả phí
        switchFreeChapter.setOnCheckedChangeListener { _, isChecked ->
            layoutCoinPrice.visibility = if (isChecked) View.GONE else View.VISIBLE
            switchFreeChapter.text     = if (isChecked) "Chương miễn phí" else "Chương trả phí"
        }

        btnUpload.setOnClickListener { validateAndUpload() }
    }

    private fun validateAndUpload() {
        val title       = etTitle.text.toString().trim()
        val authorName  = etAuthorName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val coverUrl    = etCoverUrl.text.toString().trim()
        val genresRaw   = etGenres.text.toString().trim()
        val isManga     = rbManga.isChecked
        val isFree      = switchFreeChapter.isChecked
        val coinPrice   = etCoinPrice.text.toString().toIntOrNull() ?: 0
        val chapterNum  = etChapterNumber.text.toString().toIntOrNull()
        val chapterTitle = etChapterTitle.text.toString().trim()

        // Validation
        when {
            title.isEmpty()       -> { showError("Vui lòng nhập tên truyện"); return }
            authorName.isEmpty()  -> { showError("Vui lòng nhập tên tác giả"); return }
            description.isEmpty() -> { showError("Vui lòng nhập mô tả"); return }
            coverUrl.isEmpty()    -> { showError("Vui lòng nhập URL ảnh bìa"); return }
            chapterNum == null    -> { showError("Số chương không hợp lệ"); return }
            !isFree && coinPrice <= 0 -> { showError("Vui lòng nhập giá coin hợp lệ (> 0)"); return }
        }

        val genres = genresRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val category = if (isManga) MangaCategory.TRUYEN_TRANH else MangaCategory.TIEU_THUYET

        // Nội dung chapter
        val pageUrls = if (isManga) {
            etPageUrls.text.toString().trim()
                .split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        val novelContent = if (!isManga) etNovelContent.text.toString().trim() else ""

        if (isManga && pageUrls.isEmpty()) {
            showError("Vui lòng nhập ít nhất 1 URL trang ảnh")
            return
        }
        if (!isManga && novelContent.isEmpty()) {
            showError("Vui lòng nhập nội dung chương")
            return
        }

        setLoading(true)

        // Bước 1: Tạo truyện → Bước 2: Đăng chapter đầu tiên
        MangaRepository.uploadManga(
            title       = title,
            author      = authorName,
            description = description,
            coverUrl    = coverUrl,
            genres      = genres,
            category    = category,
            onSuccess = { storyId ->
                MangaRepository.uploadChapter(
                    storyFirestoreId = storyId,
                    chapterNumber    = chapterNum!!,
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
                            "Đăng truyện thành công! ✅",
                            Toast.LENGTH_LONG
                        ).show()
                        parentFragmentManager.popBackStack()
                    },
                    onError = { e ->
                        if (!isAdded) return@uploadChapter
                        setLoading(false)
                        showError("Đăng chapter thất bại: ${e.message}")
                    }
                )
            },
            onError = { e ->
                if (!isAdded) return@uploadManga
                setLoading(false)
                showError("Đăng truyện thất bại: ${e.message}")
            }
        )
    }

    private fun setLoading(loading: Boolean) {
        progressUpload.visibility = if (loading) View.VISIBLE else View.GONE
        btnUpload.isEnabled       = !loading
        btnUpload.text            = if (loading) "Đang đăng..." else "Đăng truyện"
    }

    private fun showError(msg: String) {
        tvError.text       = "⚠️ $msg"
        tvError.visibility = View.VISIBLE
    }
}