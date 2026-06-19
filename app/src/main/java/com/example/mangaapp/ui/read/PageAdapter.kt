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
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.load.DataSource
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
            .asBitmap()
            .load(imageUrls[position])
            .apply(
                RequestOptions()
                    // ARGB_8888: 4 byte/pixel — giữ đủ màu sắc, không nén mất chất
                    .format(DecodeFormat.PREFER_ARGB_8888)
                    // Cache cả ảnh gốc lẫn ảnh đã transform
                    .diskCacheStrategy(DiskCacheStrategy.DATA)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_close_clear_cancel)
                    // KHÔNG dùng .override() — BilinearFitWidthTransform đã tự resize
                    // Dùng cả hai cùng lúc khiến Glide resize 2 lần → GPU overload → crash
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

        if (srcW <= targetWidth) return toTransform

        val scale     = targetWidth.toFloat() / srcW
        val newHeight = (srcH * scale).toInt()

        val output = Bitmap.createBitmap(targetWidth, newHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint  = Paint().apply {
            isAntiAlias    = true

            isFilterBitmap = true
        }

        val dest = android.graphics.RectF(0f, 0f, targetWidth.toFloat(), newHeight.toFloat())
        canvas.drawBitmap(toTransform, null, dest, paint)

        return output
    }

    override fun updateDiskCacheKey(messageDigest: MessageDigest) {
        messageDigest.update("BilinearFitWidth_$targetWidth".toByteArray())
    }

    override fun equals(other: Any?) =
        other is BilinearFitWidthTransform && other.targetWidth == targetWidth

    override fun hashCode() = targetWidth
}