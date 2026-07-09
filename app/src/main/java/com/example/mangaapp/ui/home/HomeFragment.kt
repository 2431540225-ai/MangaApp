package com.example.mangaapp.ui.home

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.utils.ThemeManager
import com.example.mangaapp.ui.detail.DetailFragment

class HomeFragment : Fragment() {

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var handler: Handler
    private lateinit var runnable: Runnable
    private lateinit var vpBanner: ViewPager2
    private lateinit var llDots: LinearLayout
    private lateinit var rvLatest: RecyclerView
    private lateinit var rvRanking: RecyclerView
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var tvLatestSeeAll: TextView
    private lateinit var tvRankingSeeAll: TextView

    private var realBannerCount = 0

    // Giữ list đầy đủ để truyền sang SeeAllFragment khi bấm "Xem Tất Cả"
    private var fullLatestList: ArrayList<Manga> = arrayListOf()
    private var fullRankingList: ArrayList<Manga> = arrayListOf()

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
        setupClickListeners()
        observeViewModel()
        viewModel.loadHomeData()
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

    // ─── Observe ViewModel ───
    //  - Lần đầu mở app: gọi Firebase → LiveData cập nhật → UI render
    //  - Xoay màn hình: ViewModel vẫn còn data → observe() nhận ngay → UI render KHÔNG gọi Firebase lại
    private fun observeViewModel() {
        viewModel.featuredList.observe(viewLifecycleOwner) { list ->
            if (list != null && list.isNotEmpty()) setupBannerUI(list)
        }
        viewModel.latestList.observe(viewLifecycleOwner) { list ->
            if (list != null) {
                fullLatestList = ArrayList(list)
                setupLatestUI(list)
            }
        }
        viewModel.rankingList.observe(viewLifecycleOwner) { list ->
            if (list != null) {
                fullRankingList = ArrayList(list)
                setupRankingUI(list)
            }
        }
    }

    // ─── Banner ───────────────────────────────────────────────────────────────

    private fun setupBannerUI(featuredList: List<Manga>) {
        if (!isAdded) return
        realBannerCount = featuredList.size
        if (realBannerCount == 0) return

        val LOOP_MULTIPLIER = 100
        val infiniteList = MutableList(realBannerCount * LOOP_MULTIPLIER) { i ->
            featuredList[i % realBannerCount]
        }

        val bannerAdapter = BannerAdapter(infiniteList) { manga ->
            navigateToDetail(manga.firestoreId)
        }

        vpBanner.offscreenPageLimit = 3
        vpBanner.setPageTransformer { page, position ->
            val absPos = Math.abs(position)
            val scale = 1f - (absPos * 0.12f)
            page.scaleX = scale
            page.scaleY = scale
            page.alpha = 1f - (absPos * 0.4f)
        }
        vpBanner.adapter = bannerAdapter

        val startPosition = (LOOP_MULTIPLIER / 2) * realBannerCount
        vpBanner.setCurrentItem(startPosition, false)

        setupDots(realBannerCount)
        updateDots(0, realBannerCount)

        vpBanner.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position % realBannerCount, realBannerCount)
            }
        })

        if (::handler.isInitialized && ::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                if (!isAdded) return
                val next = vpBanner.currentItem + 1
                if (next >= realBannerCount * LOOP_MULTIPLIER - realBannerCount) {
                    vpBanner.setCurrentItem((LOOP_MULTIPLIER / 2) * realBannerCount, false)
                } else {
                    vpBanner.setCurrentItem(next, true)
                }
                handler.postDelayed(this, 3000)
            }
        }
        handler.postDelayed(runnable, 3000)
    }

    // ─── Mới Cập Nhật — ngang, giới hạn 5, dùng MangaCardAdapter như cũ ──────

    private fun setupLatestUI(list: List<Manga>) {
        if (!isAdded) return
        val adapter = MangaCardAdapter(list.take(5)) { manga ->
            navigateToDetail(manga.firestoreId)
        }
        rvLatest.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        rvLatest.adapter = adapter
        tvLatestSeeAll.visibility = if (list.size > 5) View.VISIBLE else View.GONE
    }

    // ─── Bảng Xếp Hạng — dọc, giới hạn 5 ────────────────────────────────────

    private fun setupRankingUI(list: List<Manga>) {
        if (!isAdded) return
        val adapter = RankingAdapter(list.take(5)) { manga ->
            navigateToDetail(manga.firestoreId)
        }
        rvRanking.layoutManager = LinearLayoutManager(requireContext())
        rvRanking.isNestedScrollingEnabled = false
        rvRanking.adapter = adapter
        tvRankingSeeAll.visibility = if (list.size > 5) View.VISIBLE else View.GONE
    }

    // ─── Click listeners ──────────────────────────────────────────────────────

    private fun setupClickListeners() {
        updateThemeIcon()
        btnThemeToggle.setOnClickListener {
            ThemeManager.toggle()
            requireActivity().recreate()
        }

        // Bấm "Xem Tất Cả" → chuyển sang SeeAllFragment với toàn bộ list
        tvLatestSeeAll.setOnClickListener {
            navigateToSeeAll("Mới Cập Nhật", fullLatestList)
        }
        tvRankingSeeAll.setOnClickListener {
            navigateToSeeAll("Bảng Xếp Hạng", fullRankingList)
        }
    }

    private fun updateThemeIcon() {
        val iconRes = if (ThemeManager.isDarkMode()) R.drawable.ic_light_mode
        else R.drawable.ic_dark_mode
        btnThemeToggle.setImageResource(iconRes)
    }

    // ─── Navigation ───────────────────────────────────────────────────────────

    private fun navigateToDetail(firestoreId: String) {
        val fragment = DetailFragment.newInstance(firestoreId)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSeeAll(title: String, list: ArrayList<Manga>) {
        val fragment = SeeAllFragment.newInstance(title, list)
        parentFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // ─── Dots ─────────────────────────────────────────────────────────────────

    private fun setupDots(count: Int) {
        llDots.removeAllViews()
        repeat(count) { i ->
            val dot = View(requireContext()).apply {
                val width = if (i == 0) 24 else 8
                layoutParams = LinearLayout.LayoutParams(
                    (width * resources.displayMetrics.density).toInt(),
                    (8 * resources.displayMetrics.density).toInt()
                ).also { it.marginEnd = (6 * resources.displayMetrics.density).toInt() }
                setBackgroundResource(
                    if (i == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
                )
            }
            llDots.addView(dot)
        }
    }

    private fun updateDots(selected: Int, count: Int) {
        if (!isAdded) return
        for (i in 0 until llDots.childCount) {
            val dot = llDots.getChildAt(i)
            val isActive = (i == selected)
            dot.setBackgroundResource(
                if (isActive) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
            val width = if (isActive) 24 else 8
            dot.layoutParams = (dot.layoutParams as LinearLayout.LayoutParams).also {
                it.width = (width * resources.displayMetrics.density).toInt()
            }
        }
    }

    fun applyGradientToTitle(textView: TextView) {
        textView.post {
            val width = textView.width.toFloat()
            if (width <= 0f) return@post
            val colorStart = requireContext().getColor(R.color.primary)
            val colorEnd   = requireContext().getColor(R.color.gradient_end)
            textView.paint.shader = LinearGradient(
                0f, 0f, width, 0f,
                colorStart, colorEnd,
                Shader.TileMode.CLAMP
            )
            textView.invalidate()
        }
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    override fun onDestroyView() {
        super.onDestroyView()
        if (::handler.isInitialized && ::runnable.isInitialized) {
            handler.removeCallbacks(runnable)
        }
    }
}