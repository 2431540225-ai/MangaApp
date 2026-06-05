package com.example.mangaapp.ui.read

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
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
    private var fontSize: Int = 16

    private var chapterList: List<Chapter> = emptyList()

    private lateinit var viewModel: ReaderViewModel

    private lateinit var btnBack: ImageButton
    private lateinit var tvMangaName: TextView
    private lateinit var tvChapterInfo: TextView
    private lateinit var tvChapterTitle: TextView
    private lateinit var rvPages: RecyclerView
    private lateinit var tvContent: TextView
    private lateinit var btnPrev: Button
    private lateinit var btnNext: Button
    private lateinit var spinnerChapter: Spinner
    private lateinit var btnFontSize: ImageButton
    private lateinit var progressLoading: ProgressBar
    private lateinit var etChapterComment: EditText
    private lateinit var btnSendChapterComment: Button
    private lateinit var rvChapterComments: RecyclerView
    private lateinit var tvChapterCommentCount: TextView
    private lateinit var chapterCommentAdapter: com.example.mangaapp.ui.detail.CommentAdapter

    // ← THÊM MỚI
    private val historyRepository = ReadingHistoryRepository()
    private var mangaName: String = ""
    private var mangaCoverUrl: String = ""
    private var mangaAuthor: String = ""

    companion object {
        fun newInstance(mangaId: Int, chapterNumber: Int): ReadFragment {
            return newInstance(mangaId, chapterNumber, "")
        }

        fun newInstance(
            mangaId: Int,
            chapterNumber: Int,
            firestoreStoryId: String
        ): ReadFragment {
            val fragment = ReadFragment()
            val args = Bundle()
            args.putInt("chapter_number", chapterNumber)
            args.putString("firestore_story_id", firestoreStoryId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        chapterNumber    = arguments?.getInt("chapter_number") ?: 1
        firestoreStoryId = arguments?.getString("firestore_story_id") ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_read, container, false)
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.example.mangaapp.MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? com.example.mangaapp.MainActivity)?.showBottomNav()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[ReaderViewModel::class.java]
        initViews(view)
        setupObservers()
        setupClickListeners()
        loadChapterList()
    }

    private fun initViews(view: View) {
        btnBack         = view.findViewById(R.id.btn_back)
        tvMangaName     = view.findViewById(R.id.tv_manga_name)
        tvChapterInfo   = view.findViewById(R.id.tv_chapter_info)
        tvChapterTitle  = view.findViewById(R.id.tv_chapter_title)
        rvPages         = view.findViewById(R.id.rv_pages)
        tvContent       = view.findViewById(R.id.tv_content)
        btnPrev         = view.findViewById(R.id.btn_prev_chapter)
        btnNext         = view.findViewById(R.id.btn_next_chapter)
        spinnerChapter  = view.findViewById(R.id.spinner_chapter)
        btnFontSize     = view.findViewById(R.id.btn_font_size)
        etChapterComment      = view.findViewById(R.id.et_chapter_comment)
        btnSendChapterComment = view.findViewById(R.id.btn_send_chapter_comment)
        rvChapterComments     = view.findViewById(R.id.rv_chapter_comments)
        tvChapterCommentCount = view.findViewById(R.id.tv_chapter_comment_count)
        progressLoading = view.findViewById(R.id.progress_loading)
    }

    private fun loadChapterList() {
        if (firestoreStoryId.isEmpty()) return

        // Load thông tin truyện (tên + ảnh bìa + tác giả) từ Firestore
        MangaRepository.getMangaById(
            firestoreId = firestoreStoryId,
            onSuccess = { manga ->
                if (!isAdded) return@getMangaById
                tvMangaName.text = manga?.name ?: ""
                // ← THÊM MỚI: lưu lại để dùng khi ghi lịch sử
                mangaName     = manga?.name ?: ""
                mangaCoverUrl = manga?.coverUrl ?: ""
                mangaAuthor   = manga?.author ?: ""
            },
            onError = { }
        )

        MangaRepository.getChaptersByMangaId(
            firestoreId = firestoreStoryId,
            onSuccess = { chapters ->
                if (!isAdded) return@getChaptersByMangaId
                chapterList = chapters
                setupSpinner()
                loadChapter(chapterNumber)
            },
            onError = {
                if (!isAdded) return@getChaptersByMangaId
                Toast.makeText(requireContext(), "Không tải được danh sách chương", Toast.LENGTH_SHORT).show()
                loadChapter(chapterNumber)
            }
        )
    }

    private fun setupObservers() {
        viewModel.pages.observe(viewLifecycleOwner) { pages ->
            if (!isAdded) return@observe
            if (pages.isNotEmpty()) {
                rvPages.visibility   = View.VISIBLE
                tvContent.visibility = View.GONE
                rvPages.layoutManager = LinearLayoutManager(requireContext())
                rvPages.adapter = PageAdapter(pages)

                rvPages.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        val lm = rvPages.layoutManager as LinearLayoutManager
                        val lastVisible = lm.findLastVisibleItemPosition()
                        if (lastVisible >= pages.size - 2) prefetchNextChapter()
                    }
                })
            } else {
                showNoContent()
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
                if (selected != chapterNumber) loadChapter(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChapter(chapNum: Int) {
        chapterNumber = chapNum

        val total = chapterList.size
        tvChapterInfo.text = "Chương $chapNum${if (total > 0) " / $total" else ""}"

        val chapTitle = chapterList.find { it.chapterNumber == chapNum }?.title ?: ""
        tvChapterTitle.text = "Chương $chapNum${if (chapTitle.isNotEmpty()) ": $chapTitle" else ""}"

        updateNavButtons()

        val idx = chapterList.indexOfFirst { it.chapterNumber == chapNum }
        if (idx >= 0 && spinnerChapter.selectedItemPosition != idx) {
            spinnerChapter.setSelection(idx)
        }

        setupChapterComments(chapNum)

        if (firestoreStoryId.isNotEmpty()) {
            viewModel.loadChapterFromFirestore(firestoreStoryId, "chapter_$chapNum")

            // ← THÊM MỚI: lưu lịch sử đọc
            historyRepository.saveOrUpdateHistory(
                storyId      = firestoreStoryId,
                storyTitle   = mangaName,
                storyCoverUrl = mangaCoverUrl,
                authorName   = mangaAuthor,
                chapterId    = "chapter_$chapNum",
                chapterTitle = "Chương $chapNum${if (chapTitle.isNotEmpty()) ": $chapTitle" else ""}"
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

        MangaRepository.getChapterPagesFromFirestore(
            storyId   = firestoreStoryId,
            chapterId = "chapter_${nextChap.chapterNumber}",
            onSuccess = {},
            onError   = {}
        )
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        btnPrev.setOnClickListener {
            chapterList.filter { it.chapterNumber < chapterNumber }
                .maxByOrNull { it.chapterNumber }?.let { loadChapter(it.chapterNumber) }
        }

        btnNext.setOnClickListener {
            chapterList.filter { it.chapterNumber > chapterNumber }
                .minByOrNull { it.chapterNumber }?.let { loadChapter(it.chapterNumber) }
        }

        btnFontSize.setOnClickListener {
            fontSize = if (fontSize >= 22) 14 else fontSize + 2
            tvContent.textSize = fontSize.toFloat()
        }
    }

    private fun setupChapterComments(chapNum: Int) {
        if (!isAdded) return
        MangaRepository.initComments(requireContext())
        val commentList = MangaRepository.getCommentsByChapter(firestoreStoryId, chapNum).toMutableList()
        tvChapterCommentCount.text = "${commentList.size} bình luận"

        chapterCommentAdapter = com.example.mangaapp.ui.detail.CommentAdapter(commentList)
        rvChapterComments.layoutManager = LinearLayoutManager(requireContext())
        rvChapterComments.isNestedScrollingEnabled = false
        rvChapterComments.adapter = chapterCommentAdapter

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