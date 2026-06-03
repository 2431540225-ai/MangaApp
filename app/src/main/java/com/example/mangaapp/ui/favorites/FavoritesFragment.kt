package com.example.mangaapp.ui.favorites

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.UserSession
import com.google.android.material.tabs.TabLayout

class FavoritesFragment : Fragment() {

    private lateinit var tabFavorites: TabLayout
    private lateinit var rvFavorites: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyMsg: TextView
    private lateinit var progressLoading: ProgressBar

    private lateinit var adapter: FavoritesAdapter

    // 0 = Yêu thích, 1 = Theo dõi
    private var currentTab = 0
    private var allManga: List<Manga> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_favorites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupTabs()
        setupAdapter()

        if (!UserSession.isLoggedIn) {
            showEmpty("Đăng nhập để xem danh sách yêu thích")
            return
        }

        loadAllManga()
    }

    private fun initViews(view: View) {
        tabFavorites   = view.findViewById(R.id.tab_favorites)
        rvFavorites    = view.findViewById(R.id.rv_favorites)
        layoutEmpty    = view.findViewById(R.id.layout_empty)
        tvEmptyMsg     = view.findViewById(R.id.tv_empty_msg)
        progressLoading = view.findViewById(R.id.progress_loading)
    }

    private fun setupTabs() {
        tabFavorites.addTab(tabFavorites.newTab().setText("❤️ Yêu thích"))
        tabFavorites.addTab(tabFavorites.newTab().setText("🔔 Theo dõi"))

        tabFavorites.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterAndShow()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupAdapter() {
        adapter = FavoritesAdapter(
            emptyList(),
            onClick = { manga -> navigateToDetail(manga) },
            onRemove = { manga -> removeManga(manga) }
        )
        rvFavorites.layoutManager = LinearLayoutManager(requireContext())
        rvFavorites.adapter = adapter
    }

    private fun loadAllManga() {
        progressLoading.visibility = View.VISIBLE
        MangaRepository.getAllManga(
            onSuccess = { list ->
                if (!isAdded) return@getAllManga
                allManga = list
                progressLoading.visibility = View.GONE
                filterAndShow()
            },
            onError = {
                if (!isAdded) return@getAllManga
                progressLoading.visibility = View.GONE
                filterAndShow()
            }
        )
    }

    private fun filterAndShow() {
        val uid = UserSession.firebaseUid ?: run {
            showEmpty("Đăng nhập để xem danh sách yêu thích")
            return
        }

        val ids: List<String> = if (currentTab == 0) {
            MangaRepository.getFavoriteIds(uid)
        } else {
            MangaRepository.getFollowingIds(uid)
        }

        val filtered = allManga.filter { ids.contains(it.firestoreId) }

        if (filtered.isEmpty()) {
            val msg = if (currentTab == 0)
                "Chưa có truyện yêu thích\nNhấn ❤ trên truyện để thêm vào đây"
            else
                "Chưa theo dõi truyện nào\nNhấn 🔔 trên truyện để theo dõi"
            showEmpty(msg)
        } else {
            layoutEmpty.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
            adapter.updateList(filtered)
        }
    }

    private fun removeManga(manga: Manga) {
        val uid = UserSession.firebaseUid ?: return
        if (currentTab == 0) {
            MangaRepository.removeFavorite(uid, manga.firestoreId,
                onSuccess = {
                    Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()
                    filterAndShow()
                },
                onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
            )
        } else {
            MangaRepository.removeFollowing(uid, manga.firestoreId,
                onSuccess = {
                    Toast.makeText(context, "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show()
                    filterAndShow()
                },
                onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
            )
        }
    }

    private fun showEmpty(msg: String) {
        rvFavorites.visibility = View.GONE
        layoutEmpty.visibility = View.VISIBLE
        tvEmptyMsg.text = msg
    }

    private fun navigateToDetail(manga: Manga) {
        val fragment = com.example.mangaapp.ui.detail.DetailFragment.newInstance(manga.firestoreId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }
}
