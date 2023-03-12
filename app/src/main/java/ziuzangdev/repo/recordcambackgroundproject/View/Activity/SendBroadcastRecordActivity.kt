package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ServiceCompat
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.Control.Service.MRSProvider
import ziuzangdev.repo.rec_service.Control.Service.MediaRecordingService
import ziuzangdev.repo.recordcambackgroundproject.Control.BroadcastReceive.ShortcutRecordReceiver
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivitySendBroadcastRecordBinding

class SendBroadcastRecordActivity : Activity(), MediaRecordingService.DataListener {
    private var recordingService: MediaRecordingService? = null
    private var settingProvider : SettingProvider? = null
    private lateinit var binding : ActivitySendBroadcastRecordBinding
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendBroadcastRecordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initSettingProvider(this@SendBroadcastRecordActivity)
        bindService()
        onPauseRecordClicked()
        finish()
    }
    private fun initSettingProvider(context: Context?) {
        if(context != null){
            settingProvider = SettingProvider(context)
        }
    }
    fun getRecordService() : MediaRecordingService? {
        return recordingService
    }
    private fun onServiceBound(recordingService: MediaRecordingService?) {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {

            }
            MediaRecordingService.RecordingState.STOPPED -> {

            }
            else -> {
                // no-op
            }
        }

        recordingService?.addListener(this)
        recordingService?.bindPreviewUseCase(binding.previewContainer.surfaceProvider)
    }
    fun onStopApp(){
        if (recordingService?.getRecordingState() == MediaRecordingService.RecordingState.STOPPED) {
            recordingService?.let {
                ServiceCompat.stopForeground(it, ServiceCompat.STOP_FOREGROUND_REMOVE)
                recordingService?.stopSelf()
            }
        } else {
            recordingService?.startRunningInForeground(SendBroadcastRecordActivity::class.java)
        }
        recordingService?.unbindPreview()
        recordingService?.removeListener(this)
    }
    fun unBindAll(){
        recordingService?.unBindAll()
    }
    fun bindService() {
        val intent = Intent(this@SendBroadcastRecordActivity, MediaRecordingService::class.java)
        intent.action = MediaRecordingService.ACTION_START_WITH_PREVIEW
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    fun un_bindService() {
        recordingService?.setCameraProvider()
        stopService(Intent(this@SendBroadcastRecordActivity, MediaRecordingService::class.java))
        recordingService = null
    }
    fun onPauseRecordClicked() : Boolean {
        when(recordingService?.getRecordingState()){
            MediaRecordingService.RecordingState.RECORDING -> {
//                cbIsShowPreview.isEnabled = true
//                cbIsShowPreview.isClickable = true
                recordingService?.stopRecording()
//                recordingService?.removeBubblePreviewCam()
                //Toast.makeText(context, "Video saved", Toast.LENGTH_SHORT).show()
                return false

            }
            MediaRecordingService.RecordingState.STOPPED -> {
//                cbIsShowPreview.isEnabled = false
//                cbIsShowPreview.isClickable = false
                recordingService?.startRecording(binding.previewContainer)
//                val isShowPreview = settingProvider?.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW)?.settingValue.toBoolean()
//                if(isShowPreview){
//                    recordingService?.initializeBubblePreviewCam(removeView, previewView, activityClass, openApp)
//                }
                return true
            }
            else -> {
                // no-op
                return false
            }
        }
    }

    override fun onNewData(duration: Int) {

    }

    override fun onCameraOpened() {

    }

    override fun onRecordingEvent(it: VideoRecordEvent?) {

    }

    override fun onRecordingState(it: MediaRecordingService.RecordingState?) {

    }
}