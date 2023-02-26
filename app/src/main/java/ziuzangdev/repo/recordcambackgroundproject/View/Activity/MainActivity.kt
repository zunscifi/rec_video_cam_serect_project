package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ServiceCompat
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), MediaRecordingService.DataListener {
    private var recordingService: MediaRecordingService? = null
    private lateinit var viewBinding: ActivityMainBinding
    private var isReverseLandscape: Boolean = false
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.i("kkkkkkkkkkk", "onServiceConnected")
            recordingService = (service as MediaRecordingService.RecordingServiceBinder).getService()
            onServiceBound(recordingService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.btnRecord.setOnClickListener {
            Toast.makeText(this@MainActivity, "kalala", Toast.LENGTH_SHORT).show()
            onPauseRecordClicked()
        }
        viewBinding.btnMute.setOnClickListener { onMuteRecordingClicked() }
        viewBinding.btnRotate.setOnClickListener {
            requestedOrientation = if (isReverseLandscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            }
            isReverseLandscape = !isReverseLandscape
        }
        viewBinding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun onMuteRecordingClicked() {
        if(recordingService == null) return
        var soundEnabled = recordingService?.isSoundEnabled()
        soundEnabled = !soundEnabled!!
        recordingService?.setSoundEnabled(soundEnabled)
        setSoundState(soundEnabled)
    }

    private fun setSoundState(soundEnabled: Boolean) {
        if (soundEnabled){
//            viewBinding.viewMute.setBackgroundResource(R.drawable.ic_volume_up_24)
        } else {
//            viewBinding.viewMute.setBackgroundResource(R.drawable.ic_volume_off_24)
        }
    }

    private fun bindService() {
        val intent = Intent(this, MediaRecordingService::class.java)
        intent.action = MediaRecordingService.ACTION_START_WITH_PREVIEW
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStart() {
        super.onStart()
        bindService()
    }

    private fun onServiceBound(recordingService: MediaRecordingService?) {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
             //   viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_baseline_stop_24)
                viewBinding.btnMute.visibility = View.INVISIBLE
            }
            MediaRecordingService.RecordingState.STOPPED -> {
                //viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_videocam_24)
                viewBinding.txtDuration.text = "00:00:00"
                viewBinding.btnMute.visibility = View.VISIBLE
                setSoundState(recordingService.isSoundEnabled())
            }
            else -> {
                // no-op
            }
        }

        recordingService?.addListener(this)
        recordingService?.bindPreviewUseCase(viewBinding.previewContainer.surfaceProvider)
    }

    private fun onPauseRecordClicked() {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
                Log.i("kkkkkkkkkkk", "REC")
                recordingService?.stopRecording()
               // viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_videocam_24)
                viewBinding.txtDuration.text = "00:00:00"
            }
            MediaRecordingService.RecordingState.STOPPED -> {
                Log.i("kkkkkkkkkkk", "STOPPED")
               // viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_baseline_stop_24)
                recordingService?.startRecording()
            }
            else -> {
                Log.i("kkkkkkkkkkk", "NO")

            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onNewData(duration: Int) {
        runOnUiThread {
            var seconds = duration
            var minutes = seconds / MINUTE
            seconds %= MINUTE
            val hours = minutes / HOUR
            minutes %= HOUR

            val hoursString = if (hours >= 10) hours.toString() else "0$hours"
            val minutesString = if (minutes >= 10) minutes.toString() else "0$minutes"
            val secondsString = if (seconds >= 10) seconds.toString() else "0$seconds"
            viewBinding.txtDuration.text = "$hoursString:$minutesString:$secondsString"
        }
    }

    override fun onCameraOpened() {

    }

    override fun onRecordingEvent(it: VideoRecordEvent?) {
        when (it) {
            is VideoRecordEvent.Start -> {
                viewBinding.btnMute.visibility = View.INVISIBLE
               // viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_baseline_stop_24)
            }

            is VideoRecordEvent.Finalize -> {
                recordingService?.isSoundEnabled()?.let { it1 -> setSoundState(it1) }
                viewBinding.btnMute.visibility = View.VISIBLE
              //  viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_videocam_24)
                onNewData(0)
                val intent = Intent(Intent.ACTION_VIEW, it.outputResults.outputUri)
                intent.setDataAndType(it.outputResults.outputUri, "video/mp4")
                startActivity(Intent.createChooser(intent, "Open recorded video"))
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (recordingService?.getRecordingState() == MediaRecordingService.RecordingState.STOPPED) {
            recordingService?.let {
                ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_REMOVE)
                recordingService?.stopSelf()
            }
        } else {
            recordingService?.startRunningInForeground()
        }
        recordingService?.unbindPreview()
        recordingService?.removeListener(this)
    }

    companion object {
        private const val MINUTE: Int = 60
        private const val HOUR: Int = MINUTE * 60
    }
}