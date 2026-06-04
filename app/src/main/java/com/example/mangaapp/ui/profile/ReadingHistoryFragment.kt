package com.example.mangaapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.ui.profile.ReadingHistoryAdapter
import com.example.mangaapp.repository.ReadingHistoryRepository
import com.example.mangaapp.ui.detail.DetailFragment
import com.google.android.material.snackbar.Snackbar

class ReadingHistoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutEmpty: View
    private lateinit var tvHistoryCount: TextView
    private lateinit var btnClearAll: TextView
    private lateinit var adapter: ReadingHistoryAdapter

    private val repository = ReadingHistoryRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reading_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupToolbar(view)
        setupRecyclerView()
        loadHistory()
    }

    private fun initViews(view: View) {
        recyclerView   = view.findViewById(R.id.recyclerHistory)
        progressBar    = view.findViewById(R.id.progressBar)
        layoutEmpty    = view.findViewById(R.id.layoutEmpty)
        tvHistoryCount = view.findViewById(R.id.tvHistoryCount)
        btnClearAll    = view.findViewById(R.id.btnClearAll)
    }

    private fun setupToolbar(view: View) {
        view.findViewById<android.widget.ImageButton>(R.id.btnBack).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        btnClearAll.setOnClickListener { confirmClearAll() }
    }

    private fun setupRecyclerView() {
        adapter = ReadingHistoryAdapter(
            onItemClick = { history ->
                // Mở DetailFragment, truyền firestoreId (chính là storyId)
                val fragment = DetailFragment.newInstance(firestoreId = history.storyId)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onDeleteClick = { history ->
                confirmDeleteItem(history.storyId, history.storyTitle)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    private fun loadHistory() {
        showLoading(true)
        repository.getReadingHistory(
            onSuccess = { list ->
                showLoading(false)
                if (list.isEmpty()) {
                    showEmptyState()
                } else {
                    showList()
                    adapter.submitList(list)
                    tvHistoryCount.text = "${list.size} truyện đã đọc"
                }
            },
            onFailure = { e ->
                showLoading(false)
                Snackbar.make(
                    recyclerView,
                    "Không tải được lịch sử: ${e.message}",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        )
    }

    private fun confirmDeleteItem(storyId: String, storyTitle: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Xoá lịch sử")
            .setMessage("Xoá \"$storyTitle\" khỏi lịch sử đọc?")
            .setPositiveButton("Xoá") { _, _ ->
                repository.deleteHistory(storyId,
                    onSuccess = {
                        adapter.removeItem(storyId)
                        updateCountAfterDelete()
                    },
                    onFailure = {
                        Snackbar.make(recyclerView, "Xoá thất bại!", Snackbar.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun confirmClearAll() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xoá tất cả")
            .setMessage("Bạn có chắc muốn xoá toàn bộ lịch sử đọc không?")
            .setPositiveButton("Xoá tất cả") { _, _ ->
                repository.clearAllHistory(
                    onSuccess = { loadHistory() },
                    onFailure = {
                        Snackbar.make(recyclerView, "Xoá thất bại!", Snackbar.LENGTH_SHORT).show()
                    }
                )
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun updateCountAfterDelete() {
        val currentCount = adapter.itemCount
        if (currentCount == 0) showEmptyState()
        else tvHistoryCount.text = "$currentCount truyện đã đọc"
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility  = if (isLoading) View.VISIBLE else View.GONE
        recyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun showEmptyState() {
        layoutEmpty.visibility  = View.VISIBLE
        recyclerView.visibility = View.GONE
        tvHistoryCount.text     = "0 truyện đã đọc"
        btnClearAll.visibility  = View.GONE
    }

    private fun showList() {
        layoutEmpty.visibility  = View.GONE
        recyclerView.visibility = View.VISIBLE
        btnClearAll.visibility  = View.VISIBLE
    }

    companion object {
        fun newInstance() = ReadingHistoryFragment()
    }
}