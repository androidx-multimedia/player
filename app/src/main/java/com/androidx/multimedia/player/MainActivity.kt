package com.androidx.multimedia.player

import android.Manifest
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androidx.multimedia.player.adapter.VideoAdapter
import com.androidx.multimedia.player.model.LocalVideoItem

class MainActivity : AppCompatActivity() {

    private lateinit var rvVideos: RecyclerView
    private lateinit var emptyContainer: View
    private lateinit var tvVideoCount: TextView
    private lateinit var btnGrantPermission: Button
    private lateinit var btnSampleMp4: Button
    private lateinit var btnSampleHls: Button
    private lateinit var btnSampleS22: Button

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            loadLocalVideos()
        } else {
            Toast.makeText(this, "Permission denied to read local videos", Toast.LENGTH_SHORT).show()
            showEmptyState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rvVideos = findViewById(R.id.rvVideos)
        emptyContainer = findViewById(R.id.emptyContainer)
        tvVideoCount = findViewById(R.id.tvVideoCount)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
        btnSampleMp4 = findViewById(R.id.btnSampleMp4)
        btnSampleHls = findViewById(R.id.btnSampleHls)
        btnSampleS22 = findViewById(R.id.btnSampleS22)

        rvVideos.layoutManager = LinearLayoutManager(this)

        btnGrantPermission.setOnClickListener {
            requestStoragePermission()
        }

        btnSampleMp4.setOnClickListener {
            PlayerActivity.startWithUrl(
                context = this,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Big Buck Bunny (Online MP4)",
                subtitle = "Blender Foundation"
            )
        }

        btnSampleHls.setOnClickListener {
            PlayerActivity.startWithUrl(
                context = this,
                url = "https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8",
                title = "Tears of Steel (HLS Adaptive Stream)",
                subtitle = "Unified Streaming"
            )
        }

        btnSampleS22.setOnClickListener {
            PlayerActivity.startWithUrl(
                context = this,
                url = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Samsung Galaxy S22, S22+, and S22 Ultra",
                subtitle = "GSMArena Official"
            )
        }

        checkAndLoadVideos()
    }

    private fun checkAndLoadVideos() {
        val permission = getRequiredPermission()
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            loadLocalVideos()
        } else {
            requestStoragePermission()
        }
    }

    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    private fun requestStoragePermission() {
        permissionLauncher.launch(getRequiredPermission())
    }

    private fun loadLocalVideos() {
        val videoList = mutableListOf<LocalVideoItem>()
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )

        try {
            contentResolver.query(
                collection,
                projection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val name = cursor.getString(nameColumn) ?: "Video_$id"
                    val path = cursor.getString(dataColumn)
                    val duration = cursor.getLong(durationColumn)
                    val size = cursor.getLong(sizeColumn)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

                    videoList.add(
                        LocalVideoItem(
                            id = id,
                            title = name,
                            path = path,
                            uri = contentUri,
                            durationMs = duration,
                            sizeBytes = size
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (videoList.isNotEmpty()) {
            rvVideos.visibility = View.VISIBLE
            emptyContainer.visibility = View.GONE
            tvVideoCount.text = "${videoList.size} Videos"

            val adapter = VideoAdapter(videoList) { selectedVideo ->
                PlayerActivity.startWithUri(
                    context = this,
                    uri = selectedVideo.uri,
                    title = selectedVideo.title,
                    subtitle = "Local Storage Video"
                )
            }
            rvVideos.adapter = adapter
        } else {
            showEmptyState()
        }
    }

    private fun showEmptyState() {
        rvVideos.visibility = View.GONE
        emptyContainer.visibility = View.VISIBLE
        tvVideoCount.text = "0 Videos"
    }
}
