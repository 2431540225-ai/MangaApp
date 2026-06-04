package com.example.mangaapp.ui.coin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.ui.wallet.WalletTransactionAdapter
import com.example.mangaapp.R
import com.example.mangaapp.models.CoinPackage
import com.example.mangaapp.repository.CoinRepository
import com.example.mangaapp.utils.UserSession
import java.text.NumberFormat
import java.util.Locale

/**
 * Màn hình ví coin:
 *  - Số dư + quy đổi VND
 *  - Danh sách gói coin dạng grid 2 cột
 *  - Lịch sử giao dịch (nạp coin + mở khóa chapter)
 */
class CoinWalletFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var btnHistoryShortcut: TextView
    private lateinit var tvCoinBalance: TextView
    private lateinit var tvBalanceVnd: TextView
    private lateinit var rvPackages: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvLoginPrompt: TextView
    private lateinit var rvHistory: RecyclerView
    private lateinit var layoutEmptyHistory: LinearLayout
    private lateinit var scrollWallet: NestedScrollView
    private lateinit var sectionHistory: LinearLayout

    private val historyAdapter = WalletTransactionAdapter()
    private val vndFmt = NumberFormat.getInstance(Locale("vi"))

    // Lắng nghe thay đổi coin realtime
    private val coinListener: (Int) -> Unit = { newCoins ->
        if (isAdded) {
            tvCoinBalance.text = "$newCoins 🪙"
            tvBalanceVnd.text = "≈ ${formatVnd(coinsToVnd(newCoins.toLong()))}"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_coin_wallet, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupHeader()
        loadPackages()
        setupHistory()
        UserSession.addCoinChangeListener(coinListener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        UserSession.removeCoinChangeListener(coinListener)
        (activity as? com.example.mangaapp.MainActivity)?.showBottomNav()
    }

    override fun onResume() {
        super.onResume()
        (activity as? com.example.mangaapp.MainActivity)?.hideBottomNav()
    }

    private fun initViews(view: View) {
        btnBack             = view.findViewById(R.id.btn_back_wallet)
        btnHistoryShortcut  = view.findViewById(R.id.btn_history_shortcut)
        tvCoinBalance       = view.findViewById(R.id.tv_wallet_balance)
        tvBalanceVnd        = view.findViewById(R.id.tv_balance_vnd)
        rvPackages          = view.findViewById(R.id.rv_coin_packages)
        progressLoading     = view.findViewById(R.id.progress_wallet)
        tvLoginPrompt       = view.findViewById(R.id.tv_wallet_login_prompt)
        rvHistory           = view.findViewById(R.id.rv_wallet_history)
        layoutEmptyHistory  = view.findViewById(R.id.layout_empty_history)
        scrollWallet        = view.findViewById(R.id.scroll_wallet)
        sectionHistory      = view.findViewById(R.id.section_history)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Nhấn "Lịch sử" trên header → scroll xuống phần lịch sử
        btnHistoryShortcut.setOnClickListener {
            scrollWallet.post {
                scrollWallet.smoothScrollTo(0, sectionHistory.top)
            }
        }
    }

    private fun setupHeader() {
        if (!UserSession.isLoggedIn) {
            tvLoginPrompt.visibility = View.VISIBLE
            tvCoinBalance.text = "-- 🪙"
            tvBalanceVnd.text = ""
            return
        }
        tvLoginPrompt.visibility = View.GONE
        val coins = UserSession.currentUser?.coins ?: 0
        tvCoinBalance.text = "$coins 🪙"
        tvBalanceVnd.text = "≈ ${formatVnd(coinsToVnd(coins.toLong()))}"
    }

    private fun setupHistory() {
        rvHistory.layoutManager = LinearLayoutManager(requireContext())
        rvHistory.adapter = historyAdapter

        val uid = UserSession.firebaseUid ?: return

        CoinRepository.getUserCoinHistory(
            userId = uid,
            onSuccess = { list ->
                if (!isAdded) return@getUserCoinHistory
                historyAdapter.submitList(list)
                val empty = list.isEmpty()
                layoutEmptyHistory.visibility = if (empty) View.VISIBLE else View.GONE
                rvHistory.visibility = if (empty) View.GONE else View.VISIBLE
            },
            onError = {
                if (!isAdded) return@getUserCoinHistory
                layoutEmptyHistory.visibility = View.VISIBLE
                rvHistory.visibility = View.GONE
            }
        )
    }

    private fun loadPackages() {
        progressLoading.visibility = View.VISIBLE
        CoinRepository.getCoinPackages(
            onSuccess = { packages ->
                if (!isAdded) return@getCoinPackages
                progressLoading.visibility = View.GONE
                setupPackageList(packages)
            },
            onError = {
                if (!isAdded) return@getCoinPackages
                progressLoading.visibility = View.GONE
                Toast.makeText(requireContext(), "Không tải được gói coin", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun setupPackageList(packages: List<CoinPackage>) {
        val adapter = CoinPackageAdapter(packages) { pkg ->
            handleBuyPackage(pkg)
        }
        rvPackages.layoutManager = GridLayoutManager(requireContext(), 2)
        rvPackages.adapter = adapter
    }

    /**
     * Xử lý mua coin.
     *
     * TODO PRODUCTION: Thay khối AlertDialog bên dưới bằng Google Play Billing:
     *   1. Khởi tạo BillingClient
     *   2. Gọi launchBillingFlow() với productDetails tương ứng pkg.id
     *   3. Trong onPurchasesUpdated(), nếu BillingResponseCode.OK → gọi CoinRepository.topUpCoins()
     *
     * Tham khảo: https://developer.android.com/google/play/billing/integrate
     */
    private fun handleBuyPackage(pkg: CoinPackage) {
        if (!UserSession.isLoggedIn) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show()
            return
        }

        // Tính tổng coin người dùng sẽ nhận
        val totalCoinsLabel = if (pkg.bonusCoins > 0)
            "${pkg.coins} + ${pkg.bonusCoins} bonus = ${pkg.totalCoins} coin"
        else
            "${pkg.totalCoins} coin"

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận mua coin")
            .setMessage(
                "Gói: $totalCoinsLabel\n" +
                        "Giá: ${pkg.priceLabel}\n\n" +
                        "⚠️ (Demo) Thanh toán sẽ được xác nhận ngay."
            )
            .setPositiveButton("Xác nhận") { _, _ ->
                progressLoading.visibility = View.VISIBLE

                CoinRepository.topUpCoins(
                    amount    = pkg.totalCoins,
                    packageId = pkg.id,
                    onSuccess = { newBalance ->
                        if (!isAdded) return@topUpCoins
                        progressLoading.visibility = View.GONE
                        tvCoinBalance.text = "$newBalance 🪙"
                        tvBalanceVnd.text = "≈ ${formatVnd(coinsToVnd(newBalance.toLong()))}"
                        setupHistory()
                        Toast.makeText(
                            requireContext(),
                            "✅ Nạp thành công ${pkg.totalCoins} coin!\nSố dư mới: $newBalance 🪙",
                            Toast.LENGTH_LONG
                        ).show()
                    },
                    onError = { msg ->
                        if (!isAdded) return@topUpCoins
                        progressLoading.visibility = View.GONE
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                )
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** 1 coin = 100 VND (theo gói nạp: 50 coin / 5.000đ) */
    private fun coinsToVnd(coins: Long): Long = coins * 100

    private fun formatVnd(vnd: Long): String = "${vndFmt.format(vnd)}đ"
}
