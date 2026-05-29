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

class LoginFragment : Fragment() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePassword: ImageButton
    private lateinit var btnLogin: Button
    private lateinit var btnGoRegister: Button
    private lateinit var tvSkipLogin: TextView
    private lateinit var tvError: TextView
    private lateinit var progressLogin: ProgressBar

    private val auth = FirebaseAuth.getInstance()
    private var isPasswordVisible = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        setupClickListeners()

        // Nếu đã đăng nhập rồi → quay về
        if (UserSession.isLoggedIn) {
            parentFragmentManager.popBackStack()
        }
    }

    private fun initViews(view: View) {
        // Layout fragment_login dùng et_username → đổi hint thành Email
        etEmail           = view.findViewById(R.id.et_username)
        etPassword        = view.findViewById(R.id.et_password)
        btnTogglePassword = view.findViewById(R.id.btn_toggle_password)
        btnLogin          = view.findViewById(R.id.btn_login)
        btnGoRegister     = view.findViewById(R.id.btn_go_register)
        tvSkipLogin       = view.findViewById(R.id.tv_skip_login)
        tvError           = view.findViewById(R.id.tv_error)
        progressLogin     = ProgressBar(requireContext()).apply {
            visibility = View.GONE
        }
        etEmail.hint = "Email"
    }

    private fun setupClickListeners() {
        btnTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            etPassword.transformationMethod =
                if (isPasswordVisible) null else PasswordTransformationMethod.getInstance()
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val email    = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                showError("Vui lòng nhập đầy đủ email và mật khẩu!")
                return@setOnClickListener
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showError("Email không hợp lệ!")
                return@setOnClickListener
            }

            setLoading(true)

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // Tải thông tin user từ Firestore
                    UserSession.loadUser { user ->
                        if (!isAdded) return@loadUser
                        setLoading(false)
                        if (user != null) {
                            Toast.makeText(
                                requireContext(),
                                "Chào mừng trở lại, ${user.username}! 🎉",
                                Toast.LENGTH_SHORT
                            ).show()
                            parentFragmentManager.popBackStack()
                        } else {
                            showError("Không tải được thông tin tài khoản")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    if (!isAdded) return@addOnFailureListener
                    setLoading(false)
                    showError(friendlyError(e.message))
                }
        }

        btnGoRegister.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, RegisterFragment())
                .addToBackStack(null)
                .commit()
        }

        tvSkipLogin.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun setLoading(loading: Boolean) {
        btnLogin.isEnabled  = !loading
        btnLogin.text       = if (loading) "Đang đăng nhập..." else "Đăng nhập"
        btnLogin.alpha      = if (loading) 0.7f else 1f
    }

    private fun showError(message: String) {
        tvError.text       = "⚠️ $message"
        tvError.visibility = View.VISIBLE
    }

    private fun friendlyError(msg: String?): String = when {
        msg == null                           -> "Đã có lỗi xảy ra, thử lại sau"
        msg.contains("password")             -> "Mật khẩu không đúng"
        msg.contains("no user record")       -> "Email chưa được đăng ký"
        msg.contains("network")              -> "Lỗi kết nối mạng"
        msg.contains("too-many-requests")    -> "Quá nhiều lần thử. Vui lòng thử lại sau"
        else                                  -> "Đăng nhập thất bại: $msg"
    }
}