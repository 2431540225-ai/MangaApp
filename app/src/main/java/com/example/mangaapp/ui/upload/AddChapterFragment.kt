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
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.UserSession
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.security.MessageDigest

class AddChapterFragment : Fragment() {

    // ─── Cloudinary config ────────────────────────────────────────────────────
    private val CLOUD_NAME = "dlpudlgec"
    private val API_KEY    = "758624964324127"
    private val API_SECRET = "jWxCLsCM4U0i2TnQMFEjpOH-B_c"

    // ─── Views ───────────────────────────────────────────────────────────────
    private lateinit var btnBack: ImageButton
    private lateinit var progressLoadingStories: ProgressBar
    private lateinit var spinnerStory: Spinner
    private lateinit var layoutSelectedStoryInfo: LinearLayout
    private lateinit var ivSelectedStoryCover: ImageView
    private lateinit var tvSelectedStoryTitle: TextView
    private lateinit var tvSelectedStoryChapters: TextView
    private lateinit var tvSelectedStoryCategory: TextView
    private lateinit var dividerChapterSection: View
    private lateinit var layoutChapterForm: LinearLayout

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

    private lateinit var tvError: TextView
    private lateinit var progressAddChapter: ProgressBar
    private lateinit var tvProgressDetail: TextView
    private lateinit var btnSubmitChapter: Button

    // ─── State ───────────────────────────────────────────────────────────────
    private var authorStories: List<Manga> = emptyList()
    private var selectedStory: Manga? = null
    private var pageImageUris: List<Uri> = emptyList()
    private val httpClient = OkHttpClient()

    // ─── Multi-image picker ───────────────────────────────────────────────────
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
    ): View? = inflater.inflate(R.layout.fragment_add_chapter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!UserSession.isLoggedIn || UserSession.currentUser?.isAuthor == false) {
            Toast.makeText(requireContext(), "Bạn không có quyền thêm chapter", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
            return
        }

        initViews(view)
        setupListeners()
        loadAuthorStories()
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
        btnBack                 = view.findViewById(R.id.btn_back_add_chapter)
        progressLoadingStories  = view.findViewById(R.id.progress_loading_stories)
        spinnerStory            = view.findViewById(R.id.spinner_story)
        layoutSelectedStoryInfo = view.findViewById(R.id.layout_selected_story_info)
        ivSelectedStoryCover    = view.findViewById(R.id.iv_selected_story_cover)
        tvSelectedStoryTitle    = view.findViewById(R.id.tv_selected_story_title)
        tvSelectedStoryChapters = view.findViewById(R.id.tv_selected_story_chapters)
        tvSelectedStoryCategory = view.findViewById(R.id.tv_selected_story_category)
        dividerChapterSection   = view.findViewById(R.id.divider_chapter_section)
        layoutChapterForm       = view.findViewById(R.id.layout_chapter_form)

        etChapterNumber    = view.findViewById(R.id.et_chapter_number)
        etChapterTitle     = view.findViewById(R.id.et_chapter_title)
        switchFreeChapter  = view.findViewById(R.id.switch_free_chapter)
        layoutCoinPrice    = view.findViewById(R.id.layout_coin_price)
        etCoinPrice        = view.findViewById(R.id.et_coin_price)

        layoutPagePicker   = view.findViewById(R.id.layout_page_picker)
        btnPickPages       = view.findViewById(R.id.btn_pick_pages)
        tvPagesStatus      = view.findViewById(R.id.tv_pages_status)

        layoutNovelContent = view.findViewById(R.id.layout_novel_content)
        etNovelContent     = view.findViewById(R.id.et_novel_content)

        tvError            = view.findViewById(R.id.tv_add_chapter_error)
        progressAddChapter = view.findViewById(R.id.progress_add_chapter)
        tvProgressDetail   = view.findViewById(R.id.tv_progress_detail)
        btnSubmitChapter   = view.findViewById(R.id.btn_submit_chapter)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupListeners() {
        switchFreeChapter.setOnCheckedChangeListener { _, isChecked ->
            layoutCoinPrice.visibility = if (isChecked) View.GONE else View.VISIBLE
            switchFreeChapter.text     = if (isChecked) "Chương miễn phí" else "Chương trả phí"
        }

        btnPickPages.setOnClickListener {
            pickPagesLauncher.launch(
                androidx.activity.result.PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        }

        btnSubmitChapter.setOnClickListener { validateAndUpload() }
    }

    // ─── Load truyện của tác giả ─────────────────────────────────────────────

    private fun loadAuthorStories() {
        val authorId = UserSession.firebaseUid ?: return
        progressLoadingStories.visibility = View.VISIBLE
        spinnerStory.visibility = View.GONE

        MangaRepository.getAllManga(
            onSuccess = { allManga ->
                if (!isAdded) return@getAllManga
                // Lọc chỉ lấy truyện của tác giả đang đăng nhập
                authorStories = allManga.filter { it.firestoreId.isNotEmpty() }
                    .let { list ->
                        // Lọc theo authorId (lấy từ Firestore nếu có, fallback lấy tất cả cho admin)
                        list
                    }

                // Gọi Firestore lấy truyện theo authorId
                loadStoriesByAuthorId(authorId)
            },
            onError = { e ->
                if (!isAdded) return@getAllManga
                progressLoadingStories.visibility = View.GONE
                showError("Không thể tải danh sách truyện: ${e.message}")
            }
        )
    }

    private fun loadStoriesByAuthorId(authorId: String) {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("stories")
            .whereEqualTo("authorId", authorId)
            .get()
            .addOnSuccessListener { result ->
                if (!isAdded) return@addOnSuccessListener
                progressLoadingStories.visibility = View.GONE

                val stories = result.documents.mapIndexedNotNull { index, doc ->
                    val data = doc.data ?: return@mapIndexedNotNull null
                    val categoryStr = data["category"] as? String ?: "truyen_tranh"
                    Manga(
                        id            = index + 1,
                        name          = data["title"]    as? String ?: "",
                        slug          = doc.id,
                        author        = data["author"]   as? String ?: "",
                        description   = data["description"] as? String ?: "",
                        coverUrl      = data["coverUrl"] as? String ?: "",
                        genres        = (data["genres"]  as? List<String>) ?: emptyList(),
                        totalChapters = (data["totalChapters"] as? Long)?.toInt() ?: 0,
                        totalViews    = (data["totalViews"]    as? Long)?.toInt() ?: 0,
                        category      = if (categoryStr == "tieu_thuyet") MangaCategory.TIEU_THUYET else MangaCategory.TRUYEN_TRANH,
                        firestoreId   = doc.id
                    )
                }

                authorStories = stories

                if (stories.isEmpty()) {
                    showError("Bạn chưa có truyện nào. Hãy đăng truyện mới trước!")
                    return@addOnSuccessListener
                }

                setupStorySpinner(stories)
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                progressLoadingStories.visibility = View.GONE
                showError("Không thể tải danh sách truyện: ${e.message}")
            }
    }

    private fun setupStorySpinner(stories: List<Manga>) {
        spinnerStory.visibility = View.VISIBLE

        val storyNames = stories.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, storyNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStory.adapter = adapter

        spinnerStory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                selectedStory = stories[pos]
                updateStoryInfo(stories[pos])
                updateFormVisibility(stories[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedStory = null
                layoutSelectedStoryInfo.visibility = View.GONE
                layoutChapterForm.visibility       = View.GONE
                dividerChapterSection.visibility   = View.GONE
            }
        }

        // Kích hoạt lần đầu
        if (stories.isNotEmpty()) {
            selectedStory = stories[0]
            updateStoryInfo(stories[0])
            updateFormVisibility(stories[0])
        }
    }

    private fun updateStoryInfo(manga: Manga) {
        layoutSelectedStoryInfo.visibility = View.VISIBLE
        tvSelectedStoryTitle.text    = manga.name
        tvSelectedStoryChapters.text = "${manga.totalChapters} chương đã có"
        tvSelectedStoryCategory.text = if (manga.category == MangaCategory.TRUYEN_TRANH) "🖼 Truyện tranh" else "📖 Tiểu thuyết"

        if (manga.coverUrl.isNotEmpty()) {
            Glide.with(this).load(manga.coverUrl).centerCrop().into(ivSelectedStoryCover)
        }

        // Gợi ý số chương tiếp theo
        etChapterNumber.setText((manga.totalChapters + 1).toString())
    }

    private fun updateFormVisibility(manga: Manga) {
        dividerChapterSection.visibility = View.VISIBLE
        layoutChapterForm.visibility     = View.VISIBLE

        val isManga = manga.category == MangaCategory.TRUYEN_TRANH
        layoutPagePicker.visibility   = if (isManga) View.VISIBLE else View.GONE
        layoutNovelContent.visibility = if (!isManga) View.VISIBLE else View.GONE
    }

    // ─── Validate & Upload ───────────────────────────────────────────────────

    private fun validateAndUpload() {
        tvError.visibility = View.GONE
        val story        = selectedStory ?: run { showError("Vui lòng chọn truyện"); return }
        val chapterNum   = etChapterNumber.text.toString().toIntOrNull()
        val chapterTitle = etChapterTitle.text.toString().trim()
        val isFree       = switchFreeChapter.isChecked
        val coinPrice    = etCoinPrice.text.toString().toIntOrNull() ?: 0
        val novelContent = etNovelContent.text.toString().trim()
        val isManga      = story.category == MangaCategory.TRUYEN_TRANH

        when {
            chapterNum == null                 -> { showError("Số chương không hợp lệ"); return }
            chapterNum <= 0                    -> { showError("Số chương phải lớn hơn 0"); return }
            !isFree && coinPrice <= 0          -> { showError("Vui lòng nhập giá coin hợp lệ (> 0)"); return }
            isManga && pageImageUris.isEmpty() -> { showError("Vui lòng chọn ít nhất 1 ảnh trang"); return }
            !isManga && novelContent.isEmpty() -> { showError("Vui lòng nhập nội dung chương"); return }
        }

        setLoading(true)
        val slug = story.firestoreId  // slug chính là firestoreId

        if (isManga && pageImageUris.isNotEmpty()) {
            updateProgress("Đang upload ảnh trang...")
            uploadPageImages(pageImageUris, slug, chapterNum!!) { pageUrls ->
                if (!isAdded) return@uploadPageImages
                saveChapter(story, chapterNum, chapterTitle, novelContent, pageUrls, isFree, coinPrice)
            }
        } else {
            saveChapter(story, chapterNum!!, chapterTitle, novelContent, emptyList(), isFree, coinPrice)
        }
    }

    // ─── Upload ảnh trang tuần tự ─────────────────────────────────────────────

    private fun uploadPageImages(
        uris: List<Uri>,
        slug: String,
        chapterNum: Int,
        onDone: (List<String>) -> Unit
    ) {
        val urls  = mutableListOf<String>()
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

    // ─── Cloudinary Signed Upload ─────────────────────────────────────────────

    private fun uploadToCloudinary(
        uri: Uri,
        folder: String,
        publicId: String,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val bytes: ByteArray = try {
            requireContext().contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw Exception("Không thể đọc file ảnh")
        } catch (e: Exception) {
            onError(e)
            return
        }

        val timestamp    = (System.currentTimeMillis() / 1000).toString()
        val paramsToSign = "folder=$folder&public_id=$publicId&timestamp=$timestamp"
        val signature    = sha1Hex(paramsToSign + API_SECRET)

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("file", "image.jpg", bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()))
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
                        val url = try { JSONObject(body).getString("secure_url") }
                        catch (e: Exception) { onError(e); return@runOnUiThread }
                        onSuccess(url)
                    }
                }
            }
        })
    }

    private fun sha1Hex(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-1").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // ─── Lưu chapter vào Firestore ───────────────────────────────────────────

    private fun saveChapter(
        story: Manga,
        chapterNum: Int,
        chapterTitle: String,
        novelContent: String,
        pageUrls: List<String>,
        isFree: Boolean,
        coinPrice: Int
    ) {
        updateProgress("Đang lưu chapter...")
        MangaRepository.uploadChapter(
            storyFirestoreId = story.firestoreId,
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
                    "✅ Đã thêm Chapter $chapterNum vào \"${story.name}\"!",
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
    }

    // ─── UI helpers ──────────────────────────────────────────────────────────

    private fun setLoading(loading: Boolean) {
        progressAddChapter.visibility = if (loading) View.VISIBLE else View.GONE
        tvProgressDetail.visibility   = if (loading) View.VISIBLE else View.GONE
        btnSubmitChapter.isEnabled    = !loading
        btnSubmitChapter.text         = if (loading) "Đang đăng..." else "Đăng chapter"
        btnPickPages.isEnabled        = !loading
        spinnerStory.isEnabled        = !loading
    }

    private fun updateProgress(msg: String) {
        if (isAdded) tvProgressDetail.text = msg
    }

    private fun showError(msg: String) {
        tvError.text       = "⚠️ $msg"
        tvError.visibility = View.VISIBLE
    }
}
