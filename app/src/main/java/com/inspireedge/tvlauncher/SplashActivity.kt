package com.inspireedge.tvlauncher

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var videoView: VideoView
    private lateinit var handler: Handler
    private var isVideoFinished = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        videoView = findViewById(R.id.videoView)
        handler = Handler(Looper.getMainLooper())

        // Set the video from raw resources
        val videoUri = Uri.parse("android.resource://${packageName}/${R.raw.spac_os_boot}")
        videoView.setVideoURI(videoUri)

        // Set up video completion listener
        videoView.setOnCompletionListener {
            isVideoFinished = true
            navigateToMain()
        }

        // Set up error listener - fallback to main activity on error
        videoView.setOnErrorListener { _, what, extra ->
            // If video fails, still navigate to main
            isVideoFinished = true
            navigateToMain()
            true // Consume the error
        }

        // Auto-play
        videoView.start()

        // Safety timeout - always navigate after 5 seconds max
        handler.postDelayed({
            if (!isVideoFinished) {
                isVideoFinished = true
                navigateToMain()
            }
        }, 5000)
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        videoView.resume()
    }

    override fun onPause() {
        super.onPause()
        videoView.suspend()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
