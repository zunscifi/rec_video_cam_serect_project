package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.Control.Service.MRSProvider
import ziuzangdev.repo.rec_service.Control.Service.MediaRecordingService
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(){
    private lateinit var viewBinding: ActivityMainBinding
    private var mrsProvider: MRSProvider? = null
    private var settingProvider : SettingProvider? = null

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MRSProvider.CAMERA_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    // All permissions granted
                    initMRSProvider()
                } else {
                    // Permissions not granted
                    val deniedPermissions = mutableListOf<String>()
                    for (i in permissions.indices) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            deniedPermissions.add(permissions[i])
                        }
                    }
                    Toast.makeText(this, "Permission denied: $deniedPermissions", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        addControls()
        addEvents()
    }

    override fun onResume() {
        super.onResume()

    }
    override fun onStop() {
        super.onStop()
        mrsProvider?.onStopApp()
    }

    private fun addEvents() {
        viewBinding.btnRecord.setOnClickListener(){
           val isStartRecord = mrsProvider?.onPauseRecordClicked()
            if(isStartRecord == false){
                initMRSProvider()
            }
        }

        viewBinding.cbIsShowPreview.setOnCheckedChangeListener { buttonView, isChecked ->
            settingProvider?.saveSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW, isChecked.toString())
        }
    }

    private fun addControls() {
        initMRSProvider()
        initSettingProvider()
        initDefaultValue()
    }

    private fun initDefaultValue() {
        val isShowPreview = settingProvider?.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW)?.settingValue.toBoolean()
        viewBinding.cbIsShowPreview.isChecked = isShowPreview
    }

    private fun initSettingProvider() {
        settingProvider = SettingProvider(this@MainActivity)
    }

    private fun initMRSProvider() {
        mrsProvider = MRSProvider(this@MainActivity, MainActivity::class.java,
            viewBinding.previewContainer, viewBinding.txtDuration, viewBinding.btnRecord)
        if(mrsProvider?.requirePermission() == true){
            mrsProvider?.bindService()
        }else{
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            ActivityCompat.requestPermissions(this, MRSProvider.CAMERA_PERMISSION_FROM30, MRSProvider.CAMERA_PERMISSION_REQUEST_CODE)
        }else{
            ActivityCompat.requestPermissions(this, MRSProvider.CAMERA_PERMISSION_LOW30, MRSProvider.CAMERA_PERMISSION_REQUEST_CODE)
        }
    }
}