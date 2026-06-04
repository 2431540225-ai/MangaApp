package com.example.mangaapp.ui.author

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.MainActivity
import com.example.mangaapp.R
import com.example.mangaapp.models.AuthorRevenueSummary
import com.example.mangaapp.repository.AuthorRevenueRepository
import com.example.mangaapp.ui.wallet.WalletTransactionAdapter
import com.example.mangaapp.utils.UserSession
import java.text.NumberFormat
import java.util.Locale

/**
 * Ví doanh thu tác giả:
 *  - Số coin khả dụng + quy đổi VND
 *  - Thống kê tổng thu / đã rút
 *  - Nút rút tiền về ngân hàng (với validation đầy đủ)
 *  - Lịch sử hoa hồng
 */
class AuthorEarningsFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var progress: ProgressBar
    private lateinit var tvPendingCoins: TextView
    private lateinit var tvPendingVnd: TextView
    private lateinit var tvTotalEarned: TextView
    private lateinit var tvTotalWithdrawn: TextView
    private lateinit var tvCommissionInfo: TextView
    private lateinit var btnWithdraw: Button
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyEarnings: LinearLayout

    private val historyAdapter = WalletTransactionAdapter(forAuthor = true)
    private var summary: AuthorRevenueSummary? = null
    private val vndFmt = NumberFormat.getInstance(Locale("vi"))

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_author_earnings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        btnWithdraw.setOnClickListener { showWithdrawDialog() }

        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        loadData()
    }

    override fun onResume() {
        super.onResume()
        (activity as? MainActivity)?.hideBottomNav()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainActivity)?.showBottomNav()
    }

    private fun initViews(view: View) {
        btnBack             = view.findViewById(R.id.btn_back_earnings)
        progress            = view.findViewById(R.id.progress_earnings)
        tvPendingCoins      = view.findViewById(R.id.tv_pending_coins)
        tvPendingVnd        = view.findViewById(R.id.tv_pending_vnd)
        tvTotalEarned       = view.findViewById(R.id.tv_total_earned)
        tvTotalWithdrawn    = view.findViewById(R.id.tv_total_withdrawn)
        tvCommissionInfo    = view.findViewById(R.id.tv_commission_info)
        btnWithdraw         = view.findViewById(R.id.btn_withdraw)
        rvHistory           = view.findViewById(R.id.rv_earnings_history)
        layoutEmptyEarnings = view.findViewById(R.id.layout_empty_earnings)
    }

    private fun loadData() {
        val uid = UserSession.firebaseUid ?: run {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
            return
        }

        progress.visibility = View.VISIBLE

        AuthorRevenueRepository.getRevenueSummary(
            authorId  = uid,
            onSuccess = { s ->
                if (!isAdded) return@getRevenueSummary
                summary = s
                bindSummary(s)
                loadHistory(uid)
            },
            onError = { msg ->
                if (!isAdded) return@getRevenueSummary
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun bindSummary(s: AuthorRevenueSummary) {
        tvPendingCoins.text   = "${s.pendingCoins} 🪙"
        tvPendingVnd.text     = "≈ ${formatVnd(s.pendingVnd)}"
        tvTotalEarned.text    = "${s.totalEarned} 🪙"
        tvTotalWithdrawn.text = "${s.totalWithdrawn} 🪙"

        tvCommissionInfo.text =
            "Bạn nhận ${s.authorSharePercent}% mỗi lần độc giả mở khóa chapter trả phí. " +
                    "App giữ ${s.platformSharePercent}% phí nền tảng."

        val canWithdraw = s.pendingCoins >= AuthorRevenueRepository.MIN_WITHDRAW_COINS
        btnWithdraw.isEnabled = canWithdraw
        btnWithdraw.alpha = if (canWithdraw) 1f else 0.5f
    }

    private fun loadHistory(authorId: String) {
        AuthorRevenueRepository.getCombinedHistory(
            authorId  = authorId,
            onSuccess = { list ->
                if (!isAdded) return@getCombinedHistory
                progress.visibility = View.GONE
                historyAdapter.submitList(list)

                val empty = list.isEmpty()
                layoutEmptyEarnings.visibility = if (empty) View.VISIBLE else View.GONE
                rvHistory.visibility           = if (empty) View.GONE else View.VISIBLE
            },
            onError = { msg ->
                if (!isAdded) return@getCombinedHistory
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // ── Dialog rút tiền ──────────────────────────────────────────────────────

    private fun showWithdrawDialog() {
        val s = summary ?: return

        if (s.pendingCoins < AuthorRevenueRepository.MIN_WITHDRAW_COINS) {
            Toast.makeText(
                requireContext(),
                "Cần tối thiểu ${AuthorRevenueRepository.MIN_WITHDRAW_COINS} coin để rút",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val dialogView  = layoutInflater.inflate(R.layout.dialog_withdraw, null)
        val etAmount    = dialogView.findViewById<EditText>(R.id.et_withdraw_amount)
        val tvVndHint   = dialogView.findViewById<TextView>(R.id.tv_withdraw_vnd_hint)
        val etBank      = dialogView.findViewById<EditText>(R.id.et_bank_name)
        val etAccount   = dialogView.findViewById<EditText>(R.id.et_account_number)
        val etHolder    = dialogView.findViewById<EditText>(R.id.et_account_holder)

        // Điền sẵn số coin tối đa
        etAmount.setText(s.pendingCoins.toString())
        updateVndHint(tvVndHint, s.pendingCoins)

        // Cập nhật hint VND khi gõ
        etAmount.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateVndHint(tvVndHint, s?.toString()?.toLongOrNull() ?: 0L)
            }
        })

        // Điền sẵn thông tin ngân hàng đã lưu
        val uid = UserSession.firebaseUid ?: return
        AuthorRevenueRepository.getSavedBankInfo(uid) { bank, acc, holder ->
            if (!isAdded) return@getSavedBankInfo
            if (bank.isNotEmpty()) etBank.setText(bank)
            if (acc.isNotEmpty()) etAccount.setText(acc)
            if (holder.isNotEmpty()) etHolder.setText(holder)
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle("Rút tiền về ngân hàng")
            .setView(dialogView)
            .setPositiveButton("Gửi yêu cầu", null) // null để override bên dưới
            .setNegativeButton("Hủy", null)
            .create()

        dialog.setOnShowListener {
            // Override positive button để validate trước khi đóng
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = etAmount.text.toString().toLongOrNull() ?: 0L
                val bank   = etBank.text.toString().trim()
                val acc    = etAccount.text.toString().trim()
                val holder = etHolder.text.toString().trim()

                // ── Validation ──────────────────────────────────
                when {
                    amount <= 0 -> {
                        etAmount.error = "Nhập số coin muốn rút"
                        return@setOnClickListener
                    }
                    amount > s.pendingCoins -> {
                        etAmount.error = "Vượt quá số coin khả dụng (${s.pendingCoins})"
                        return@setOnClickListener
                    }
                    amount < AuthorRevenueRepository.MIN_WITHDRAW_COINS -> {
                        etAmount.error = "Tối thiểu ${AuthorRevenueRepository.MIN_WITHDRAW_COINS} coin"
                        return@setOnClickListener
                    }
                    bank.isEmpty() -> {
                        etBank.error = "Nhập tên ngân hàng"
                        return@setOnClickListener
                    }
                    acc.isEmpty() -> {
                        etAccount.error = "Nhập số tài khoản"
                        return@setOnClickListener
                    }
                    holder.isEmpty() -> {
                        etHolder.error = "Nhập tên chủ tài khoản"
                        return@setOnClickListener
                    }
                }

                dialog.dismiss()
                submitWithdraw(amount, bank, acc, holder)
            }
        }

        dialog.show()
    }

    private fun updateVndHint(tv: TextView, coins: Long) {
        val vnd = coins * AuthorRevenueRepository.VND_PER_COIN
        tv.text = "≈ ${formatVnd(vnd)}"
    }

    private fun submitWithdraw(
        amount: Long,
        bankName: String,
        accountNumber: String,
        accountHolder: String
    ) {
        progress.visibility = View.VISIBLE

        AuthorRevenueRepository.requestWithdrawal(
            coinAmount    = amount,
            bankName      = bankName,
            accountNumber = accountNumber,
            accountHolder = accountHolder,
            onSuccess = {
                if (!isAdded) return@requestWithdrawal

                // Lưu lại thông tin ngân hàng
                AuthorRevenueRepository.saveBankInfo(bankName, accountNumber, accountHolder,
                    onSuccess = {}, onError = {})

                val vnd = formatVnd(amount * AuthorRevenueRepository.VND_PER_COIN)
                Toast.makeText(
                    requireContext(),
                    "✅ Đã gửi yêu cầu rút $amount coin (≈ $vnd).\nAdmin sẽ xử lý trong 3–7 ngày làm việc.",
                    Toast.LENGTH_LONG
                ).show()

                loadData() // Reload để cập nhật số dư
            },
            onError = { msg ->
                if (!isAdded) return@requestWithdrawal
                progress.visibility = View.GONE
                Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
            }
        )
    }

    private fun formatVnd(vnd: Long): String = "${vndFmt.format(vnd)}đ"
}
