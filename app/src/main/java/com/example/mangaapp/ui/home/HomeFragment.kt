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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.mangaapp.R
import com.example.mangaapp.repository.MangaRepository
import com.example.mangaapp.utils.ThemeManager
import com.example.mangaapp.ui.detail.DetailFragment

class HomeFragment : Fragment() {
    private val handler = Handler(Looper.getMainLooper())
    private var autoScrollRunnable: Runnable? = null
    private lateinit var vpBanner: ViewPager2
    private lateinit var llDots: LinearLayout
    private lateinit var rvLatest: RecyclerView
    private lateinit var rvRanking: RecyclerView
    private lateinit var btnThemeToggle: ImageButton
    private lateinit var btnHomeCheckin: TextView
    private lateinit var tvLatestSeeAll: TextView
    private lateinit var tvRankingSeeAll: TextView

    private var realBannerCount = 0

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
        checkDailyCheckIn()
    }

    private fun initViews(view: View) {
        vpBanner        = view.findViewById(R.id.vp_banner)
        llDots          = view.findViewById(R.id.ll_dots)
        rvLatest        = view.findViewById(R.id.rv_latest)
        rvRanking       = view.findViewById(R.id.rv_ranking)
        btnThemeToggle  = view.findViewById(R.id.btn_theme_toggle)
        btnHomeCheckin  = view.findViewById(R.id.btn_home_checkin)
        tvLatestSeeAll  = view.findViewById(R.id.tv_latest_see_all)
        tvRankingSeeAll = view.findViewById(R.id.tv_ranking_see_all)
    }

    private fun checkDailyCheckIn() {
        if (!com.example.mangaapp.utils.UserSession.isLoggedIn) return

        com.example.mangaapp.repository.CheckInRepository.getCheckInStatus { status ->
            if (isAdded && !status.alreadyCheckedInToday) {
                com.example.mangaapp.ui.checkin.DailyCheckInDialog.show(parentFragmentManager)
            }
        }
    }

    private fun setupBanner() {
        MangaRepository.getFeaturedManga(
            onSuccess = { featuredList ->
                if (!isAdded) return@getFeaturedManga
                realBannerCount = featuredList.size
                if (realBannerCount == 0) return@getFeaturedManga

                val LOOP_MULTIPLIER = 100
                val infiniteList = MutableList(realBannerCount * LOOP_MULTIPLIER) { i ->
                    featuredList[i % realBannerCount]
                }

                val bannerAdapter = BannerAdapter(infiniteList) { manga ->
                    val fragment = DetailFragment.newInstance(manga.firestoreId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .addToBackStack(null)
                        .commit()
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

                autoScrollRunnable = object : Runnable {
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
                handler.postDelayed(autoScrollRunnable!!, 3000)
            },
            onError = { /* banner trống nếu lỗi */ }
        )
    }

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

    private fun setupLatestManga() {
        MangaRepository.getLatestManga(
            onSuccess = { latestList ->
                if (!isAdded) return@getLatestManga
                val adapter = MangaCardAdapter(latestList) { manga ->
                    val fragment = DetailFragment.newInstance(manga.firestoreId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                rvLatest.layoutManager = LinearLayoutManager(
                    requireContext(), LinearLayoutManager.HORIZONTAL, false
                )
                rvLatest.adapter = adapter
            },
            onError = { }
        )
    }

    private fun setupRanking() {
        MangaRepository.getRankingManga(
            onSuccess = { rankingList ->
                if (!isAdded) return@getRankingManga
                val adapter = RankingAdapter(rankingList) { manga ->
                    val fragment = DetailFragment.newInstance(manga.firestoreId)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                rvRanking.layoutManager = LinearLayoutManager(requireContext())
                rvRanking.isNestedScrollingEnabled = false
                rvRanking.adapter = adapter
            },
            onError = { }
        )
    }

    private fun setupClickListeners() {
        updateThemeIcon()
        btnThemeToggle.setOnClickListener {
            ThemeManager.toggle()
            requireActivity().recreate()
        }
        btnHomeCheckin.setOnClickListener {
            if (!com.example.mangaapp.utils.UserSession.isLoggedIn) {
                android.widget.Toast.makeText(requireContext(), "Vui lòng đăng nhập để điểm danh", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            com.example.mangaapp.ui.checkin.DailyCheckInDialog.show(parentFragmentManager)
        }
        tvLatestSeeAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, com.example.mangaapp.ui.list.ListFragment.newInstance("Mới nhất"))
                .addToBackStack(null)
                .commit()
        }
        tvRankingSeeAll.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, com.example.mangaapp.ui.list.ListFragment.newInstance("Xem nhiều"))
                .addToBackStack(null)
                .commit()
        }
    }

    private fun updateThemeIcon() {
        val iconRes = if (ThemeManager.isDarkMode()) R.drawable.ic_light_mode
        else R.drawable.ic_dark_mode
        btnThemeToggle.setImageResource(iconRes)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        autoScrollRunnable?.let { handler.removeCallbacks(it) }
        autoScrollRunnable = null
    }
}