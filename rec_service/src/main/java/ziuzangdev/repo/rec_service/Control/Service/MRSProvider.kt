package ziuzangdev.repo.rec_service.Control.Service

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MRSProvider(
    private val context: Context,
    private val activityClass: Class<*>,
    private val previewView: PreviewView
) : MediaRecordingService.DataListener {
    private var recordingService: MediaRecordingService? = null
    private var isReverseLandscape: Boolean = false
    private var isGrantedPermission: Boolean = false
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            recordingService = (service as MediaRecordingService.RecordingServiceBinder).getService()
            onServiceBound(recordingService)
        }

        override fun onServiceDisconnected(name: ComponentName?) {

        }
    }
    init {
        requirePermission()
    }

    private fun onServiceBound(recordingService: MediaRecordingService?) {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
//                viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_baseline_stop_24)
//                viewBinding.btnMute.visibility = View.INVISIBLE
            }
            MediaRecordingService.RecordingState.STOPPED -> {
//                viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_videocam_24)
//                viewBinding.txtDuration.text = "00:00:00"
//                viewBinding.btnMute.visibility = View.VISIBLE
//                setSoundState(recordingService.isSoundEnabled())
            }
            else -> {
                // no-op
            }
        }

        recordingService?.addListener(this)
        recordingService?.bindPreviewUseCase(previewView.surfaceProvider)
    }
    fun bindService() {
        val intent = Intent(context, MediaRecordingService::class.java)
        intent.action = MediaRecordingService.ACTION_START_WITH_PREVIEW
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    fun onPauseRecordClicked() {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
                println("fffffffffffff stop rec")
                recordingService?.stopRecording()

            }
            MediaRecordingService.RecordingState.STOPPED -> {
                println("fffffffffffff start rec")
                recordingService?.startRecording()
            }
            else -> {
                // no-op
            }
        }
    }
    private fun requirePermission() {
        val cameraPermission = Manifest.permission.CAMERA
        val audioPermission = Manifest.permission.RECORD_AUDIO
        val foregroundServicePermission = Manifest.permission.FOREGROUND_SERVICE
        val storagePermission = Manifest.permission.WRITE_EXTERNAL_STORAGE
        val permissions = arrayOf(cameraPermission, audioPermission, foregroundServicePermission, storagePermission)
        val deniedPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
        }
        isGrantedPermission = deniedPermissions.isEmpty()
    }

    override fun onNewData(duration: Int) {
        if(isGrantedPermission){

        }
    }

    override fun onCameraOpened() {
        if(isGrantedPermission){

        }
    }

    override fun onRecordingEvent(it: VideoRecordEvent?) {
//        if(isGrantedPermission){
            when (it) {
                is VideoRecordEvent.Start -> {

                }

                is VideoRecordEvent.Finalize -> {
                    recordingService?.isSoundEnabled()?.let {

                    }

                    onNewData(0)
                    val intent = Intent(Intent.ACTION_VIEW, it.outputResults.outputUri)
                    intent.setDataAndType(it.outputResults.outputUri, "video/mp4")
                    context.startActivity(Intent.createChooser(intent, "Open recorded video"))
                }
            }
//        }
    }

    private fun onMuteRecordingClicked() {
        if(recordingService == null) return
        var soundEnabled = recordingService?.isSoundEnabled()
        soundEnabled = !soundEnabled!!
        recordingService?.setSoundEnabled(soundEnabled)
    }
}