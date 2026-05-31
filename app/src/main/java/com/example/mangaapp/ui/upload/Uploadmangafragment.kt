package com.example.mangaapp.ui.upload

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.UserSession
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest

class UploadMangaFragment : Fragment() {

    // ─── Cloudinary config (giống hệt admin) ─────────────────────────────────
    private val CLOUD_NAME = "dlpudlgec"
    private val API_KEY    = "758624964324127"
    private val API_SECRET = "jWxCLsCM4U0i2TnQMFEjpOH-B_c"

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageButton
    private lateinit var etTitle: EditText
    private lateinit var etAuthorName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etGenres: EditText
    private lateinit var rgCategory: RadioGroup
    private lateinit var rbManga: RadioButton
    private lateinit var rbNovel: RadioButton

    private lateinit var ivCoverPreview: ImageView
    private lateinit var btnPickCover: Button
    private lateinit var tvCoverStatus: TextView

    private lateinit var etChapterNumber: EditText
    private lateinit var etChapterTitle: EditText
    private lateinit var switchFreeChapter: Switch
    private lateinit var layoutCoinPrice: LinearLayout
    private lateinit var etCoinPrice: EditText

    private lateinit var layoutPagePicker: LinearLayout
    private lateinit var btnPickPages: Button
    private lateinit var tvPagesStatus: TextView

    private lateinit var layoutNovelContent: LinearLayout
    private lateinit var etNovelContent: EditText

    private lateinit var btnUpload: Button
    private lateinit var progressUpload: ProgressBar
    private lateinit var tvProgressDetail: TextView
    private lateinit var tvError: TextView

    // ─── State ───────────────────────────────────────────────────────────────
    private var coverImageUri: Uri? = null
    private var pageImageUris: List<Uri> = emptyList()

    private val httpClient = OkHttpClient()

    // ─── Image pickers ───────────────────────────────────────────────────────

    // Photo Picker API — hỗ trợ multi-select đúng trên cả emulator lẫn máy thật
    private val pickCoverLauncher = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            coverImageUri = uri
            tvCoverStatus.text = "✅ Đã chọn ảnh bìa"
            tvCoverStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            Glide.with(this).load(uri).centerCrop().into(ivCoverPreview)
            ivCoverPreview.visibility = View.VISIBLE
        }
    }

    private val pickPagesLauncher = registerForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia()
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
        rgCategory.setOnCheckedChangeListener { _, checkedId ->
            val isManga = checkedId == R.id.rb_upload_manga
            layoutPagePicker.visibility   = if (isManga) View.VISIBLE else View.GONE
            layoutNovelContent.visibility = if (!isManga) View.VISIBLE else View.GONE
        }

        switchFreeChapter.setOnCheckedChangeListener { _, isChecked ->
            layoutCoinPrice.visibility = if (isChecked) View.GONE else View.VISIBLE
            switchFreeChapter.text     = if (isChecked) "Chương miễn phí" else "Chương trả phí"
        }

        btnPickCover.setOnClickListener {
            pickCoverLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }
        btnPickPages.setOnClickListener {
            pickPagesLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
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
            title.isEmpty()                    -> { showError("Vui lòng nhập tên truyện"); return }
            authorName.isEmpty()               -> { showError("Vui lòng nhập tên tác giả"); return }
            description.isEmpty()              -> { showError("Vui lòng nhập mô tả"); return }
            coverImageUri == null              -> { showError("Vui lòng chọn ảnh bìa"); return }
            chapterNum == null                 -> { showError("Số chương không hợp lệ"); return }
            !isFree && coinPrice <= 0          -> { showError("Vui lòng nhập giá coin hợp lệ (> 0)"); return }
            isManga && pageImageUris.isEmpty() -> { showError("Vui lòng chọn ít nhất 1 ảnh trang"); return }
            !isManga && novelContent.isEmpty() -> { showError("Vui lòng nhập nội dung chương"); return }
        }

        val genres   = genresRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val category = if (isManga) MangaCategory.TRUYEN_TRANH else MangaCategory.TIEU_THUYET

        // Tạo slug từ tên truyện (giống admin dùng làm folder/publicId)
        val slug = generateSlug(title)

        setLoading(true)

        // Bước 1: Upload ảnh bìa lên Cloudinary
        updateProgress("Đang upload ảnh bìa...")
        uploadToCloudinary(
            uri      = coverImageUri!!,
            folder   = "manga/$slug",
            publicId = "${slug}_cover",
            onSuccess = { coverUrl ->
                if (!isAdded) return@uploadToCloudinary

                if (isManga && pageImageUris.isNotEmpty()) {
                    // Bước 2a: Upload ảnh trang tuần tự
                    uploadPageImages(pageImageUris, slug, chapterNum!!) { pageUrls ->
                        if (!isAdded) return@uploadPageImages
                        createStory(
                            title, authorName, description, coverUrl, genres, category,
                            chapterNum, chapterTitle, novelContent, pageUrls, isFree, coinPrice
                        )
                    }
                } else {
                    // Bước 2b: Tiểu thuyết — không upload ảnh trang
                    createStory(
                        title, authorName, description, coverUrl, genres, category,
                        chapterNum!!, chapterTitle, novelContent, emptyList(), isFree, coinPrice
                    )
                }
            },
            onError = { e ->
                if (!isAdded) return@uploadToCloudinary
                setLoading(false)
                showError("Upload ảnh bìa thất bại: ${e.message}")
            }
        )
    }

    // ─── Upload ảnh trang tuần tự ─────────────────────────────────────────────

    private fun uploadPageImages(
        uris: List<Uri>,
        slug: String,
        chapterNum: Int,
        onDone: (List<String>) -> Unit
    ) {
        val urls = mutableListOf<String>()
        var index = 0

        fun uploadNext() {
            if (index >= uris.size) { onDone(urls); return }
            updateProgress("Đang upload trang ${index + 1}/${uris.size}...")
            uploadToCloudinary(
                uri      = uris[index],
                folder   = "manga/$slug/chapter_$chapterNum",
                publicId = "page_${index + 1}",
                onSuccess = { url ->
                    urls.add(url)
                    index++
                    uploadNext()
                },
                onError = { e ->
                    if (!isAdded) return@uploadToCloudinary
                    setLoading(false)
                    showError("Upload trang ${index + 1} thất bại: ${e.message}")
                }
            )
        }
        uploadNext()
    }

    // ─── Cloudinary Signed Upload (giống hệt admin) ───────────────────────────
    //
    // Quy trình thực tế của Cloudinary Signed Upload:
    //   1. Tạo timestamp (Unix seconds)
    //   2. Tạo chuỗi params cần ký: "folder=...&public_id=...&timestamp=..."
    //      (sắp xếp theo alphabet, KHÔNG bao gồm api_key và file)
    //   3. Nối thêm API Secret vào cuối chuỗi params
    //   4. Hash SHA-1 → signature (hex string)
    //   5. Gửi multipart POST lên https://api.cloudinary.com/v1_1/{cloud_name}/image/upload
    //      với các field: file, api_key, timestamp, signature, folder, public_id
    //   6. Response JSON chứa secure_url → dùng làm URL lưu vào Firestore

    private fun uploadToCloudinary(
        uri: Uri,
        folder: String,
        publicId: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        // Đọc bytes từ ContentResolver (chạy trên main thread là an toàn)
        val bytes: ByteArray = try {
            requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Không thể đọc file ảnh")
        } catch (e: Exception) {
            onError(e)
            return
        }

        // Tạo timestamp và signature (SHA-1, giống admin)
        val timestamp = (System.currentTimeMillis() / 1000).toString()
        // Params phải sắp xếp theo alphabet
        val paramsToSign = "folder=$folder&public_id=$publicId&timestamp=$timestamp"
        val signature   = sha1Hex(paramsToSign + API_SECRET)

        // Build multipart request
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file", "image.jpg",
                bytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .addFormDataPart("api_key",   API_KEY)
            .addFormDataPart("timestamp", timestamp)
            .addFormDataPart("signature", signature)
            .addFormDataPart("folder",    folder)
            .addFormDataPart("public_id", publicId)
            .build()

        val request = Request.Builder()
            .url("https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload")
            .post(requestBody)
            .build()

        // OkHttp chạy trên background thread tự động
        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread { onError(e) }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string() ?: ""
                activity?.runOnUiThread {
                    if (!response.isSuccessful) {
                        val msg = try { JSONObject(body).optString("error", body) } catch (_: Exception) { body }
                        onError(Exception("Cloudinary lỗi ${response.code}: $msg"))
                    } else {
                        val secureUrl = try { JSONObject(body).getString("secure_url") }
                        catch (e: Exception) { onError(e); return@runOnUiThread }
                        onSuccess(secureUrl)
                    }
                }
            }
        })
    }

    /**
     * SHA-1 hash → hex string (giống generateSHA1 trong admin)
     */
    private fun sha1Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Tạo slug giống admin (dùng làm folder name trên Cloudinary)
     */
    private fun generateSlug(title: String): String {
        val map = mapOf(
            'à' to 'a', 'á' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
            'ă' to 'a', 'ắ' to 'a', 'ặ' to 'a', 'ằ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a',
            'â' to 'a', 'ấ' to 'a', 'ầ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a', 'ậ' to 'a',
            'è' to 'e', 'é' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
            'ê' to 'e', 'ế' to 'e', 'ề' to 'e', 'ể' to 'e', 'ễ' to 'e', 'ệ' to 'e',
            'ì' to 'i', 'í' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
            'ò' to 'o', 'ó' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
            'ô' to 'o', 'ố' to 'o', 'ồ' to 'o', 'ổ' to 'o', 'ỗ' to 'o', 'ộ' to 'o',
            'ơ' to 'o', 'ớ' to 'o', 'ờ' to 'o', 'ở' to 'o', 'ỡ' to 'o', 'ợ' to 'o',
            'ù' to 'u', 'ú' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
            'ư' to 'u', 'ứ' to 'u', 'ừ' to 'u', 'ử' to 'u', 'ữ' to 'u', 'ự' to 'u',
            'ỳ' to 'y', 'ý' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',
            'đ' to 'd'
        )
        return title.lowercase()
            .map { map[it] ?: it }
            .joinToString("")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(80)
    }

    // ─── Tạo truyện + chapter trên Firestore ─────────────────────────────────

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