package com.example.mangaapp.ui.search

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.ui.detail.DetailFragment
import com.example.mangaapp.ui.list.MangaListAdapter
import com.example.mangaapp.utils.EventTracker

/**
 * Màn hình tìm kiếm truyện theo tên hoặc tác giả.
 * Dùng chung MangaRepository.searchManga() (đã có sẵn) + MangaListAdapter
 * (đã dùng ở SeeAllFragment / ListFragment) để đồng bộ UI với phần còn lại của app.
 *
 * Có debounce 400ms để tránh gọi Firestore liên tục mỗi lần gõ phím.
 */
class SearchFragment : Fragment() {

    private lateinit var etSearch: EditText
    private lateinit var btnClear: ImageButton
    private lateinit var tvResultCount: TextView
    private lateinit var rvResults: RecyclerView
    private lateinit var layoutHint: LinearLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var progressBar: ProgressBar

    private lateinit var adapter: MangaListAdapter

    private val handler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val DEBOUNCE_MS = 400L

    // Đếm để tránh race-condition: chỉ hiển thị kết quả của lần gõ mới nhất
    private var searchToken = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapter()
        setupSearchInput()
    }

    private fun initViews(view: View) {
        etSearch      = view.findViewById(R.id.et_search)
        btnClear      = view.findViewById(R.id.btn_clear)
        tvResultCount = view.findViewById(R.id.tv_result_count)
        rvResults     = view.findViewById(R.id.rv_search_results)
        layoutHint    = view.findViewById(R.id.layout_hint)
        layoutEmpty   = view.findViewById(R.id.layout_empty)
        progressBar   = view.findViewById(R.id.progress_bar)
    }

    private fun setupAdapter() {
        adapter = MangaListAdapter(emptyList()) { manga -> navigateToDetail(manga) }
        rvResults.layoutManager = LinearLayoutManager(requireContext())
        rvResults.adapter = adapter
    }

    private fun setupSearchInput() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim().orEmpty()
                btnClear.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                // Hủy lần tìm kiếm đang chờ trước đó (debounce)
                searchRunnable?.let { handler.removeCallbacks(it) }

                if (query.isEmpty()) {
                    showHintState()
                    return
                }

                val runnable = Runnable { performSearch(query) }
                searchRunnable = runnable
                handler.postDelayed(runnable, DEBOUNCE_MS)
            }
        })

        etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchRunnable?.let { handler.removeCallbacks(it) }
                performSearch(etSearch.text.toString().trim())
                true
            } else {
                false
            }
        }

        btnClear.setOnClickListener {
            etSearch.text.clear()
            showHintState()
        }
    }

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            showHintState()
            return
        }
        if (!isAdded) return

        showLoadingState()
        val currentToken = ++searchToken

        EventTracker.logEvent("search_query", mapOf("query" to query))

        MangaRepository.searchManga(
            query = query,
            onSuccess = { list ->
                if (!isAdded || currentToken != searchToken) return@searchManga
                showResults(list)
            },
            onError = {
                if (!isAdded || currentToken != searchToken) return@searchManga
                showResults(emptyList())
            }
        )
    }

    private fun showHintState() {
        progressBar.visibility = View.GONE
        rvResults.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        layoutHint.visibility = View.VISIBLE
        tvResultCount.visibility = View.GONE
    }

    private fun showLoadingState() {
        layoutHint.visibility = View.GONE
        layoutEmpty.visibility = View.GONE
        rvResults.visibility = View.GONE
        tvResultCount.visibility = View.GONE
        progressBar.visibility = View.VISIBLE
    }

    private fun showResults(list: List<Manga>) {
        progressBar.visibility = View.GONE
        layoutHint.visibility = View.GONE

        if (list.isEmpty()) {
            rvResults.visibility = View.GONE
            layoutEmpty.visibility = View.VISIBLE
            tvResultCount.visibility = View.GONE
        } else {
            layoutEmpty.visibility = View.GONE
            rvResults.visibility = View.VISIBLE
            tvResultCount.visibility = View.VISIBLE
            tvResultCount.text = "Tìm thấy ${list.size} kết quả"
            adapter.updateList(list)
        }
    }

    private fun navigateToDetail(manga: Manga) {
        val fragment = DetailFragment.newInstance(manga.firestoreId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchRunnable?.let { handler.removeCallbacks(it) }
    }
}