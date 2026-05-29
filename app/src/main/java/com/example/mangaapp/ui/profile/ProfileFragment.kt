package com.example.mangaapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mangaapp.R
import com.example.mangaapp.ui.auth.LoginFragment
import com.example.mangaapp.ui.coin.CoinWalletFragment
import com.example.mangaapp.ui.upload.UploadMangaFragment
import com.example.mangaapp.utils.UserSession

class ProfileFragment : Fragment() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvCoinBalance: TextView
    private lateinit var btnWallet: LinearLayout
    private lateinit var btnUploadManga: LinearLayout
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var layoutLoggedIn: LinearLayout
    private lateinit var layoutGuest: LinearLayout
    private lateinit var layoutAuthorSection: LinearLayout

    // Lắng nghe thay đổi coin realtime
    private val coinListener: (Int) -> Unit = { newCoins ->
        if (isAdded) tvCoinBalance.text = "$newCoins 🪙"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.profile_fragment, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        renderUI()
        UserSession.addCoinChangeListener(coinListener)
    }

    override fun onResume() {
        super.onResume()
        // Refresh lại khi quay về (ví dụ sau khi mua coin)
        renderUI()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        UserSession.removeCoinChangeListener(coinListener)
    }

    private fun initViews(view: View) {
        tvUsername         = view.findViewById(R.id.tv_username)
        tvEmail            = view.findViewById(R.id.tv_email)
        tvRole             = view.findViewById(R.id.tv_role)
        tvCoinBalance      = view.findViewById(R.id.tv_coin_balance)
        btnWallet          = view.findViewById(R.id.btn_wallet)
        btnUploadManga     = view.findViewById(R.id.btn_upload_manga)
        btnLogin           = view.findViewById(R.id.btn_login_profile)
        btnLogout          = view.findViewById(R.id.btn_logout)
        layoutLoggedIn     = view.findViewById(R.id.layout_logged_in)
        layoutGuest        = view.findViewById(R.id.layout_guest)
        layoutAuthorSection = view.findViewById(R.id.layout_author_section)
    }

    private fun renderUI() {
        val user = UserSession.currentUser

        if (user == null || !UserSession.isLoggedIn) {
            // Chưa đăng nhập
            layoutGuest.visibility    = View.VISIBLE
            layoutLoggedIn.visibility = View.GONE

            btnLogin.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.container, LoginFragment())
                    .addToBackStack(null)
                    .commit()
            }
            return
        }

        // Đã đăng nhập
        layoutGuest.visibility    = View.GONE
        layoutLoggedIn.visibility = View.VISIBLE

        tvUsername.text    = user.username
        tvEmail.text       = user.email
        tvCoinBalance.text = "${user.coins} 🪙"

        val roleLabel = when (user.roleId) {
            1    -> "👑 Admin"
            3    -> "✍️ Tác giả"
            else -> "📖 Độc giả"
        }
        tvRole.text = roleLabel

        // Hiện section đăng truyện nếu là tác giả hoặc admin
        layoutAuthorSection.visibility =
            if (user.isAuthor) View.VISIBLE else View.GONE

        // Nút ví coin
        btnWallet.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, CoinWalletFragment())
                .addToBackStack(null)
                .commit()
        }

        // Nút đăng truyện (chỉ hiện với tác giả/admin)
        btnUploadManga.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, UploadMangaFragment())
                .addToBackStack(null)
                .commit()
        }

        // Đăng xuất
        btnLogout.setOnClickListener {
            android.app.AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất") { _, _ ->
                    UserSession.signOut()
                    renderUI()
                    Toast.makeText(requireContext(), "Đã đăng xuất", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }
}