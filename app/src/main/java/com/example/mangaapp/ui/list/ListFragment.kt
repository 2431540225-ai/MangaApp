package com.example.mangaapp.ui.list

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.ui.detail.DetailFragment

class ListFragment : Fragment() {

    // Views
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageButton
    private lateinit var spinnerSort: Spinner
    private lateinit var llGenreTags: LinearLayout
    private lateinit var rvMangaList: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvResultCount: TextView
    private lateinit var btnViewGrid: ImageButton
    private lateinit var btnViewList: ImageButton

    // State
    private var isGridMode = true
    private var selectedGenre = "Tất cả"
    private var selectedSort = "Mới nhất"
    private var currentQuery = ""

    // Adapters
    private lateinit var gridAdapter: MangaGridAdapter
    private lateinit var listAdapter: MangaListAdapter

    // Data
    private var allManga = MangaRepository.getAllManga()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupAdapters()
        setupSearch()
        setupSort()
        setupGenreTags()
        setupViewModeToggle()
        loadManga()
    }

    private fun initViews(view: View) {
        etSearch       = view.findViewById(R.id.et_search)
        btnClearSearch = view.findViewById(R.id.btn_clear_search)
        spinnerSort    = view.findViewById(R.id.spinner_sort)
        llGenreTags    = view.findViewById(R.id.ll_genre_tags)
        rvMangaList    = view.findViewById(R.id.rv_manga_list)
        layoutEmpty    = view.findViewById(R.id.layout_empty)
        tvResultCount  = view.findViewById(R.id.tv_result_count)
        btnViewGrid    = view.findViewById(R.id.btn_view_grid)
        btnViewList    = view.findViewById(R.id.btn_view_list)
    }

    private fun setupAdapters() {
        gridAdapter = MangaGridAdapter(allManga) { manga -> navigateToDetail(manga) }
        listAdapter = MangaListAdapter(allManga) { manga -> navigateToDetail(manga) }

        // Mặc định dạng grid 2 cột
        rvMangaList.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMangaList.adapter = gridAdapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentQuery = s.toString()
                btnClearSearch.visibility = if (currentQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilters()
            }
        })

        btnClearSearch.setOnClickListener {
            etSearch.setText("")
            currentQuery = ""
            btnClearSearch.visibility = View.GONE
        }
    }

    private fun setupSort() {
        val sortOptions = arrayOf("Mới nhất", "Xem nhiều", "Tên A-Z")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSort.adapter = adapter

        spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSort = sortOptions[position]
                applyFilters()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupGenreTags() {
        llGenreTags.removeAllViews()
        val genres = MangaRepository.genres

        genres.forEach { genre ->
            val tag = TextView(requireContext()).apply {
                text = genre
                textSize = 13f
                setPadding(28, 12, 28, 12)
                setTextColor(
                    if (genre == selectedGenre)
                        resources.getColor(R.color.white, null)
                    else
                        resources.getColor(R.color.light_text_primary, null)
                )
                setBackgroundColor(
                    if (genre == selectedGenre)
                        resources.getColor(R.color.primary, null)
                    else
                        resources.getColor(R.color.light_surface, null)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 8 }

                setOnClickListener {
                    selectedGenre = genre
                    setupGenreTags() // Refresh tags
                    applyFilters()
                }
            }
            llGenreTags.addView(tag)
        }
    }

    private fun setupViewModeToggle() {
        btnViewGrid.setOnClickListener {
            isGridMode = true
            rvMangaList.layoutManager = GridLayoutManager(requireContext(), 2)
            rvMangaList.adapter = gridAdapter
            btnViewGrid.setBackgroundColor(resources.getColor(R.color.primary, null))
            btnViewList.setBackgroundColor(resources.getColor(R.color.light_surface, null))
        }

        btnViewList.setOnClickListener {
            isGridMode = false
            rvMangaList.layoutManager = LinearLayoutManager(requireContext())
            rvMangaList.adapter = listAdapter
            btnViewList.setBackgroundColor(resources.getColor(R.color.primary, null))
            btnViewGrid.setBackgroundColor(resources.getColor(R.color.light_surface, null))
        }
    }

    private fun applyFilters() {
        var filtered = allManga

        // Lọc theo thể loại
        if (selectedGenre != "Tất cả") {
            filtered = filtered.filter { it.genres.contains(selectedGenre) }
        }

        // Lọc theo tìm kiếm
        if (currentQuery.isNotBlank()) {
            filtered = filtered.filter {
                it.name.contains(currentQuery, ignoreCase = true) ||
                        it.author.contains(currentQuery, ignoreCase = true)
            }
        }

        // Sắp xếp
        filtered = when (selectedSort) {
            "Xem nhiều" -> filtered.sortedByDescending { it.totalViews }
            "Tên A-Z"   -> filtered.sortedBy { it.name }
            else        -> filtered.sortedByDescending { it.createdAt }
        }

        // Cập nhật UI
        updateResultCount(filtered.size)
        showEmptyOrList(filtered)

        if (isGridMode) gridAdapter.updateList(filtered)
        else listAdapter.updateList(filtered)
    }

    private fun updateResultCount(count: Int) {
        if (currentQuery.isNotBlank() || selectedGenre != "Tất cả") {
            tvResultCount.visibility = View.VISIBLE
            tvResultCount.text = "Tìm thấy $count kết quả"
        } else {
            tvResultCount.visibility = View.GONE
        }
    }

    private fun showEmptyOrList(list: List<Manga>) {
        if (list.isEmpty()) {
            rvMangaList.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvMangaList.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun loadManga() {
        applyFilters()
    }

    private fun navigateToDetail(manga: Manga) {
        val fragment = DetailFragment.newInstance(manga.id)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
