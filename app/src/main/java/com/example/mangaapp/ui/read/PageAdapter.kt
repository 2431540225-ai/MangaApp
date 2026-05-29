package com.example.mangaapp.ui.read

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.transition.Transition
import com.example.mangaapp.R
import java.security.MessageDigest

class PageAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPage: ImageView = view.findViewById(R.id.iv_page)
        val progressBar: ProgressBar = view.findViewById(R.id.progress_bar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        holder.progressBar.visibility = View.VISIBLE

        // Lấy chiều rộng màn hình thực tế để làm target width
        val screenWidth = holder.itemView.context.resources.displayMetrics.widthPixels

        Glide.with(holder.itemView.context)
            .asBitmap()                                  // Load dưới dạng Bitmap để xử lý thủ công
            .load(imageUrls[position])
            .apply(
                RequestOptions()
                    // ARGB_8888: 4 byte/pixel — giữ đủ màu sắc, không nén mất chất
                    // (mặc định Glide dùng RGB_565 chỉ 2 byte/pixel → mất màu)
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    // Cache cả ảnh gốc lẫn ảnh đã transform
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    // Override target size = chiều rộng màn hình × chiều cao tự tính
                    // Glide sẽ decode ảnh gốc rồi scale xuống đúng kích thước này
                    .override(screenWidth, Target.SIZE_ORIGINAL)
                    // Thuật toán Bilinear khi scale xuống:
                    // Thay vì bỏ pixel (nearest neighbor), lấy trung bình 4 pixel lân cận
                    // → ảnh mịn hơn, không bị răng cưa khi thu nhỏ từ 500→300
                    .transform(BilinearFitWidthTransform(screenWidth))
            )
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: com.bumptech.glide.load.engine.GlideException?,
                    model: Any?,
                    target: Target<Bitmap>,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: Target<Bitmap>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    holder.progressBar.visibility = View.GONE
                    return false
                }
            })
            .into(holder.ivPage)
    }

    override fun getItemCount() = imageUrls.size
}

/**
 * BitmapTransformation: Scale ảnh vừa chiều ngang màn hình (FitWidth)
 * bằng thuật toán Bilinear — lấy trung bình 4 pixel lân cận khi thu nhỏ.
 *
 * Ví dụ: ảnh tác giả 500×800, màn hình 360px rộng
 *   → scale xuống 360×576 (giữ đúng tỉ lệ 500:800 = 360:576)
 *   → dùng Paint.FILTER_BITMAP_FLAG để bật Bilinear filter
 *
 * Không dùng khi phóng to (ảnh nhỏ hơn màn hình) vì sẽ bị vỡ pixel.
 */
class BilinearFitWidthTransform(
    private val targetWidth: Int
) : BitmapTransformation() {

    override fun transform(
        pool: BitmapPool,
        toTransform: Bitmap,
        outWidth: Int,
        outHeight: Int
    ): Bitmap {
        val srcW = toTransform.width
        val srcH = toTransform.height

        // Nếu ảnh đã nhỏ hơn hoặc bằng màn hình → không scale, trả về nguyên bản
        // (scale lên sẽ làm vỡ pixel, không có ích)
        if (srcW <= targetWidth) return toTransform

        // Tính chiều cao mới giữ đúng tỉ lệ gốc
        val scale     = targetWidth.toFloat() / srcW
        val newHeight = (srcH * scale).toInt()

        // Lấy Bitmap từ pool để tránh cấp phát bộ nhớ mới (Glide best practice)
        val output = pool.get(targetWidth, newHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint  = Paint().apply {
            isAntiAlias       = true
            // FILTER_BITMAP_FLAG = bật Bilinear interpolation khi vẽ
            // Thay vì chọn pixel gần nhất (nearest neighbor), Android sẽ
            // tính trung bình có trọng số của 4 pixel xung quanh → mịn hơn
            isFilterBitmap    = true
        }

        val src  = android.graphics.RectF(0f, 0f, srcW.toFloat(), srcH.toFloat())
        val dest = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), newHeight.toFloat())
        canvas.drawBitmap(toTransform, null, dest, paint)

        return output
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        // Key để Glide nhận ra transform này khác với transform khác khi cache
        messageDigest.update("BilinearFitWidth_$targetWidth".toByteArray())
    }

    override fun equals(other: Any?) =
        other is BilinearFitWidthTransform && other.targetWidth == targetWidth

    override fun hashCode() = targetWidth
}
