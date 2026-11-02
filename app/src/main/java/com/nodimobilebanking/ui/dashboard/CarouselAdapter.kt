package com.nodimobilebanking.ui.dashboard

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.nodimobilebanking.databinding.ItemCarouselBinding

data class CarouselItem(
    val title: String,
    val subtitle: String,
    val bgColor: String = "#0D2C6D",  // default primary blue
    val imageUrl: String? = null
)

class CarouselAdapter(private val items: List<CarouselItem>) :
    RecyclerView.Adapter<CarouselAdapter.VH>() {

    inner class VH(val b: ItemCarouselBinding) : RecyclerView.ViewHolder(b.root) {
        fun bind(item: CarouselItem) {
            b.tvBannerTitle.text = item.title
            b.tvBannerSubtitle.text = item.subtitle

            if (!item.imageUrl.isNullOrBlank()) {
                // ── Image mode: show uploaded promotion image with overlay ──
                b.ivBanner.visibility = View.VISIBLE
                b.imageOverlay.visibility = View.VISIBLE
                b.textBanner.setBackgroundColor(Color.TRANSPARENT)

                Glide.with(b.root.context)
                    .load(item.imageUrl)
                    .transition(DrawableTransitionOptions.withCrossFade(300))
                    .centerCrop()
                    .into(b.ivBanner)

                // Text sits over the gradient overlay for readability
                b.tvBannerTitle.setTextColor(Color.WHITE)
                b.tvBannerSubtitle.setTextColor(Color.parseColor("#E0E0E0"))
            } else {
                // ── Text-only fallback: colored card ──
                b.ivBanner.visibility = View.GONE
                b.imageOverlay.visibility = View.GONE
                b.textBanner.setBackgroundColor(Color.parseColor(item.bgColor))
                b.tvBannerTitle.setTextColor(Color.WHITE)
                b.tvBannerSubtitle.setTextColor(Color.parseColor("#E0E0E0"))
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position % items.size])

    override fun getItemCount() = if (items.isEmpty()) 0 else items.size * 100  // infinite scroll
}
