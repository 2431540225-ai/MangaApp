package com.example.mangaapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.ui.detail.DetailFragment
import com.example.mangaapp.ui.list.MangaListAdapter

/**
 * Fragment hiển thị toàn bộ danh sách truyện dạng dọc.
 * Dùng chung cho cả "Mới Cập Nhật" lẫn "Bảng Xếp Hạng".
 *
 * Nhận dữ liệu qua arguments:
 *  - ARG_TITLE : tiêu đề hiển thị trên toolbar
 *  - ARG_LIST  : ArrayList<Manga> cần hiển thị
 */
class SeeAllFragment : Fragment() {

    companion object {
        private const val ARG_TITLE = "arg_title"
        private const val ARG_LIST  = "arg_list"

        fun newInstance(title: String, list: ArrayList<Manga>): SeeAllFragment {
            return SeeAllFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putSerializable(ARG_LIST, list)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_see_all, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val title = arguments?.getString(ARG_TITLE) ?: "Xem Tất Cả"
        val list  = arguments?.getSerializable(ARG_LIST) as? ArrayList<Manga> ?: arrayListOf()

        // Toolbar
        view.findViewById<TextView>(R.id.tv_title).text = title
        view.findViewById<ImageButton>(R.id.btn_back).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // RecyclerView — dùng MangaListAdapter dọc, hiện TẤT CẢ (không giới hạn)
        val adapter = MangaListAdapter(list) { manga ->
            val fragment = DetailFragment.newInstance(manga.firestoreId)
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit()
        }

        view.findViewById<RecyclerView>(R.id.rv_see_all).apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter  = adapter
        }
    }
}