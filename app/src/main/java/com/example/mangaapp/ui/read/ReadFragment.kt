package com.example.mangaapp.ui.read

import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.repository.ReadingHistoryRepository

class ReadFragment : Fragment() {

    private var firestoreStoryId: String = ""
    private var chapterNumber: Int = 1
    private var chapterList: List<Chapter> = emptyList()

    private lateinit var viewModel: ReaderViewModel

    private lateinit var btnBack: ImageButton
    private lateinit var tvMangaName: TextView
    private lateinit var tvChapterInfo: TextView
    private lateinit var tvChapterTitle: TextView
    private lateinit var rvPages: RecyclerView
    private lateinit var scrollContent: NestedScrollView
    private lateinit var tvContent: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var spinnerChapter: Spinner
    private lateinit var btnFontSize: ImageButton
    private lateinit var btnFollowRead: ImageButton
    private lateinit var progressLoading: ProgressBar
    private lateinit var etChapterComment: EditText
    private lateinit var btnSendChapterComment: Button
    private lateinit var rvChapterComments: RecyclerView
    private lateinit var tvChapterCommentCount: TextView
    private lateinit var chapterCommentAdapter: com.example.mangaapp.ui.detail.CommentAdapter

    private val historyRepository = ReadingHistoryRepository()
    private var mangaName: String = ""
    private var mangaCoverUrl: String = ""
    private var mangaAuthor: String = ""

    // true = đang load chương mới (chuyển chương), scroll về 0 sau khi load xong
    // false = restore sau xoay màn hình, dùng viewModel.lastScrollPosition
    private var isNewChapterLoad = false

    private var isPrefetching = false
    private var prefetchedChapterNumber = -1

    companion object {
        private const val PREFS_NAME = "reading_progress"
        private const val KEY_LAST_STORY_ID = "last_story_id"
        private const val KEY_LAST_CHAPTER = "last_chapter_number"
        private const val KEY_LAST_SCROLL = "last_scroll_position"

        fun newInstance(mangaId: Int, chapterNumber: Int): ReadFragment {
            return newInstance(mangaId, chapterNumber, "")
        }

        fun newInstance(mangaId: Int, chapterNumber: Int, firestoreStoryId: String): ReadFragment {
            val fragment = ReadFragment()
            val args = Bundle()
            args.putInt("chapter_number", chapterNumber)
            args.putString("firestore_story_id", firestoreStoryId)
            fragment.arguments = args
            return fragment
        }
    }

    private lateinit var readingPrefs: SharedPreferences
    private val saveProgressHandler = Handler(Looper.getMainLooper())
    private var pendingSaveRunnable: Runnable? = null
    private val SAVE_DEBOUNCE_MS = 800L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chapterNumber    = arguments?.getInt("chapter_number") ?: 1
        firestoreStoryId = arguments?.getString("firestore_story_id") ?: ""
        android.util.Log.d("ReadFragment", "onCreate: savedInstance=${savedInstanceState != null}")
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_read, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.example.mangaapp.MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Lưu vị trí hiện tại vào ViewModel trước khi view bị destroy
        // Làm ở đây thay vì onSaveInstanceState để chắc chắn view còn sống
        //
        // QUAN TRỌNG: rv_pages có layout_height="wrap_content" và
        // nestedScrollingEnabled="false" (xem fragment_read.xml) — nó KHÔNG tự
        // cuộn. Việc cuộn thực tế do scrollContent (NestedScrollView) đảm nhiệm.
        // Vì vậy phải tính vị trí trang dựa trên scrollContent.scrollY, không
        // dùng LinearLayoutManager.findFirstVisibleItemPosition() (luôn trả về 0
        // vì với rv_pages, mọi item luôn nằm trong "vùng nhìn thấy" của chính nó).
        val pos = getCurrentPageIndex(scrollContent.scrollY)
        if (pos != RecyclerView.NO_POSITION) {
            viewModel.lastScrollPosition = pos
            android.util.Log.d("ReadFragment", "onDestroyView: saved pos=$pos to ViewModel")
        }
        pendingSaveRunnable?.let { saveProgressHandler.removeCallbacks(it) }
        (activity as? com.example.mangaapp.MainActivity)?.showBottomNav()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ReaderViewModel::class.java]

        // Phục hồi đúng chương đang đọc sau khi xoay màn hình.
        // `chapterNumber` ở trên vừa bị onCreate() gán lại từ arguments (chương
        // ban đầu lúc mở truyện) — phải ghi đè bằng giá trị thật từ ViewModel
        // (nếu có) TRƯỚC khi loadChapterList() chạy ở cuối hàm này.
        if (viewModel.currentChapterNumber != -1) {
            chapterNumber = viewModel.currentChapterNumber
        }

        readingPrefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        android.util.Log.d("ReadFragment", "onViewCreated: vmLastScroll=${viewModel.lastScrollPosition}, vmChapter=${viewModel.currentChapterNumber}")
        initViews(view)
        setupObservers()
        setupClickListeners()
        loadChapterList()
    }

    private fun initViews(view: View) {
        btnBack               = view.findViewById(R.id.btn_back)
        tvMangaName           = view.findViewById(R.id.tv_manga_name)
        tvChapterInfo         = view.findViewById(R.id.tv_chapter_info)
        tvChapterTitle        = view.findViewById(R.id.tv_chapter_title)
        rvPages               = view.findViewById(R.id.rv_pages)
        scrollContent         = view.findViewById(R.id.scroll_content)
        tvContent             = view.findViewById(R.id.tv_content)
        btnPrev               = view.findViewById(R.id.btn_prev_chapter)
        btnNext               = view.findViewById(R.id.btn_next_chapter)
        spinnerChapter        = view.findViewById(R.id.spinner_chapter)
        btnFontSize           = view.findViewById(R.id.btn_font_size)
        btnFollowRead         = view.findViewById(R.id.btn_follow_read)
        etChapterComment      = view.findViewById(R.id.et_chapter_comment)
        btnSendChapterComment = view.findViewById(R.id.btn_send_chapter_comment)
        rvChapterComments     = view.findViewById(R.id.rv_chapter_comments)
        tvChapterCommentCount = view.findViewById(R.id.tv_chapter_comment_count)
        progressLoading       = view.findViewById(R.id.progress_loading)
        tvContent.textSize    = viewModel.fontSize.toFloat()
    }

    private fun loadChapterList() {
        if (firestoreStoryId.isEmpty()) return

        val uid = com.example.mangaapp.utils.UserSession.firebaseUid
        if (uid != null && com.example.mangaapp.utils.UserSession.isLoggedIn) {
            MangaRepository.loadUserLists(uid) {
                if (isAdded) updateFollowReadUI(uid, firestoreStoryId)
            }
        }

        MangaRepository.getMangaById(
            firestoreId = firestoreStoryId,
            onSuccess = { manga ->
                if (!isAdded) return@getMangaById
                tvMangaName.text = manga?.name ?: ""
                mangaName        = manga?.name ?: ""
                mangaCoverUrl    = manga?.coverUrl ?: ""
                mangaAuthor      = manga?.author ?: ""
            },
            onError = {}
        )

        MangaRepository.getChaptersByMangaId(
            firestoreId = firestoreStoryId,
            onSuccess = { chapters ->
                if (!isAdded) return@getChaptersByMangaId
                chapterList = chapters
                setupSpinner()

                // Chỉ đọc SharedPrefs khi ViewModel chưa có vị trí nào
                // (tức là lần đầu mở app sau khi bị kill, không phải xoay màn hình)
                if (viewModel.lastScrollPosition == 0) {
                    val resumeStoryId = readingPrefs.getString(KEY_LAST_STORY_ID, null)
                    if (resumeStoryId == firestoreStoryId) {
                        chapterNumber = readingPrefs.getInt(KEY_LAST_CHAPTER, chapterNumber)
                        val savedScroll = readingPrefs.getInt(KEY_LAST_SCROLL, 0)
                        if (savedScroll > 0) viewModel.lastScrollPosition = savedScroll
                    }
                }

                loadChapter(chapterNumber, isNewChapter = false)
            },
            onError = {
                if (!isAdded) return@getChaptersByMangaId
                Toast.makeText(requireContext(), "Không tải được danh sách chương", Toast.LENGTH_SHORT).show()
                loadChapter(chapterNumber, isNewChapter = false)
            }
        )
    }

    // Tìm index của trang đang ở mép trên cùng khung nhìn, dựa trên scrollY
    // thực của scrollContent (NestedScrollView). rv_pages chỉ wrap_content lồng
    // bên trong nên KHÔNG thể dùng findFirstVisibleItemPosition() của nó.
    private fun getCurrentPageIndex(scrollY: Int): Int {
        val lm = rvPages.layoutManager as? LinearLayoutManager ?: return RecyclerView.NO_POSITION
        var result = RecyclerView.NO_POSITION
        for (i in 0 until rvPages.childCount) {
            val child = rvPages.getChildAt(i) ?: continue
            // Toạ độ của trang con tính theo hệ scrollContent = top của rv_pages
            // (trong LinearLayout con của scrollContent) + top của trang trong rv_pages
            val childTopInScroll = rvPages.top + child.top
            if (childTopInScroll <= scrollY + 4) {
                result = lm.getPosition(child)
            } else break
        }
        return result
    }

    private val scrollChangeListener = NestedScrollView.OnScrollChangeListener { _, _, scrollY, _, _ ->
        val pageIdx = getCurrentPageIndex(scrollY)
        if (pageIdx != RecyclerView.NO_POSITION) {
            viewModel.lastScrollPosition = pageIdx
            scheduleSaveProgress(pageIdx)
        }

        // Prefetch chương kế tiếp khi cuộn gần hết nội dung (còn dưới 1 màn hình)
        val contentView = scrollContent.getChildAt(0)
        if (contentView != null && !isPrefetching) {
            val remaining = contentView.height - (scrollY + scrollContent.height)
            if (remaining < scrollContent.height) prefetchNextChapter()
        }
    }

    // Cuộn scrollContent tới đúng trang `index`. Cần rvPages.post vì rv_pages
    // (wrap_content, lồng trong NestedScrollView) cần 1 frame để layout xong
    // các trang con trước khi findViewByPosition() trả về view thật.
    private fun scrollToPageIndex(index: Int) {
        rvPages.post {
            val lm = rvPages.layoutManager as? LinearLayoutManager
            val childView = lm?.findViewByPosition(index)
            if (childView != null) {
                scrollContent.scrollTo(0, rvPages.top + childView.top)
            }
        }
    }

    private fun scheduleSaveProgress(scrollPosition: Int) {
        pendingSaveRunnable?.let { saveProgressHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (!isAdded || firestoreStoryId.isEmpty()) return@Runnable
            readingPrefs.edit()
                .putString(KEY_LAST_STORY_ID, firestoreStoryId)
                .putInt(KEY_LAST_CHAPTER, chapterNumber)
                .putInt(KEY_LAST_SCROLL, scrollPosition)
                .apply()
        }
        pendingSaveRunnable = runnable
        saveProgressHandler.postDelayed(runnable, SAVE_DEBOUNCE_MS)
    }

    private fun setupObservers() {
        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            if (!isAdded) return@observe
            if (pages.isNullOrEmpty()) { showNoContent(); return@observe }

            // isNewChapterLoad = true  → người dùng bấm Next/Prev → scroll về 0
            // isNewChapterLoad = false → xoay màn hình / restore → dùng lastScrollPosition
            val target = if (isNewChapterLoad) 0 else viewModel.lastScrollPosition
            android.util.Log.d("ReadFragment", "observer: isNewChapter=$isNewChapterLoad, vmLast=${viewModel.lastScrollPosition}, target=$target, pages=${pages.size}")

            // Reset flag — chỉ dùng một lần cho lần load này
            isNewChapterLoad = false

            rvPages.visibility    = View.VISIBLE
            tvContent.visibility  = View.GONE
            rvPages.layoutManager = LinearLayoutManager(requireContext())
            rvPages.adapter       = PageAdapter(pages)
            scrollContent.setOnScrollChangeListener(scrollChangeListener)

            // QUAN TRỌNG: rv_pages có layout_height="wrap_content" nên
            // rvPages.scrollToPosition() không có tác dụng — view thực sự
            // đang cuộn là scrollContent (NestedScrollView), phải cuộn nó.
            scrollContent.post {
                if (target > 0 && target < pages.size) {
                    scrollToPageIndex(target)
                } else {
                    scrollContent.scrollTo(0, 0)
                }
            }

            val preloadStart = (target + 1).coerceAtMost(pages.size)
            val preloadEnd   = (preloadStart + 2).coerceAtMost(pages.size)
            if (preloadStart < preloadEnd) {
                viewModel.preloadPages(requireContext(), pages.subList(preloadStart, preloadEnd))
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (!isAdded) return@let
                Toast.makeText(requireContext(), "Không tải được ảnh từ cloud", Toast.LENGTH_SHORT).show()
                showNoContent()
            }
        }
    }

    private fun showNoContent() {
        rvPages.visibility   = View.GONE
        tvContent.visibility = View.VISIBLE
        tvContent.text       = "Chương này chưa có nội dung."
    }

    private fun setupSpinner() {
        if (!isAdded) return
        val labels = chapterList.map { "Chương ${it.chapterNumber}: ${it.title}" }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, labels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChapter.adapter = adapter

        val idx = chapterList.indexOfFirst { it.chapterNumber == chapterNumber }
        if (idx >= 0) spinnerChapter.setSelection(idx)

        spinnerChapter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = chapterList[position].chapterNumber
                if (selected != chapterNumber) loadChapter(selected, isNewChapter = true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // isNewChapter = true  → người dùng chủ động chuyển chương → scroll về đầu
    // isNewChapter = false → restore sau xoay / load lại cùng chương → giữ vị trí
    private fun loadChapter(chapNum: Int, isNewChapter: Boolean) {
        chapterNumber    = chapNum
        viewModel.currentChapterNumber = chapNum
        isNewChapterLoad = isNewChapter
        isPrefetching    = false
        prefetchedChapterNumber = -1

        android.util.Log.d("ReadFragment", "loadChapter: chap=$chapNum, isNew=$isNewChapter, vmLast=${viewModel.lastScrollPosition}")

        val total     = chapterList.size
        val chapTitle = chapterList.find { it.chapterNumber == chapNum }?.title ?: ""
        tvChapterInfo.text  = "Chương $chapNum${if (total > 0) " / $total" else ""}"
        tvChapterTitle.text = "Chương $chapNum${if (chapTitle.isNotEmpty()) ": $chapTitle" else ""}"

        updateNavButtons()

        val idx = chapterList.indexOfFirst { it.chapterNumber == chapNum }
        if (idx >= 0 && spinnerChapter.selectedItemPosition != idx) {
            spinnerChapter.setSelection(idx)
        }

        setupChapterComments(chapNum)

        if (firestoreStoryId.isNotEmpty()) {
            viewModel.loadChapterFromFirestore(firestoreStoryId, "chapter_$chapNum")
            historyRepository.saveOrUpdateHistory(
                storyId       = firestoreStoryId,
                storyTitle    = mangaName,
                storyCoverUrl = mangaCoverUrl,
                authorName    = mangaAuthor,
                chapterId     = "chapter_$chapNum",
                chapterTitle  = "Chương $chapNum${if (chapTitle.isNotEmpty()) ": $chapTitle" else ""}"
            )
        } else {
            showNoContent()
        }
    }

    private fun updateNavButtons() {
        val prevChap = chapterList.filter { it.chapterNumber < chapterNumber }.maxByOrNull { it.chapterNumber }
        val nextChap = chapterList.filter { it.chapterNumber > chapterNumber }.minByOrNull { it.chapterNumber }
        btnPrev.isEnabled = prevChap != null
        btnPrev.alpha     = if (prevChap != null) 1f else 0.4f
        btnNext.isEnabled = nextChap != null
        btnNext.alpha     = if (nextChap != null) 1f else 0.4f
    }

    private fun prefetchNextChapter() {
        if (firestoreStoryId.isEmpty()) return
        val nextChap = chapterList
            .filter { it.chapterNumber > chapterNumber }
            .minByOrNull { it.chapterNumber } ?: return
        if (nextChap.chapterNumber == prefetchedChapterNumber) return

        isPrefetching           = true
        prefetchedChapterNumber = nextChap.chapterNumber

        MangaRepository.getChapterPagesFromFirestore(
            storyId   = firestoreStoryId,
            chapterId = "chapter_${nextChap.chapterNumber}",
            onSuccess = { pageUrls ->
                isPrefetching = false
                if (isAdded) viewModel.preloadPages(requireContext(), pageUrls.take(2))
            },
            onError = { isPrefetching = false }
        )
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnPrev.setOnClickListener {
            chapterList.filter { it.chapterNumber < chapterNumber }
                .maxByOrNull { it.chapterNumber }?.let { loadChapter(it.chapterNumber, isNewChapter = true) }
        }

        btnNext.setOnClickListener {
            chapterList.filter { it.chapterNumber > chapterNumber }
                .minByOrNull { it.chapterNumber }?.let { loadChapter(it.chapterNumber, isNewChapter = true) }
        }

        btnFontSize.setOnClickListener {
            viewModel.fontSize = if (viewModel.fontSize >= 22) 14 else viewModel.fontSize + 2
            tvContent.textSize = viewModel.fontSize.toFloat()
        }

        btnFollowRead.setOnClickListener {
            val uid = com.example.mangaapp.utils.UserSession.firebaseUid
            if (uid == null || !com.example.mangaapp.utils.UserSession.isLoggedIn) {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để mượn/theo dõi truyện", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            MangaRepository.toggleFavorite(uid, firestoreStoryId,
                onSuccess = { isNowFav ->
                    val msg = if (isNowFav) "Đã thêm vào yêu thích ❤" else "Đã xóa khỏi yêu thích"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    MangaRepository.toggleFollowing(uid, firestoreStoryId,
                        onSuccess = { updateFollowReadUI(uid, firestoreStoryId) },
                        onError   = { updateFollowReadUI(uid, firestoreStoryId) }
                    )
                },
                onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun updateFollowReadUI(uid: String, storyId: String) {
        val isFav    = MangaRepository.isFavorite(uid, storyId)
        val isFollow = MangaRepository.isFollowing(uid, storyId)
        btnFollowRead.setImageResource(
            if (isFav || isFollow) R.drawable.ic_favorite_filled else R.drawable.ic_heart_plus
        )
    }

    private fun setupChapterComments(chapNum: Int) {
        if (!isAdded) return
        MangaRepository.initComments(requireContext())
        val commentList = MangaRepository.getCommentsByChapter(firestoreStoryId, chapNum).toMutableList()
        tvChapterCommentCount.text = "${commentList.size} bình luận"

        chapterCommentAdapter = com.example.mangaapp.ui.detail.CommentAdapter(commentList)
        rvChapterComments.layoutManager          = LinearLayoutManager(requireContext())
        rvChapterComments.isNestedScrollingEnabled = false
        rvChapterComments.adapter                = chapterCommentAdapter

        btnSendChapterComment.setOnClickListener {
            val content = etChapterComment.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập bình luận", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newComment = com.example.mangaapp.models.Comment(
                id          = System.currentTimeMillis().toString(),
                firestoreId = firestoreStoryId,
                chapterId   = chapNum,
                userId      = "me",
                userName    = "Bạn",
                content     = content,
                timestamp   = System.currentTimeMillis()
            )
            MangaRepository.addComment(requireContext(), newComment)
            chapterCommentAdapter.addComment(newComment)
            etChapterComment.setText("")
            tvChapterCommentCount.text = "${chapterCommentAdapter.itemCount} bình luận"
            Toast.makeText(requireContext(), "Đã gửi bình luận!", Toast.LENGTH_SHORT).show()
        }
    }
}