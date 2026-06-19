package com.example.mangaapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.MainActivity
import com.example.mangaapp.R
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Comment
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaStatus
import com.example.mangaapp.ui.read.ReadFragment
import com.example.mangaapp.utils.UserSession
import java.text.NumberFormat
import java.util.Locale

class DetailFragment : Fragment() {

    private var firestoreId: String = ""

    // ── ViewModel (giữ state qua rotation) ───────────────────────────────────
    private val viewModel: DetailViewModel by viewModels()

    // ── Views (khởi tạo lại sau mỗi rotation) ────────────────────────────────
    private lateinit var ivBackdrop: android.widget.ImageView
    private lateinit var ivCover: android.widget.ImageView
    private lateinit var btnBack: ImageButton
    private lateinit var tvName: TextView
    private lateinit var tvAuthor: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvViews: TextView
    private lateinit var tvChapterCount: TextView
    private lateinit var btnReadFirst: Button
    private lateinit var btnReadLatest: Button
    private lateinit var llGenres: LinearLayout
    private lateinit var tvDescription: TextView
    private lateinit var tvReadMore: TextView
    private lateinit var rvChapters: RecyclerView
    private lateinit var tvSortChapter: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var etComment: EditText
    private lateinit var btnSendComment: Button
    private lateinit var rvComments: RecyclerView
    private lateinit var tvCommentCount: TextView
    private lateinit var commentAdapter: CommentAdapter
    private lateinit var chapterAdapter: ChapterAdapter

    companion object {
        fun newInstance(firestoreId: String): DetailFragment {
            val fragment = DetailFragment()
            val args = Bundle()
            args.putString("firestore_id", firestoreId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestoreId = arguments?.getString("firestore_id") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        observeViewModel()
        // Chỉ load lần đầu; sau rotation ViewModel còn data sẽ skip fetch
        viewModel.loadDetail(firestoreId)
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showBottomNav()
    }

    // ── Init Views ────────────────────────────────────────────────────────────

    private fun initViews(view: View) {
        ivBackdrop      = view.findViewById(R.id.iv_backdrop)
        ivCover         = view.findViewById(R.id.iv_cover)
        btnBack         = view.findViewById(R.id.btn_back)
        tvName          = view.findViewById(R.id.tv_manga_name)
        tvAuthor        = view.findViewById(R.id.tv_author)
        tvStatus        = view.findViewById(R.id.tv_status)
        tvViews         = view.findViewById(R.id.tv_views)
        tvChapterCount  = view.findViewById(R.id.tv_chapter_count)
        btnReadFirst    = view.findViewById(R.id.btn_read_first)
        btnReadLatest   = view.findViewById(R.id.btn_read_latest)
        llGenres        = view.findViewById(R.id.ll_genres)
        tvDescription   = view.findViewById(R.id.tv_description)
        tvReadMore      = view.findViewById(R.id.tv_read_more)
        rvChapters      = view.findViewById(R.id.rv_chapters)
        tvSortChapter   = view.findViewById(R.id.tv_sort_chapter)
        progressLoading = view.findViewById(R.id.progress_loading)
        etComment       = view.findViewById(R.id.et_comment)
        btnSendComment  = view.findViewById(R.id.btn_send_comment)
        rvComments      = view.findViewById(R.id.rv_comments)
        tvCommentCount  = view.findViewById(R.id.tv_comment_count)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    // ── Observe ViewModel ─────────────────────────────────────────────────────

    private fun observeViewModel() {
        // Loading state
        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        // Error
        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (!isAdded) return@let
                Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
            }
        }

        // Manga info — bind ngay khi có (kể cả sau rotation)
        viewModel.manga.observe(viewLifecycleOwner) { manga ->
            if (!isAdded || manga == null) return@observe
            bindMangaInfo(manga)
        }

        // Chapters — bind ngay khi có
        viewModel.chapters.observe(viewLifecycleOwner) { chapters ->
            if (!isAdded) return@observe
            bindChapters(chapters)
        }

        // Comments — load từ local storage khi view ready
        viewModel.loadComments(firestoreId, requireContext())
        viewModel.comments.observe(viewLifecycleOwner) { comments ->
            if (!isAdded) return@observe
            bindComments(comments.toMutableList())
        }
    }

    // ── Bind Manga Info ───────────────────────────────────────────────────────

    private fun bindMangaInfo(manga: Manga) {
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivCover)
        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivBackdrop)
        tvName.text         = manga.name
        tvAuthor.text       = "✍️ ${manga.author}"
        tvViews.text        = format.format(manga.totalViews)
        tvDescription.text  = manga.description

        // Khôi phục trạng thái mô tả sau rotation
        tvDescription.maxLines = if (viewModel.isDescExpanded) Int.MAX_VALUE else 4
        tvReadMore.text = if (viewModel.isDescExpanded) "Thu gọn ‹" else "Xem thêm ›"

        if (manga.status == MangaStatus.ONGOING) {
            tvStatus.text = "Đang ra"
            tvStatus.setBackgroundColor(resources.getColor(R.color.badge_new, null))
        } else {
            tvStatus.text = "Hoàn thành"
            tvStatus.setBackgroundColor(resources.getColor(R.color.badge_full, null))
        }

        setupGenreTags(manga)

        tvReadMore.setOnClickListener {
            viewModel.isDescExpanded = !viewModel.isDescExpanded
            tvDescription.maxLines = if (viewModel.isDescExpanded) Int.MAX_VALUE else 4
            tvReadMore.text = if (viewModel.isDescExpanded) "Thu gọn ‹" else "Xem thêm ›"
        }
    }

    // ── Bind Chapters ─────────────────────────────────────────────────────────

    private fun bindChapters(chapters: List<Chapter>) {
        val manga = viewModel.manga.value ?: return

        // Cập nhật số chương
        tvChapterCount.text = chapters.size.toString()

        // Nút đọc từ đầu / mới nhất / đọc tiếp
        val lastReadChap = com.example.mangaapp.utils.UserPreferences.getLastReadChapter(requireContext(), firestoreId)
        if (lastReadChap != -1) {
            btnReadFirst.text = "▶ Đọc tiếp C.$lastReadChap"
            btnReadFirst.setOnClickListener {
                val targetChapter = chapters.find { it.chapterNumber == lastReadChap }
                if (targetChapter != null) {
                    handleChapterClick(manga, targetChapter)
                } else {
                    chapters.firstOrNull()?.let { handleChapterClick(manga, it) }
                }
            }
        } else {
            btnReadFirst.text = "▶ Đọc từ đầu"
            btnReadFirst.setOnClickListener {
                chapters.firstOrNull()?.let { handleChapterClick(manga, it) }
            }
        }

        btnReadLatest.setOnClickListener {
            chapters.lastOrNull()?.let { handleChapterClick(manga, it) }
        }

        // Khôi phục trạng thái sort sau rotation
        val sortedChapters = viewModel.getSortedChapters()
        tvSortChapter.text = if (viewModel.isChapterReversed) "Cũ nhất ↑" else "Mới nhất ↓"

        chapterAdapter = ChapterAdapter(sortedChapters) { chapter ->
            handleChapterClick(manga, chapter)
        }
        chapterAdapter.setStoryId(firestoreId)

        rvChapters.layoutManager = LinearLayoutManager(requireContext())
        rvChapters.isNestedScrollingEnabled = false
        rvChapters.adapter = chapterAdapter

        tvSortChapter.setOnClickListener {
            viewModel.isChapterReversed = !viewModel.isChapterReversed
            val sorted = viewModel.getSortedChapters()
            tvSortChapter.text = if (viewModel.isChapterReversed) "Cũ nhất ↑" else "Mới nhất ↓"
            chapterAdapter.updateList(sorted)
        }
    }

    // ── Bind Comments ─────────────────────────────────────────────────────────

    private fun bindComments(commentList: MutableList<Comment>) {
        tvCommentCount.text = "${commentList.size} bình luận"
        commentAdapter = CommentAdapter(commentList)
        rvComments.layoutManager = LinearLayoutManager(requireContext())
        rvComments.isNestedScrollingEnabled = false
        rvComments.adapter = commentAdapter

        btnSendComment.setOnClickListener {
            val content = etComment.text.toString().trim()
            if (content.isEmpty()) {
                Toast.makeText(requireContext(), "Vui lòng nhập bình luận", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val newComment = Comment(
                id          = System.currentTimeMillis().toString(),
                firestoreId = firestoreId,
                userId      = "me",
                userName    = UserSession.currentUser?.username ?: "Bạn",
                content     = content,
                timestamp   = System.currentTimeMillis()
            )
            viewModel.addComment(requireContext(), newComment, firestoreId)
            etComment.setText("")
        }
    }

    // ── Handle Chapter Click ──────────────────────────────────────────────────

    private fun handleChapterClick(manga: Manga, chapter: Chapter) {
        if (chapter.isFree) {
            navigateToRead(manga, chapter.chapterNumber)
            return
        }
        val user = UserSession.currentUser
        if (user != null && user.hasUnlocked(firestoreId, chapter.chapterNumber)) {
            navigateToRead(manga, chapter.chapterNumber)
            return
        }
        if (!UserSession.isLoggedIn) {
            Toast.makeText(
                requireContext(),
                "Vui lòng đăng nhập để mở khóa chương này",
                Toast.LENGTH_LONG
            ).show()
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, com.example.mangaapp.ui.auth.LoginFragment())
                .addToBackStack(null)
                .commit()
            return
        }
        UnlockChapterDialog.show(
            fragmentManager = parentFragmentManager,
            storyId         = firestoreId,
            chapter         = chapter,
            onUnlocked      = {
                chapterAdapter.notifyDataSetChanged()
                navigateToRead(manga, chapter.chapterNumber)
            }
        )
    }

    // ── Genre Tags ────────────────────────────────────────────────────────────

    private fun setupGenreTags(manga: Manga) {
        llGenres.removeAllViews()
        manga.genres.forEach { genre ->
            val tag = TextView(requireContext()).apply {
                text = genre
                textSize = 12f
                setTextColor(resources.getColor(R.color.primary, null))
                // padding tính theo pixel (ở đây khoảng 12dp ngang, 6dp dọc)
                setPadding(32, 16, 32, 16)
                
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 100f // Pill shape
                    setColor(resources.getColor(R.color.color_background, null))
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 8 }
            }
            llGenres.addView(tag)
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    private fun navigateToRead(manga: Manga, chapterNumber: Int) {
        val fragment = ReadFragment.newInstance(
            mangaId          = manga.id,
            chapterNumber    = chapterNumber,
            firestoreStoryId = manga.firestoreId
        )
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}