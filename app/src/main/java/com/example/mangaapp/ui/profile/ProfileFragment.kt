package com.example.mangaapp.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.mangaapp.R
import com.google.android.material.snackbar.Snackbar

class ProfileFragment : Fragment() {

    private lateinit var rootView: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.profile_fragment, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
    }

    private fun setupClickListeners() {

        // Click phần avatar - Đăng nhập
        rootView.findViewById<View>(R.id.img_avatar)?.setOnClickListener {
            showMessage("Mở màn hình Đăng nhập / Đăng ký")
        }

        // Các item trong Liên Hệ
        rootView.findViewById<View>(R.id.item_admin)?.setOnClickListener {
            showMessage("Liên hệ Admin")
        }

        rootView.findViewById<View>(R.id.item_privacy)?.setOnClickListener {
            showMessage("Mở Chính sách Bảo mật (sẽ dùng WebView)")
        }

        rootView.findViewById<View>(R.id.item_rate)?.setOnClickListener {
            showMessage("Đánh giá ứng dụng")
        }

        rootView.findViewById<View>(R.id.item_share)?.setOnClickListener {
            showMessage("Chia sẻ ứng dụng")
        }
    }

    private fun showMessage(message: String) {
        Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    }
}