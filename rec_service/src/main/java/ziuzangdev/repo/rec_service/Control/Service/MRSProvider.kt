package ziuzangdev.repo.rec_service.Control.Service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MRSProvider(
    private val context: Context,
    private val activityClass: Class<out Activity>
) : MediaRecordingService.DataListener {
    private var recordingService: MediaRecordingService? = null
    private var isReverseLandscape: Boolean = false
    private var isGrantedPermission: Boolean = false
    init {
        requirePermission()
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
        if(isGrantedPermission){

        }
    }

    private fun onMuteRecordingClicked() {
        if(recordingService == null) return
        var soundEnabled = recordingService?.isSoundEnabled()
        soundEnabled = !soundEnabled!!
        recordingService?.setSoundEnabled(soundEnabled)
    }
}