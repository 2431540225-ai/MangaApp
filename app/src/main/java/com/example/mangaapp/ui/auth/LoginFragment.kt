package com.example.mangaapp.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mangaapp.R

class LoginFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnLogin: Button
    private lateinit var btnGoRegister: Button
    private lateinit var tvSkipLogin: TextView
    private lateinit var tvError: TextView
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        etUsername        = view.findViewById(R.id.et_username)
        etPassword        = view.findViewById(R.id.et_password)
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password)
        btnLogin          = view.findViewById(R.id.btn_login)
        btnGoRegister     = view.findViewById(R.id.btn_go_register)
        tvSkipLogin       = view.findViewById(R.id.tv_skip_login)
        tvError           = view.findViewById(R.id.tv_error)
    }

    private fun setupClickListeners() {

        // Toggle hiện/ẩn mật khẩu
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod =
                if (isPasswordVisible) null
                else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
        }

        // Đăng nhập
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                showError("Vui lòng nhập đầy đủ thông tin!")
                return@setOnClickListener
            }

            // TODO: Khi có API thật, thay đoạn này bằng gọi API login
            // Hiện tại dùng tài khoản giả để test
            if (username == "admin" && password == "123456") {
                hideError()
                Toast.makeText(requireContext(), "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()
                // TODO: Navigate về Home
                parentFragmentManager.popBackStack()
            } else {
                showError("Tên đăng nhập hoặc mật khẩu không đúng!")
            }
        }

        // Chuyển sang đăng ký
        btnGoRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        // Bỏ qua đăng nhập
        tvSkipLogin.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun showError(message: String) {
        tvError.text = "⚠️ $message"
        tvError.visibility = View.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = View.GONE
    }
}
