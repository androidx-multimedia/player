package com.androidx.multimedia.player.adapter

import android.graphics.Bitmap
import android.os.Build
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.androidx.multimedia.player.R
import com.androidx.multimedia.player.model.LocalVideoItem

class VideoAdapter(
    private val videos: List<LocalVideoItem>,
    private val onVideoClick: (LocalVideoItem) -> Unit
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivThumbnail: ImageView = itemView.findViewById(R.id.ivThumbnail)
        val tvDuration: TextView = itemView.findViewById(R.id.tvDuration)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvDetails: TextView = itemView.findViewById(R.id.tvDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videos[position]
        holder.tvTitle.text = video.title
        holder.tvDuration.text = video.formattedDuration
        holder.tvDetails.text = "${video.formattedDuration} • ${video.formattedSize}"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val thumbnail: Bitmap = holder.itemView.context.contentResolver.loadThumbnail(
                    video.uri,
                    Size(200, 120),
                    null
                )
                holder.ivThumbnail.setImageBitmap(thumbnail)
            }
        } catch (_: Exception) {
            holder.ivThumbnail.setImageDrawable(null)
        }

        holder.itemView.setOnClickListener {
            onVideoClick(video)
        }
    }

    override fun getItemCount(): Int = videos.size
}
