package com.example.mangaapp.ui.list

import android.os.Bundle
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
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.repository.MangaRepository
import com.google.android.material.tabs.TabLayout

class ListFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var spinnerSort: Spinner
    private lateinit var llGenreTags: LinearLayout
    private lateinit var rvMangaList: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvResultCount: TextView
    private lateinit var btnViewGrid: ImageButton
    private lateinit var btnViewList: ImageButton

    private var isGridMode = true
    private var selectedCategory = MangaCategory.TRUYEN_TRANH
    private var selectedGenre = "Tất cả"
    private var selectedSort = "Mới nhất"

    // Cache toàn bộ list để filter local (tránh gọi Firestore liên tục)
    private var allMangaList: List<Manga> = emptyList()

    private lateinit var gridAdapter: MangaGridAdapter
    private lateinit var listAdapter: MangaListAdapter

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
        setupTabs()
        setupSort()
        setupGenreTags()
        setupViewModeToggle()
        loadMangaFromFirestore()
    }

    private fun initViews(view: View) {
        tabLayout     = view.findViewById(R.id.tab_layout)
        spinnerSort   = view.findViewById(R.id.spinner_sort)
        llGenreTags   = view.findViewById(R.id.ll_genre_tags)
        rvMangaList   = view.findViewById(R.id.rv_manga_list)
        layoutEmpty   = view.findViewById(R.id.layout_empty)
        tvResultCount = view.findViewById(R.id.tv_result_count)
        btnViewGrid   = view.findViewById(R.id.btn_view_grid)
        btnViewList   = view.findViewById(R.id.btn_view_list)
    }

    private fun loadMangaFromFirestore() {
        MangaRepository.getAllManga(
            onSuccess = { list ->
                if (!isAdded) return@getAllManga
                allMangaList = list
                applyFilters()
            },
            onError = { applyFilters() }
        )
    }

    private fun setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText("Truyện Tranh"))
        tabLayout.addTab(tabLayout.newTab().setText("Tiểu Thuyết"))

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedCategory = when (tab?.position) {
                    1    -> MangaCategory.TIEU_THUYET
                    else -> MangaCategory.TRUYEN_TRANH
                }
                selectedGenre = "Tất cả"
                setupGenreTags()
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapters() {
        gridAdapter = MangaGridAdapter(emptyList()) { manga -> navigateToDetail(manga) }
        listAdapter = MangaListAdapter(emptyList()) { manga -> navigateToDetail(manga) }
        rvMangaList.layoutManager = GridLayoutManager(requireContext(), 2)
        rvMangaList.adapter = gridAdapter
    }

    private fun setupSort() {
        val sortOptions = arrayOf("Mới nhất", "Xem nhiều", "Tên A-Z")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, sortOptions)
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
        MangaRepository.genres.forEach { genre ->
            val tag = TextView(requireContext()).apply {
                text = genre
                textSize = 12f
                setPadding(24, 10, 24, 10)
                setTextColor(
                    if (genre == selectedGenre) resources.getColor(R.color.white, null)
                    else resources.getColor(R.color.light_text_primary, null)
                )
                setBackgroundColor(
                    if (genre == selectedGenre) resources.getColor(R.color.primary, null)
                    else resources.getColor(R.color.light_background, null)
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 8 }
                setOnClickListener {
                    selectedGenre = genre
                    setupGenreTags()
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
        if (!isAdded) return

        // Lọc theo category
        var filtered = allMangaList.filter { it.category == selectedCategory }

        // Lọc theo thể loại
        if (selectedGenre != "Tất cả") {
            filtered = filtered.filter { it.genres.contains(selectedGenre) }
        }

        // Sắp xếp
        filtered = when (selectedSort) {
            "Xem nhiều" -> filtered.sortedByDescending { it.totalViews }
            "Tên A-Z"   -> filtered.sortedBy { it.name }
            else        -> filtered.sortedByDescending { it.createdAt }
        }

        tvResultCount.visibility = if (selectedGenre != "Tất cả") View.VISIBLE else View.GONE
        tvResultCount.text = "Tìm thấy ${filtered.size} kết quả"

        if (filtered.isEmpty()) {
            rvMangaList.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
        } else {
            rvMangaList.visibility = View.VISIBLE
            layoutEmpty.visibility = View.GONE
        }

        if (isGridMode) gridAdapter.updateList(filtered)
        else listAdapter.updateList(filtered)
    }

    private fun navigateToDetail(manga: Manga) {
        val fragment = com.example.mangaapp.ui.detail.DetailFragment.newInstance(manga.firestoreId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}