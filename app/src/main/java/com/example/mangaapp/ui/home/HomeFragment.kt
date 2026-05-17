package com.example.mangaapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaapp.R
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.ThemeManager

class HomeFragment : Fragment() {

    private lateinit var vpBanner: ViewPager2
    private lateinit var llDots: LinearLayout
    private lateinit var rvLatest: RecyclerView
    private lateinit var rvRanking: RecyclerView
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var tvLatestSeeAll: TextView
    private lateinit var tvRankingSeeAll: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupBanner()
        setupLatestManga()
        setupRanking()
        setupClickListeners()
    }

    private fun initViews(view: View) {
        vpBanner        = view.findViewById(R.id.vp_banner)
        llDots          = view.findViewById(R.id.ll_dots)
        rvLatest        = view.findViewById(R.id.rv_latest)
        rvRanking       = view.findViewById(R.id.rv_ranking)
        btnThemeToggle  = view.findViewById(R.id.btn_theme_toggle)
        tvLatestSeeAll  = view.findViewById(R.id.tv_latest_see_all)
        tvRankingSeeAll = view.findViewById(R.id.tv_ranking_see_all)
    }

    private fun setupBanner() {
        val featuredList = MangaRepository.getFeaturedManga()
        val bannerAdapter = BannerAdapter(featuredList) { manga ->
            // Mở DetailActivity khi click banner
            // TODO: Người 3 sẽ implement DetailActivity
            // val intent = Intent(requireContext(), DetailActivity::class.java)
            // intent.putExtra("manga_id", manga.id)
            // startActivity(intent)
        }
        vpBanner.adapter = bannerAdapter

        // Tạo dots indicator
        setupDots(featuredList.size)

        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position, featuredList.size)
            }
        })
    }

    private fun setupDots(count: Int) {
        llDots.removeAllViews()
        repeat(count) { i ->
            val dot = View(requireContext()).apply {
                val size = if (i == 0) 24 else 16
                val params = LinearLayout.LayoutParams(size, 8).also {
                    it.marginEnd = 6
                }
                layoutParams = params
                setBackgroundColor(
                    if (i == 0) resources.getColor(R.color.primary, null)
                    else resources.getColor(R.color.light_text_secondary, null)
                )
            }
            llDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int, count: Int) {
        for (i in 0 until llDots.childCount) {
            val dot = llDots.getChildAt(i)
            val color = if (i == selected) R.color.primary else R.color.light_text_secondary
            dot.setBackgroundColor(resources.getColor(color, null))
            val size = if (i == selected) 24 else 16
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).also {
                it.width = size
            }
        }
    }

    private fun setupLatestManga() {
        val latestList = MangaRepository.getLatestManga()
        val adapter = MangaCardAdapter(latestList) { manga ->
            // TODO: Người 3 sẽ implement
            // startActivity(Intent(requireContext(), DetailActivity::class.java).apply {
            //     putExtra("manga_id", manga.id)
            // })
        }
        rvLatest.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        rvLatest.adapter = adapter
    }

    private fun setupRanking() {
        val rankingList = MangaRepository.getRankingManga()
        val adapter = RankingAdapter(rankingList) { manga ->
            // TODO: Navigate to detail
        }
        rvRanking.layoutManager = LinearLayoutManager(requireContext())
        rvRanking.isNestedScrollingEnabled = false
        rvRanking.adapter = adapter
    }

    private fun setupClickListeners() {
        // Toggle Dark/Light mode
        btnThemeToggle.setOnClickListener {
            ThemeManager.toggle()
            requireActivity().recreate()
        }

        // Xem tất cả mới cập nhật → chuyển sang tab List
        tvLatestSeeAll.setOnClickListener {
            // TODO: Chuyển sang tab danh sách
            // (requireActivity() as MainActivity).navigateTo(R.id.nav_list)
        }

        tvRankingSeeAll.setOnClickListener {
            // TODO: Chuyển sang tab danh sách
        }
    }
}
