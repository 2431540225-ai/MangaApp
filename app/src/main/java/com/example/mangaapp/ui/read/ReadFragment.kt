package com.example.mangaapp.ui.read

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.repository.MangaRepository

class ReadFragment : Fragment() {

    private var mangaId: Int = -1
    private var chapterNumber: Int = 1
    private var fontSize: Int = 16

    // Views
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

    companion object {
        fun newInstance(mangaId: Int, chapterNumber: Int): ReadFragment {
            val fragment = ReadFragment()
            val args = Bundle()
            args.putInt("manga_id", mangaId)
            args.putInt("chapter_number", chapterNumber)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mangaId       = arguments?.getInt("manga_id") ?: -1
        chapterNumber = arguments?.getInt("chapter_number") ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_read, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupSpinner()
        loadChapter(chapterNumber)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        btnBack        = view.findViewById(R.id.btn_back)
        tvMangaName    = view.findViewById(R.id.tv_manga_name)
        tvChapterInfo  = view.findViewById(R.id.tv_chapter_info)
        tvChapterTitle = view.findViewById(R.id.tv_chapter_title)
        rvPages        = view.findViewById(R.id.rv_pages)
        tvContent      = view.findViewById(R.id.tv_content)
        btnPrev        = view.findViewById(R.id.btn_prev_chapter)
        btnNext        = view.findViewById(R.id.btn_next_chapter)
        spinnerChapter = view.findViewById(R.id.spinner_chapter)
        btnFontSize    = view.findViewById(R.id.btn_font_size)
    }

    private fun setupSpinner() {
        val chapters = MangaRepository.getChaptersByMangaId(mangaId)
        val chapterLabels = chapters.map { "Chương ${it.chapterNumber}: ${it.title}" }
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            chapterLabels
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerChapter.adapter = adapter

        // Chọn chương hiện tại
        val currentIndex = chapters.indexOfFirst { it.chapterNumber == chapterNumber }
        if (currentIndex >= 0) spinnerChapter.setSelection(currentIndex)

        spinnerChapter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = chapters[position].chapterNumber
                if (selected != chapterNumber) loadChapter(selected)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadChapter(chapNum: Int) {
        chapterNumber = chapNum
        val manga   = MangaRepository.getMangaById(mangaId) ?: return
        val chapter = MangaRepository.getChapter(mangaId, chapNum) ?: return
        val chapters = MangaRepository.getChaptersByMangaId(mangaId)

        // Cập nhật UI
        tvMangaName.text    = manga.name
        tvChapterInfo.text  = "Chương $chapNum / ${chapters.size}"
        tvChapterTitle.text = "Chương $chapNum: ${chapter.title}"

        // Nội dung ảnh hay text
        if (chapter.imageUrls.isNotEmpty()) {
            rvPages.visibility  = View.VISIBLE
            tvContent.visibility = View.GONE
            rvPages.layoutManager = LinearLayoutManager(requireContext())
            rvPages.adapter = PageAdapter(chapter.imageUrls)
        } else if (chapter.content.isNotEmpty()) {
            rvPages.visibility  = View.GONE
            tvContent.visibility = View.VISIBLE
            tvContent.text = chapter.content
        }

        // Nút prev/next
        val prevChap = chapters.filter { it.chapterNumber < chapNum }.maxByOrNull { it.chapterNumber }
        val nextChap = chapters.filter { it.chapterNumber > chapNum }.minByOrNull { it.chapterNumber }

        btnPrev.isEnabled = prevChap != null
        btnPrev.alpha     = if (prevChap != null) 1f else 0.4f

        btnNext.isEnabled = nextChap != null
        btnNext.alpha     = if (nextChap != null) 1f else 0.4f

        // Cập nhật spinner
        val index = chapters.indexOfFirst { it.chapterNumber == chapNum }
        if (index >= 0) spinnerChapter.setSelection(index)
    }

    private fun setupClickListeners() {
        val chapters = MangaRepository.getChaptersByMangaId(mangaId)

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnPrev.setOnClickListener {
            val prev = chapters.filter { it.chapterNumber < chapterNumber }
                .maxByOrNull { it.chapterNumber }
            prev?.let { loadChapter(it.chapterNumber) }
        }

        btnNext.setOnClickListener {
            val next = chapters.filter { it.chapterNumber > chapterNumber }
                .minByOrNull { it.chapterNumber }
            next?.let { loadChapter(it.chapterNumber) }
        }

        // Toggle cỡ chữ
        btnFontSize.setOnClickListener {
            fontSize = if (fontSize >= 22) 14 else fontSize + 2
            tvContent.textSize = fontSize.toFloat()
        }
    }
}
