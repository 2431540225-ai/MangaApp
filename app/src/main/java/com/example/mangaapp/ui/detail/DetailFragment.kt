package com.example.mangaapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.MainActivity
import com.example.mangaapp.R
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Comment
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaStatus
import com.example.mangaapp.repository.MangaRepository
import java.text.NumberFormat
import java.util.Locale
import com.example.mangaapp.ui.read.ReadFragment

class DetailFragment : Fragment() {

    private var firestoreId: String = ""
    private var isDescExpanded = false
    private var isChapterReversed = false
    private var chapterList: List<Chapter> = emptyList()

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

    // ── Comment views ──
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
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadMangaDetail()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showBottomNav()
    }

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

        // Comment views
        etComment       = view.findViewById(R.id.et_comment)
        btnSendComment  = view.findViewById(R.id.btn_send_comment)
        rvComments      = view.findViewById(R.id.rv_comments)
        tvCommentCount  = view.findViewById(R.id.tv_comment_count)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun loadMangaDetail() {
        if (firestoreId.isEmpty()) return
        progressLoading.visibility = View.VISIBLE

        MangaRepository.getMangaById(
            firestoreId = firestoreId,
            onSuccess = { manga ->
                if (!isAdded || manga == null) return@getMangaById
                progressLoading.visibility = View.GONE
                bindMangaInfo(manga)
                loadChapters(manga)
            },
            onError = {
                if (!isAdded) return@getMangaById
                progressLoading.visibility = View.GONE
                Toast.makeText(requireContext(), "Không tải được thông tin truyện", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun bindMangaInfo(manga: Manga) {
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivCover)
        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivBackdrop)

        tvName.text         = manga.name
        tvAuthor.text       = "✍️ ${manga.author}"
        tvViews.text        = format.format(manga.totalViews)
        tvChapterCount.text = manga.totalChapters.toString()
        tvDescription.text  = manga.description

        if (manga.status == MangaStatus.ONGOING) {
            tvStatus.text = "Đang ra"
            tvStatus.setBackgroundColor(resources.getColor(R.color.badge_new, null))
        } else {
            tvStatus.text = "Hoàn thành"
            tvStatus.setBackgroundColor(resources.getColor(R.color.badge_full, null))
        }

        setupGenreTags(manga)

        tvReadMore.setOnClickListener {
            isDescExpanded = !isDescExpanded
            tvDescription.maxLines = if (isDescExpanded) Int.MAX_VALUE else 4
            tvReadMore.text = if (isDescExpanded) "Thu gọn ‹" else "Xem thêm ›"
        }
    }

    private fun loadChapters(manga: Manga) {
        MangaRepository.getChaptersByMangaId(
            firestoreId = firestoreId,
            onSuccess = { chapters ->
                if (!isAdded) return@getChaptersByMangaId
                chapterList = chapters
                tvChapterCount.text = chapters.size.toString()

                btnReadFirst.setOnClickListener {
                    chapters.firstOrNull()?.let { navigateToRead(manga, it.chapterNumber) }
                }
                btnReadLatest.setOnClickListener {
                    chapters.lastOrNull()?.let { navigateToRead(manga, it.chapterNumber) }
                }

                chapterAdapter = ChapterAdapter(chapters) { chapter ->
                    navigateToRead(manga, chapter.chapterNumber)
                }
                rvChapters.layoutManager = LinearLayoutManager(requireContext())
                rvChapters.isNestedScrollingEnabled = false
                rvChapters.adapter = chapterAdapter

                tvSortChapter.setOnClickListener {
                    isChapterReversed = !isChapterReversed
                    val sorted = if (isChapterReversed) chapters.sortedBy { it.chapterNumber }
                    else chapters.sortedByDescending { it.chapterNumber }
                    tvSortChapter.text = if (isChapterReversed) "Cũ nhất ↑" else "Mới nhất ↓"
                    chapterAdapter.updateList(sorted)
                }

                // Load comments sau khi có firestoreId sẵn sàng
                setupComments()
            },
            onError = {
                if (!isAdded) return@getChaptersByMangaId
                Toast.makeText(requireContext(), "Không tải được danh sách chương", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupComments() {
        MangaRepository.initComments(requireContext())
        val commentList = MangaRepository.getCommentsByFirestoreId(firestoreId).toMutableList()
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
                userName    = "Bạn",
                content     = content,
                timestamp   = System.currentTimeMillis()
            )
            MangaRepository.addComment(requireContext(), newComment)
            commentAdapter.addComment(newComment)
            etComment.setText("")
            tvCommentCount.text = "${commentAdapter.itemCount} bình luận"
            Toast.makeText(requireContext(), "Đã gửi bình luận!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupGenreTags(manga: Manga) {
        llGenres.removeAllViews()
        manga.genres.forEach { genre ->
            val tag = TextView(requireContext()).apply {
                text = genre
                textSize = 12f
                setTextColor(resources.getColor(R.color.primary, null))
                setPadding(20, 8, 20, 8)
                background = resources.getDrawable(android.R.drawable.btn_default_small, null)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 8 }
            }
            llGenres.addView(tag)
        }
    }

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