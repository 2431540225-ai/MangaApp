package com.example.mangaapp.ui.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.ImageView
import android.widget.LinearLayout
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
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.ui.read.ReadFragment
import com.example.mangaapp.utils.UserSession
import java.text.NumberFormat
import java.util.Locale

class DetailFragment : Fragment() {

    // rating views
    private lateinit var tvAverageRating: TextView
    private lateinit var llStarsDisplay: LinearLayout
    private lateinit var tvRatingCount: TextView
    private lateinit var llStarsInput: LinearLayout
    private lateinit var tvYourRatingLabel: TextView
    private var userCurrentStar = 0

    private var firestoreId: String = ""
    private var chapterList: List<Chapter> = emptyList()

    // ── ViewModel (giữ state qua rotation) ───────────────────────────────────
    private val viewModel: DetailViewModel by viewModels()

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
    private lateinit var btnFavoriteFollow: ImageView
    private lateinit var btnFollowCard: ImageButton
    private lateinit var btnLibraryContainer: LinearLayout
    private lateinit var tvLibraryLabel: TextView
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

        // Đảm bảo UserSession đã load trước khi dùng
        if (com.example.mangaapp.utils.UserSession.firebaseUid != null &&
            !com.example.mangaapp.utils.UserSession.isLoggedIn) {
            com.example.mangaapp.utils.UserSession.loadUser {
                viewModel.loadDetail(firestoreId)
            }
        } else {
            viewModel.loadDetail(firestoreId)
        }
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
        btnFavoriteFollow = view.findViewById(R.id.btn_favorite_follow)
        btnFollowCard   = view.findViewById(R.id.btn_follow_card)
        btnLibraryContainer = view.findViewById(R.id.btn_library_container)
        tvLibraryLabel  = view.findViewById(R.id.tv_library_label)
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

        tvAverageRating  = view.findViewById(R.id.tv_average_rating)
        llStarsDisplay   = view.findViewById(R.id.ll_stars_display)
        tvRatingCount    = view.findViewById(R.id.tv_rating_count)
        llStarsInput     = view.findViewById(R.id.ll_stars_input)
        tvYourRatingLabel = view.findViewById(R.id.tv_your_rating_label)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

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

        // Manga info — bind ngay khi có
        viewModel.manga.observe(viewLifecycleOwner) { manga ->
            if (!isAdded || manga == null) return@observe
            bindMangaInfo(manga)
            setupFavoriteFollowButtons(manga)
            setupRating(manga)
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

    private fun bindMangaInfo(manga: Manga) {
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))
        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivCover)
        Glide.with(this).load(manga.coverUrl).centerCrop().into(ivBackdrop)
        tvName.text         = manga.name
        tvAuthor.text       = "✍️ ${manga.author}"
        tvViews.text        = format.format(manga.totalViews)
        tvChapterCount.text = manga.totalChapters.toString()
        tvDescription.text  = manga.description

        // Khôi phục trạng thái mô tả sau rotation
        tvDescription.maxLines = if (viewModel.isDescExpanded) Int.MAX_VALUE else 4
        tvReadMore.text = if (viewModel.isDescExpanded) "Thu gọn ‹" else "Xem thêm ›"

        if (manga.status == MangaStatus.ONGOING) {
            tvStatus.text = "Đang ra"
            tvStatus.setBackgroundResource(R.drawable.bg_badge_status)
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.emerald, null))
        } else {
            tvStatus.text = "Hoàn thành"
            tvStatus.setBackgroundResource(R.drawable.bg_badge_status)
            tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(resources.getColor(R.color.amber, null))
        }

        setupGenreTags(manga)
        tvReadMore.setOnClickListener {
            viewModel.isDescExpanded = !viewModel.isDescExpanded
            tvDescription.maxLines = if (viewModel.isDescExpanded) Int.MAX_VALUE else 4
            tvReadMore.text = if (viewModel.isDescExpanded) "Thu gọn ‹" else "Xem thêm ›"
        }
    }

    private fun bindChapters(chapters: List<Chapter>) {
        val manga = viewModel.manga.value ?: return
        chapterList = chapters
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

    /**
     * Xử lý click vào chapter:
     *  - Nếu miễn phí hoặc đã mở khóa → đọc ngay
     *  - Nếu trả phí và chưa mở → hiện dialog xác nhận coin
     *  - Nếu chưa đăng nhập → yêu cầu đăng nhập
     */
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

    private fun setupFavoriteFollowButtons(manga: Manga) {
        val uid = UserSession.firebaseUid

        if (uid == null || !UserSession.isLoggedIn) {
            btnLibraryContainer.setOnClickListener {
                Toast.makeText(context, "Vui lòng đăng nhập để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            }
            return
        }

        MangaRepository.loadUserLists(uid) {
            if (!isAdded) return@loadUserLists
            updateFavoriteFollowButtonUI(uid, manga.firestoreId)
        }

        btnLibraryContainer.setOnClickListener {
            showLibraryBottomSheet(uid, manga)
        }
    }

    private fun showLibraryBottomSheet(uid: String, manga: Manga) {
        val isFav    = MangaRepository.isFavorite(uid, manga.firestoreId)
        val isFollow = MangaRepository.isFollowing(uid, manga.firestoreId)

        val options = arrayOf(
            (if (isFav) "❤️ Bỏ yêu thích" else "🤍 Yêu thích"),
            (if (isFollow) "🔔 Hủy theo dõi" else "🔕 Theo dõi")
        )

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Thêm vào thư viện")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> MangaRepository.toggleFavorite(uid, manga.firestoreId,
                        onSuccess = { isNowFav ->
                            val msg = if (isNowFav) "Đã thêm vào yêu thích ❤" else "Đã xóa khỏi yêu thích"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            updateFavoriteFollowButtonUI(uid, manga.firestoreId)
                        },
                        onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
                    )
                    1 -> MangaRepository.toggleFollowing(uid, manga.firestoreId,
                        onSuccess = { isNowFollow ->
                            val msg = if (isNowFollow) "Đã theo dõi truyện 🔔" else "Đã hủy theo dõi"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            updateFavoriteFollowButtonUI(uid, manga.firestoreId)
                        },
                        onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
                    )
                }
            }
            .show()
    }

    private fun updateFavoriteFollowButtonUI(uid: String, storyId: String) {
        val isFav    = MangaRepository.isFavorite(uid, storyId)
        val isFollow = MangaRepository.isFollowing(uid, storyId)

        when {
            isFav && isFollow -> {
                btnFavoriteFollow.setImageResource(R.drawable.ic_favorite_filled)
                btnFavoriteFollow.setColorFilter(resources.getColor(R.color.error_red, null))
                tvLibraryLabel.text = "Yêu thích & Đang theo dõi"
                tvLibraryLabel.setTextColor(resources.getColor(R.color.error_red, null))
            }
            isFav -> {
                btnFavoriteFollow.setImageResource(R.drawable.ic_favorite_filled)
                btnFavoriteFollow.setColorFilter(resources.getColor(R.color.error_red, null))
                tvLibraryLabel.text = "Đã yêu thích"
                tvLibraryLabel.setTextColor(resources.getColor(R.color.error_red, null))
            }
            isFollow -> {
                btnFavoriteFollow.setImageResource(R.drawable.ic_follow_filled)
                btnFavoriteFollow.setColorFilter(resources.getColor(R.color.primary, null))
                tvLibraryLabel.text = "Đang theo dõi"
                tvLibraryLabel.setTextColor(resources.getColor(R.color.primary, null))
            }
            else -> {
                btnFavoriteFollow.setImageResource(R.drawable.ic_favorite_border)
                btnFavoriteFollow.setColorFilter(resources.getColor(R.color.color_inactive_icon, null))
                tvLibraryLabel.text = "Thêm vào thư viện"
                tvLibraryLabel.setTextColor(resources.getColor(R.color.color_text_secondary, null))
            }
        }
    }

    private fun setupGenreTags(manga: Manga) {
        llGenres.removeAllViews()
        manga.genres.forEach { genre ->
            val tag = TextView(requireContext()).apply {
                text = genre
                textSize = 12f
                setTextColor(resources.getColor(R.color.primary, null))
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

    // ─── Rating ──────────────────────────────────────────────────────────────

    private fun setupRating(manga: Manga) {
        updateRatingDisplay(manga.averageRating, manga.ratingCount)
        buildInputStars(manga.firestoreId)

        MangaRepository.getUserRating(manga.firestoreId) { savedStar ->
            if (!isAdded) return@getUserRating
            userCurrentStar = savedStar
            highlightInputStars(savedStar)
            if (savedStar > 0) tvYourRatingLabel.text = "Bạn đã đánh giá $savedStar ⭐"
        }
    }

    private fun updateRatingDisplay(avg: Float, count: Int) {
        tvAverageRating.text = if (avg > 0) "%.1f".format(avg) else "–"
        tvRatingCount.text   = "$count đánh giá"

        llStarsDisplay.removeAllViews()
        val full  = avg.toInt()
        val half  = (avg - full) >= 0.4f
        for (i in 1..5) {
            val tv = TextView(requireContext()).apply {
                text     = when {
                    i <= full          -> "★"
                    i == full + 1 && half -> "★"
                    else               -> "☆"
                }
                textSize = 18f
                setTextColor(
                    if (i <= full || (i == full + 1 && half))
                        resources.getColor(R.color.amber, null)
                    else
                        resources.getColor(R.color.color_inactive_icon, null)
                )
            }
            llStarsDisplay.addView(tv)
        }
    }

    private fun buildInputStars(storyId: String) {
        llStarsInput.removeAllViews()
        for (i in 1..5) {
            val tv = TextView(requireContext()).apply {
                text     = "☆"
                textSize = 28f
                setPadding(4, 0, 4, 0)
                setTextColor(resources.getColor(R.color.color_inactive_icon, null))
                setOnClickListener { onStarSelected(storyId, i) }
            }
            llStarsInput.addView(tv)
        }
    }

    private fun highlightInputStars(selected: Int) {
        for (i in 0 until llStarsInput.childCount) {
            val tv = llStarsInput.getChildAt(i) as TextView
            if (i < selected) {
                tv.text = "★"
                tv.setTextColor(resources.getColor(R.color.amber, null))
            } else {
                tv.text = "☆"
                tv.setTextColor(resources.getColor(R.color.color_inactive_icon, null))
            }
        }
    }

    private fun onStarSelected(storyId: String, star: Int) {
        if (!UserSession.isLoggedIn) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để đánh giá", Toast.LENGTH_SHORT).show()
            return
        }
        if (star == userCurrentStar) return

        highlightInputStars(star)
        tvYourRatingLabel.text = "Đang lưu..."

        MangaRepository.submitRating(
            storyId   = storyId,
            star      = star,
            onSuccess = { newAvg, newCount ->
                if (!isAdded) return@submitRating
                userCurrentStar = star
                updateRatingDisplay(newAvg, newCount)
                tvYourRatingLabel.text = "Bạn đã đánh giá $star ⭐"
                Toast.makeText(requireContext(), "Đánh giá thành công!", Toast.LENGTH_SHORT).show()
            },
            onError = { e ->
                if (!isAdded) return@submitRating
                highlightInputStars(userCurrentStar)
                tvYourRatingLabel.text = if (userCurrentStar > 0) "Bạn đã đánh giá $userCurrentStar ⭐" else ""
                Toast.makeText(requireContext(), "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}