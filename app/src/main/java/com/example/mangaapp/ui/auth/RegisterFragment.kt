package com.example.mangaapp.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mangaapp.R

class RegisterFragment : Fragment() {

    private lateinit var btnBack: ImageButton
    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnRegister: Button
    private lateinit var tvGoLogin: TextView
    private lateinit var tvError: TextView
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_register, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        btnBack           = view.findViewById(R.id.btn_back)
        etUsername        = view.findViewById(R.id.et_username)
        etEmail           = view.findViewById(R.id.et_email)
        etPassword        = view.findViewById(R.id.et_password)
        etConfirmPassword = view.findViewById(R.id.et_confirm_password)
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password)
        btnRegister       = view.findViewById(R.id.btn_register)
        tvGoLogin         = view.findViewById(R.id.tv_go_login)
        tvError           = view.findViewById(R.id.tv_error)
    }

    private fun setupClickListeners() {

        btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Toggle mật khẩu
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod =
                if (isPasswordVisible) null
                else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
        }

        // Đăng ký
        btnRegister.setOnClickListener {
            val username  = etUsername.text.toString().trim()
            val email     = etEmail.text.toString().trim()
            val password  = etPassword.text.toString()
            val confirm   = etConfirmPassword.text.toString()

            // Validation
            when {
                username.isEmpty() || email.isEmpty() || password.isEmpty() -> {
                    showError("Vui lòng nhập đầy đủ thông tin!")
                    return@setOnClickListener
                }
                username.length < 4 -> {
                    showError("Tên đăng nhập phải có ít nhất 4 ký tự!")
                    return@setOnClickListener
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    showError("Email không hợp lệ!")
                    return@setOnClickListener
                }
                password.length < 6 -> {
                    showError("Mật khẩu phải có ít nhất 6 ký tự!")
                    return@setOnClickListener
                }
                password != confirm -> {
                    showError("Mật khẩu xác nhận không khớp!")
                    return@setOnClickListener
                }
            }

            // TODO: Khi có API thật thay bằng gọi API register
            hideError()
            Toast.makeText(requireContext(), "Đăng ký thành công! Vui lòng đăng nhập.", Toast.LENGTH_LONG).show()
            parentFragmentManager.popBackStack()
        }

        // Quay lại đăng nhập
        tvGoLogin.setOnClickListener {
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
