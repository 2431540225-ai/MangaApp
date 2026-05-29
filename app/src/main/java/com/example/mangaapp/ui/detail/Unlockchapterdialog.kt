package com.example.mangaapp.ui.detail

import android.app.Dialog
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
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.repository.CoinRepository
import com.example.mangaapp.utils.UserSession

/**
 * Dialog xác nhận mở khóa chapter bằng coin.
 *
 * Cách dùng:
 * ```
 * UnlockChapterDialog.show(
 *     fragmentManager  = parentFragmentManager,
 *     storyId          = firestoreId,
 *     chapter          = chapter,
 *     onUnlocked       = { navigateToRead() }
 * )
 * ```
 */
class UnlockChapterDialog : DialogFragment() {

    private var storyId: String  = ""
    private var chapter: Chapter? = null
    private var onUnlocked: (() -> Unit)? = null

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            storyId: String,
            chapter: Chapter,
            onUnlocked: () -> Unit
        ) {
            val dialog = UnlockChapterDialog()
            dialog.storyId    = storyId
            dialog.chapter    = chapter
            dialog.onUnlocked = onUnlocked
            dialog.show(fragmentManager, "unlock_chapter")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_unlock_chapter, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val chap          = chapter ?: run { dismiss(); return }
        val currentCoins  = UserSession.currentUser?.coins ?: 0

        val tvChapterName  = view.findViewById<TextView>(R.id.tv_dialog_chapter_name)
        val tvCoinCost     = view.findViewById<TextView>(R.id.tv_dialog_coin_cost)
        val tvCurrentCoins = view.findViewById<TextView>(R.id.tv_dialog_current_coins)
        val tvWarning      = view.findViewById<TextView>(R.id.tv_dialog_warning)
        val btnUnlock      = view.findViewById<Button>(R.id.btn_dialog_unlock)
        val btnCancel      = view.findViewById<Button>(R.id.btn_dialog_cancel)
        val btnBuyCoins    = view.findViewById<Button>(R.id.btn_dialog_buy_coins)
        val progress       = view.findViewById<ProgressBar>(R.id.progress_dialog)

        tvChapterName.text  = "Chương ${chap.chapterNumber}: ${chap.title.ifEmpty { "(không có tên)" }}"
        tvCoinCost.text     = "${chap.coinPrice} 🪙"
        tvCurrentCoins.text = "Ví của bạn: $currentCoins 🪙"

        val canAfford = currentCoins >= chap.coinPrice

        if (!canAfford) {
            tvWarning.visibility   = View.VISIBLE
            tvWarning.text         = "Không đủ coin! Cần thêm ${chap.coinPrice - currentCoins} 🪙"
            btnUnlock.isEnabled    = false
            btnUnlock.alpha        = 0.5f
            btnBuyCoins.visibility = View.VISIBLE
        } else {
            tvWarning.visibility   = View.GONE
            btnBuyCoins.visibility = View.GONE
        }

        btnUnlock.setOnClickListener {
            progress.visibility = View.VISIBLE
            btnUnlock.isEnabled = false
            btnCancel.isEnabled = false

            CoinRepository.unlockChapter(
                storyFirestoreId = storyId,
                chapterNumber    = chap.chapterNumber,
                coinPrice        = chap.coinPrice,
                authorId         = chap.authorId,
                onSuccess = {
                    if (!isAdded) return@unlockChapter
                    progress.visibility = View.GONE
                    Toast.makeText(requireContext(), "Mở khóa thành công! 🎉", Toast.LENGTH_SHORT).show()
                    dismiss()
                    onUnlocked?.invoke()
                },
                onError = { msg ->
                    if (!isAdded) return@unlockChapter
                    progress.visibility = View.GONE
                    btnUnlock.isEnabled = true
                    btnCancel.isEnabled = true
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                }
            )
        }

        btnCancel.setOnClickListener { dismiss() }

        btnBuyCoins.setOnClickListener {
            dismiss()
            // Navigate đến CoinWalletFragment
            parentFragmentManager.beginTransaction()
                .replace(R.id.container, com.example.mangaapp.ui.coin.CoinWalletFragment())
                .addToBackStack(null)
                .commit()
        }

        // Làm cho dialog rộng hơn
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.9).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}