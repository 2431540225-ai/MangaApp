package com.example.mangaapp.ui.auth

import android.os.Bundle
import android.text.method.PasswordTransformationMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.example.mangaapp.R
import com.example.mangaapp.utils.UserSession
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest

class RegisterFragment : Fragment() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnRegister: Button
    private lateinit var tvGoLogin: TextView
    private lateinit var tvError: TextView

    private val auth = FirebaseAuth.getInstance()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_register, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()
    }

    private fun initViews(view: View) {
        // Dùng đúng ID trong fragment_register.xml của bạn
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
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val method = if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword.transformationMethod        = method
            etConfirmPassword.transformationMethod = method
            etPassword.setSelection(etPassword.text.length)
        }

        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirm  = etConfirmPassword.text.toString()

            when {
                username.length < 3 ->
                    showError("Tên hiển thị cần ít nhất 3 ký tự")
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    showError("Email không hợp lệ")
                password.length < 6 ->
                    showError("Mật khẩu cần ít nhất 6 ký tự")
                password != confirm ->
                    showError("Mật khẩu xác nhận không khớp")
                else ->
                    doRegister(username, email, password)
            }
        }

        tvGoLogin.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun doRegister(username: String, email: String, password: String) {
        setLoading(true)
        tvError.visibility = View.GONE

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val firebaseUser = result.user ?: run { setLoading(false); return@addOnSuccessListener }

                // Gán displayName
                firebaseUser.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(username).build()
                )

                // Tạo document user trong Firestore
                val db  = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                val uid = firebaseUser.uid
                val userData = hashMapOf(
                    "username"         to username,
                    "email"            to email,
                    "avatarUrl"        to "",
                    "roleId"           to 2,          // mặc định là Độc giả
                    "coins"            to 0,
                    "unlockedChapters" to emptyList<String>(),
                    "createdAt"        to System.currentTimeMillis()
                )

                db.collection("users").document(uid).set(userData)
                    .addOnSuccessListener {
                        UserSession.loadUser { user ->
                            if (!isAdded) return@loadUser
                            setLoading(false)
                            if (user != null) {
                                Toast.makeText(
                                    requireContext(),
                                    "Đăng ký thành công! Chào mừng $username 🎉",
                                    Toast.LENGTH_LONG
                                ).show()
                                // Quay về Home (pop 2 màn hình: Register + Login)
                                parentFragmentManager.popBackStack()
                                parentFragmentManager.popBackStack()
                            } else {
                                showError("Đăng ký thành công nhưng không tải được thông tin")
                            }
                        }
                    }
                    .addOnFailureListener {
                        setLoading(false)
                        showError("Lỗi tạo tài khoản: ${it.message}")
                    }
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                setLoading(false)
                showError(friendlyError(it.message))
            }
    }

    private fun setLoading(loading: Boolean) {
        btnRegister.isEnabled = !loading
        btnRegister.text      = if (loading) "Đang đăng ký..." else "Tạo tài khoản"
        btnRegister.alpha     = if (loading) 0.7f else 1f
    }

    private fun showError(msg: String) {
        tvError.text       = "⚠️ $msg"
        tvError.visibility = View.VISIBLE
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null                           -> "Đã có lỗi xảy ra"
        msg.contains("email-already-in-use") -> "Email này đã được đăng ký"
        msg.contains("network")              -> "Lỗi kết nối mạng"
        else                                  -> "Đăng ký thất bại: $msg"
    }
}