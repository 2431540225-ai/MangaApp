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

class FavoritesFragment : Fragment() {

    private lateinit var rvFavorites: RecyclerView
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyMsg: TextView
    private lateinit var progressLoading: ProgressBar

    private lateinit var adapter: FavoritesAdapter

    private var allManga: List<Manga> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_favorites, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupAdapter()

        if (!UserSession.isLoggedIn) {
            showEmpty("Đăng nhập để xem danh sách yêu thích")
            return
        }

        loadAllManga()
    }

    private fun initViews(view: View) {
        rvFavorites    = view.findViewById(R.id.rv_favorites)
        layoutEmpty    = view.findViewById(R.id.layout_empty)
        tvEmptyMsg     = view.findViewById(R.id.tv_empty_msg)
        progressLoading = view.findViewById(R.id.progress_loading)
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

        val favIds = MangaRepository.getFavoriteIds(uid)
        val folIds = MangaRepository.getFollowingIds(uid)
        // Kết hợp lại để lấy danh sách đầy đủ
        val ids = (favIds + folIds).distinct()

        val filtered = allManga.filter { ids.contains(it.firestoreId) }

        if (filtered.isEmpty()) {
            showEmpty("Chưa có truyện yêu thích\nNhấn ❤ trên truyện để thêm vào đây")
        } else {
            layoutEmpty.visibility = View.GONE
            rvFavorites.visibility = View.VISIBLE
            adapter.updateList(filtered)
        }
    }

    private fun removeManga(manga: Manga) {
        val uid = UserSession.firebaseUid ?: return
        MangaRepository.removeFavorite(uid, manga.firestoreId,
            onSuccess = {
                // Xóa cả trường hợp nằm bên follow để tránh dính lại
                MangaRepository.removeFollowing(uid, manga.firestoreId, onSuccess = {}, onError = {})
                Toast.makeText(context, "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show()
                filterAndShow()
            },
            onError = { Toast.makeText(context, "Lỗi, thử lại sau", Toast.LENGTH_SHORT).show() }
        )
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
