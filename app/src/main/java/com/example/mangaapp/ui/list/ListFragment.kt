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
import com.example.mangaapp.utils.EventTracker
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

    companion object {
        private const val ARG_SORT = "initial_sort"

        /** @param initialSort "Mới nhất" hoặc "Xem nhiều" */
        fun newInstance(initialSort: String = "Mới nhất"): ListFragment {
            val f = ListFragment()
            f.arguments = Bundle().apply { putString(ARG_SORT, initialSort) }
            return f
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // 4.3: Nhận lại dữ liệu restore (sau khi xoay màn hình v.v.)
        if (savedInstanceState != null) {
            isGridMode = savedInstanceState.getBoolean("state_isGridMode", true)
            val catName = savedInstanceState.getString("state_selectedCategory", MangaCategory.TRUYEN_TRANH.name)
            selectedCategory = MangaCategory.valueOf(catName)
            selectedGenre = savedInstanceState.getString("state_selectedGenre", "Tất cả")
            selectedSort = savedInstanceState.getString("state_selectedSort", "Mới nhất")
        } else {
            arguments?.getString("initial_sort")?.let { selectedSort = it }
        }

        initViews(view)
        setupAdapters()
        setupTabs()
        setupSort()
        setupGenreTags()
        setupViewModeToggle()
        loadMangaFromFirestore()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 4.3: Lưu trữ dữ liệu khi bị hủy để khôi phục
        outState.putBoolean("state_isGridMode", isGridMode)
        outState.putString("state_selectedCategory", selectedCategory.name)
        outState.putString("state_selectedGenre", selectedGenre)
        outState.putString("state_selectedSort", selectedSort)
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

        // Chọn lại tab khi khôi phục state 4.3
        val tabIndex = if (selectedCategory == MangaCategory.TIEU_THUYET) 1 else 0
        tabLayout.getTabAt(tabIndex)?.select()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                selectedCategory = when (tab?.position) {
                    1    -> MangaCategory.TIEU_THUYET
                    else -> MangaCategory.TRUYEN_TRANH
                }
                selectedGenre = "Tất cả"
                EventTracker.logEvent("category_tab_changed", mapOf("category" to selectedCategory.name)) // 4.1
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

        // Đặt vị trí spinner theo selectedSort hiện tại (có thể được set từ args)
        val initialIndex = sortOptions.indexOf(selectedSort).coerceAtLeast(0)
        spinnerSort.setSelection(initialIndex, false)

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
                setPadding(24, 12, 24, 12)
                setTextColor(
                    if (genre == selectedGenre) resources.getColor(R.color.white, null)
                    else resources.getColor(R.color.color_text_secondary, null)
                )
                setBackgroundResource(
                    if (genre == selectedGenre) R.drawable.bg_chip_active
                    else R.drawable.bg_chip_inactive
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 8 }
                setOnClickListener {
                    selectedGenre = genre
                    EventTracker.logEvent("genre_clicked", mapOf("genre" to genre)) // 4.1
                    setupGenreTags()
                    applyFilters()
                }
            }
            llGenreTags.addView(tag)
        }
    }

    private fun setupViewModeToggle() {
        // Set initial state
        if (isGridMode) {
            btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_active)
            btnViewGrid.setColorFilter(resources.getColor(R.color.primary, null))
            btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_inactive)
            btnViewList.setColorFilter(resources.getColor(R.color.color_inactive_icon, null))
        } else {
            btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_active)
            btnViewList.setColorFilter(resources.getColor(R.color.primary, null))
            btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_inactive)
            btnViewGrid.setColorFilter(resources.getColor(R.color.color_inactive_icon, null))
        }

        btnViewGrid.setOnClickListener {
            isGridMode = true
            rvMangaList.layoutManager = GridLayoutManager(requireContext(), 2)
            rvMangaList.adapter = gridAdapter
            btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_active)
            btnViewGrid.setColorFilter(resources.getColor(R.color.primary, null))
            btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_inactive)
            btnViewList.setColorFilter(resources.getColor(R.color.color_inactive_icon, null))
        }
        btnViewList.setOnClickListener {
            isGridMode = false
            rvMangaList.layoutManager = LinearLayoutManager(requireContext())
            rvMangaList.adapter = listAdapter
            btnViewList.setBackgroundResource(R.drawable.bg_view_toggle_active)
            btnViewList.setColorFilter(resources.getColor(R.color.primary, null))
            btnViewGrid.setBackgroundResource(R.drawable.bg_view_toggle_inactive)
            btnViewGrid.setColorFilter(resources.getColor(R.color.color_inactive_icon, null))
        }
    }

    private fun applyFilters() {
        if (!isAdded) return

        // 4.1 Logs events locally
        EventTracker.logEvent("filter_applied", mapOf(
            "category" to selectedCategory.name,
            "genre" to selectedGenre, "sort" to selectedSort, "isGridMode" to isGridMode.toString()
        ))

        // 4.4 Các thao tác quá nặng, dùng Thread hoặc Service
        Thread {
            try {
                // Giả lập xử lý nặng / data khổng lồ
                Thread.sleep(50) 
                
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

                // Trả về UI Thread
                activity?.runOnUiThread {
                    if (!isAdded) return@runOnUiThread
                    
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
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun navigateToDetail(manga: Manga) {
        val fragment = com.example.mangaapp.ui.detail.DetailFragment.newInstance(manga.firestoreId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}