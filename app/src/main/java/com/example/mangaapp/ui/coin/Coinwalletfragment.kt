package com.example.mangaapp.ui.coin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.CoinPackage
import com.example.mangaapp.repository.CoinRepository
import com.example.mangaapp.utils.UserSession

/**
 * Màn hình ví coin:
 *  - Hiển thị số dư hiện tại
 *  - Danh sách gói coin có thể mua
 *  - Nút mua (giả lập thanh toán thành công cho demo)
 */
class CoinWalletFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var tvCoinBalance: TextView
    private lateinit var rvPackages: RecyclerView
    private lateinit var progressLoading: ProgressBar
    private lateinit var tvLoginPrompt: TextView

    // Lắng nghe thay đổi coin realtime
    private val coinListener: (Int) -> Unit = { newCoins ->
        if (isAdded) {
            tvCoinBalance.text = "$newCoins 🪙"
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
        btnBack        = view.findViewById(R.id.btn_back_wallet)
        tvCoinBalance  = view.findViewById(R.id.tv_wallet_balance)
        rvPackages     = view.findViewById(R.id.rv_coin_packages)
        progressLoading = view.findViewById(R.id.progress_wallet)
        tvLoginPrompt  = view.findViewById(R.id.tv_wallet_login_prompt)

        btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setupHeader() {
        if (!UserSession.isLoggedIn) {
            tvLoginPrompt.visibility = View.VISIBLE
            tvCoinBalance.text = "-- 🪙"
            return
        }
        tvLoginPrompt.visibility = View.GONE
        val coins = UserSession.currentUser?.coins ?: 0
        tvCoinBalance.text = "$coins 🪙"
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
     * Trong thực tế: tích hợp Google Play Billing hoặc Momo/ZaloPay.
     * Hiện tại giả lập thanh toán thành công để demo.
     */
    private fun handleBuyPackage(pkg: CoinPackage) {
        if (!UserSession.isLoggedIn) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập trước", Toast.LENGTH_SHORT).show()
            return
        }

        // ── DEMO: Giả lập thanh toán thành công ──────────────────────────────
        // TODO: Thay bằng Google Play Billing API hoặc payment gateway thật
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận thanh toán")
            .setMessage("Mua ${pkg.totalCoins} 🪙 với giá ${pkg.priceLabel}?\n\n(Chế độ demo: thanh toán thành công ngay)")
            .setPositiveButton("Xác nhận") { _, _ ->
                progressLoading.visibility = View.VISIBLE
                CoinRepository.topUpCoins(
                    amount    = pkg.totalCoins,
                    packageId = pkg.id,
                    onSuccess = { newBalance ->
                        if (!isAdded) return@topUpCoins
                        progressLoading.visibility = View.GONE
                        tvCoinBalance.text = "$newBalance 🪙"
                        Toast.makeText(
                            requireContext(),
                            "Nạp thành công ${pkg.totalCoins} coin! Số dư: $newBalance 🪙",
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
}