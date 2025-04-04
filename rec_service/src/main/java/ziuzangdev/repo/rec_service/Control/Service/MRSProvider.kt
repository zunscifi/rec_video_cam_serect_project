package ziuzangdev.repo.rec_service.Control.Service

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.R

class MRSProvider(
    private val context: Context,
    private val activityClass: Class<*>,
    private val previewView: PreviewView,
    private val txtDuration: TextView,
    private val btnRecord: ImageButton,
    private val removeView: RelativeLayout,
    private val openApp: ImageButton,
    private val cbIsShowPreview: CheckBox
) : MediaRecordingService.DataListener {
    private var recordingService: MediaRecordingService? = null
    private var isReverseLandscape: Boolean = false
    private var isGrantedPermission: Boolean = false
    private var settingProvider : SettingProvider? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            recordingService = (service as MediaRecordingService.RecordingServiceBinder).getService()
            onServiceBound(recordingService)
            Log.i("MRSProvider", "onServiceConnected: ")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            Log.i("MRSProvider", "onServiceDisconnected: ")
        }
    }

    init {
        settingProvider = SettingProvider(context)
    }


    fun getRecordService() : MediaRecordingService? {
        return recordingService
    }
    private fun onServiceBound(recordingService: MediaRecordingService?) {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
//                viewBinding.viewRecordPause.setBackgroundResource(R.drawable.ic_baseline_stop_24)
//                viewBinding.btnMute.visibility = View.INVISIBLE
                btnRecord.setImageResource(R.drawable.icon_rec_video_recording)
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
    fun onStopApp(){
        if (recordingService?.getRecordingState() == MediaRecordingService.RecordingState.STOPPED) {
            recordingService?.let {
                ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_REMOVE)
                recordingService?.stopSelf()
            }
        }
        recordingService?.unbindPreview()
        recordingService?.removeListener(this)
    }
    fun unBindAll(){
        recordingService?.unBindAll()
    }
    fun bindService() {
        val intent = Intent(context, MediaRecordingService::class.java)
        intent.action = MediaRecordingService.ACTION_START_WITH_PREVIEW
        context.startService(intent)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    fun un_bindService() {
        recordingService?.setCameraProvider()
        context.stopService(Intent(context, MediaRecordingService::class.java))
        recordingService = null
    }
    fun onPauseRecordClicked() : Boolean {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
                cbIsShowPreview.isEnabled = true
                cbIsShowPreview.isClickable = true
                recordingService?.stopRecording()
                recordingService?.removeBubblePreviewCam()
                //Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                return false

            }
            MediaRecordingService.RecordingState.STOPPED -> {
                cbIsShowPreview.isEnabled = false
                cbIsShowPreview.isClickable = false
                recordingService?.startRecording(previewView)
                val isShowPreview = settingProvider?.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW)?.settingValue.toBoolean()
                if(isShowPreview){
                    recordingService?.initializeBubblePreviewCam(removeView, previewView, activityClass, openApp)
                }
                recordingService?.startRunningInForeground(activityClass)
                return true
            }
            else -> {
                // no-op
                return false
            }
        }
    }
    fun requirePermission() : Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            val deniedPermissions = CAMERA_PERMISSION_FROM30.filter {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
            }
            isGrantedPermission = deniedPermissions.isEmpty()
        }else{
            val deniedPermissions = CAMERA_PERMISSION_LOW30.filter {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_DENIED
            }
            isGrantedPermission = deniedPermissions.isEmpty()
        }
        return isGrantedPermission
    }

    override fun onNewData(duration: Int) {
        if(isGrantedPermission){
            if(context is Activity){
                var seconds = duration
                var minutes = seconds / MINUTE
                seconds %= MINUTE
                val hours = minutes / HOUR
                minutes %= HOUR
                val hoursString = if (hours >= 10) hours.toString() else "0$hours"
                val minutesString = if (minutes >= 10) minutes.toString() else "0$minutes"
                val secondsString = if (seconds >= 10) seconds.toString() else "0$seconds"
                context.runOnUiThread {
                    txtDuration.text = "$hoursString:$minutesString:$secondsString"
                }
            }
        }
    }

    override fun onCameraOpened() {
        if(isGrantedPermission){

        }
    }

    override fun onRecordingEvent(it: VideoRecordEvent?) {
        if(isGrantedPermission){
            when (it) {
                is VideoRecordEvent.Start -> {

                }
                is VideoRecordEvent.Finalize -> {
                    recordingService?.isSoundEnabled()?.let {

                    }
                    btnRecord.setImageResource(R.drawable.icon_rec_video)
                    onNewData(0)
//                    val isShowPreview = settingProvider?.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW)?.settingValue.toBoolean()
//                    if(isShowPreview){
//                        val intent = Intent(Intent.ACTION_VIEW, it.outputResults.outputUri)
//                        intent.setDataAndType(it.outputResults.outputUri, "video/mp4")
//                        context.startActivity(Intent.createChooser(intent, "Open recorded video"))
//                    }
                }
            }
        }
    }

    override fun onRecordingState(it: MediaRecordingService.RecordingState?) {
        if(it == MediaRecordingService.RecordingState.RECORDING){
            btnRecord.setImageResource(R.drawable.icon_rec_video_recording)
            cbIsShowPreview.isEnabled = false
            cbIsShowPreview.isClickable = false
        }else{
            cbIsShowPreview.isEnabled = true
            cbIsShowPreview.isClickable = true
        }
    }

    private fun onMuteRecordingClicked() {
        if(recordingService == null) return
        var soundEnabled = recordingService?.isSoundEnabled()
        soundEnabled = !soundEnabled!!
        recordingService?.setSoundEnabled(soundEnabled)
    }
    companion object {
        const val CAMERA_PERMISSION_REQUEST_CODE = 789
        const val MINUTE: Int = 60
        const val HOUR: Int = MINUTE * 60
        val CAMERA_PERMISSION_LOW30 = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        @RequiresApi(Build.VERSION_CODES.R)
        val CAMERA_PERMISSION_FROM30 = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
    }
}