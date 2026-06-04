package com.example.mangaapp.ui.checkin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.mangaapp.R
import com.example.mangaapp.repository.CheckInRepository

/**
 * Dialog điểm danh hàng ngày nhận coin.
 *
 * Cách dùng:
 * ```
 * DailyCheckInDialog.show(parentFragmentManager)
 * ```
 */
class DailyCheckInDialog : DialogFragment() {

    private lateinit var tvStreak: TextView
    private lateinit var tvReward: TextView
    private lateinit var tvDone: TextView
    private lateinit var btnCheckIn: Button
    private lateinit var btnClose: Button
    private lateinit var progress: ProgressBar

    private val dayIconIds = listOf(
        R.id.tv_day1_icon,
        R.id.tv_day2_icon,
        R.id.tv_day3_icon,
        R.id.tv_day4_icon,
        R.id.tv_day5_icon,
        R.id.tv_day6_icon,
        R.id.tv_day7_icon
    )

    companion object {
        fun show(fragmentManager: androidx.fragment.app.FragmentManager) {
            // Tránh show trùng
            if (fragmentManager.findFragmentByTag("daily_checkin") != null) return
            DailyCheckInDialog().show(fragmentManager, "daily_checkin")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_daily_checkin, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        loadStatus()
        setupListeners()

        // Làm cho dialog rộng hơn
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun initViews(view: View) {
        tvStreak  = view.findViewById(R.id.tv_checkin_streak)
        tvReward  = view.findViewById(R.id.tv_checkin_reward)
        tvDone    = view.findViewById(R.id.tv_checkin_done)
        btnCheckIn = view.findViewById(R.id.btn_checkin)
        btnClose   = view.findViewById(R.id.btn_checkin_close)
        progress   = view.findViewById(R.id.progress_checkin)
    }

    private fun loadStatus() {
        CheckInRepository.getCheckInStatus { status ->
            if (!isAdded) return@getCheckInStatus

            tvStreak.text = "🔥 Chuỗi: ${status.currentStreak} ngày liên tiếp"
            tvReward.text = "Hôm nay nhận: ${status.todayReward} 🪙"

            updateDayIcons(status.currentStreak, status.alreadyCheckedInToday)

            if (status.alreadyCheckedInToday) {
                showAlreadyCheckedIn()
            }
        }
    }

    /**
     * Cập nhật 7 ô ngày:
     *  - Ngày đã qua (< currentStreak): ✅ (nền xanh)
     *  - Ngày hôm nay (= currentStreak): 🎁 (nếu chưa điểm danh) hoặc ✅ (đã điểm danh)
     *  - Ngày chưa đến: ⬜
     */
    private fun updateDayIcons(streak: Int, alreadyCheckedIn: Boolean) {
        if (!isAdded) return
        val view = view ?: return

        for (i in dayIconIds.indices) {
            val dayNum = i + 1
            val tv = view.findViewById<TextView>(dayIconIds[i])

            when {
                // Ngày đã hoàn thành trong streak
                dayNum < streak || (dayNum <= streak && alreadyCheckedIn) -> {
                    tv.text = "✅"
                    tv.setBackgroundResource(R.drawable.bg_dot_active)
                }
                // Ngày hôm nay (chưa điểm danh)
                dayNum == streak && !alreadyCheckedIn -> {
                    tv.text = "🎁"
                    tv.setBackgroundResource(R.drawable.bg_badge_hot)
                }
                // Ngày chưa đến
                else -> {
                    tv.text = "⬜"
                    tv.setBackgroundResource(R.drawable.bg_dot_inactive)
                }
            }
        }
    }

    private fun setupListeners() {
        btnCheckIn.setOnClickListener { doCheckIn() }
        btnClose.setOnClickListener { dismiss() }
    }

    private fun doCheckIn() {
        progress.visibility = View.VISIBLE
        btnCheckIn.isEnabled = false

        CheckInRepository.checkIn(
            onSuccess = { reward, newStreak ->
                if (!isAdded) return@checkIn
                progress.visibility = View.GONE

                // Hiệu ứng thành công
                Toast.makeText(
                    requireContext(),
                    "🎉 Nhận $reward coin! Chuỗi $newStreak ngày!",
                    Toast.LENGTH_LONG
                ).show()

                // Cập nhật UI
                tvStreak.text = "🔥 Chuỗi: $newStreak ngày liên tiếp"
                tvReward.text = "Đã nhận: $reward 🪙"
                updateDayIcons(newStreak, true)
                showAlreadyCheckedIn()
            },
            onError = { msg ->
                if (!isAdded) return@checkIn
                progress.visibility = View.GONE
                btnCheckIn.isEnabled = true
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

                if (msg.contains("đã điểm danh", ignoreCase = true)) {
                    showAlreadyCheckedIn()
                }
            }
        )
    }

    private fun showAlreadyCheckedIn() {
        btnCheckIn.visibility = View.GONE
        btnClose.text = "Đóng"
        tvDone.visibility = View.VISIBLE
    }
}
