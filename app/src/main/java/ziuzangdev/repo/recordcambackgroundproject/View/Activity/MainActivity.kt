package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.QualitySelector
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import nl.invissvenska.modalbottomsheetdialog.Item
import nl.invissvenska.modalbottomsheetdialog.ModalBottomSheetDialog
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.Control.Service.MRSProvider
import ziuzangdev.repo.rec_service.Control.Service.MediaRecordingService
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding
import java.util.Locale


class MainActivity : AppCompatActivity(), ModalBottomSheetDialog.Listener{
    private lateinit var viewBinding: ActivityMainBinding
    private var mrsProvider: MRSProvider? = null
    private var settingProvider : SettingProvider? = null
    private lateinit var modalBottomSheetDialog : ModalBottomSheetDialog
    private val SYSTEM_ALERT_WINDOW_PERMISSION = 2084
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == SYSTEM_ALERT_WINDOW_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                showPreviewOverlay()
            } else {
                // Permission not granted, handle accordingly
            }
        }
    }

    private fun showPreviewOverlay() {
        settingProvider?.saveSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW, "true")
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
        reloadSettup()
    }
    override fun onStop() {
        super.onStop()
        mrsProvider?.onStopApp()
    }

    override fun onDestroy() {
        super.onDestroy()
        mrsProvider?.onStopApp()
    }
    private fun addEvents() {
        viewBinding.btnRecord.setOnClickListener(){
           val isStartRecord = mrsProvider?.onPauseRecordClicked()
            if(isStartRecord == false){
                reloadSettupForStopRecord()
            }
        }

        viewBinding.cbIsShowPreview.setOnClickListener(){
            if(viewBinding.cbIsShowPreview.isChecked){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                    if(!checkPermisionOverlayScreen()){
                        viewBinding.cbIsShowPreview.isChecked = false
                    }
                }else{
                    showPreviewOverlay()
                }
            }else{
                settingProvider?.saveSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW, "false")
            }
            reloadSettup()
        }
        viewBinding.imgbtnVideoManager.setOnClickListener(){
            val intent: Intent = Intent(this@MainActivity, VideoManagerActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        viewBinding.imgbtnSettingBottomDialog.setOnClickListener(){
            val intent = Intent(this@MainActivity, SettingActivity::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
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
        viewBinding.txtDuration.text = "00:00:00"
        val inforStr = StringBuilder()
        val settingValue = settingProvider?.loadSetting(SettingLogic.SETTING_CAMERA)?.settingValue
        if(settingValue == "CAMERA_FRONT") {
            inforStr.append("Front Camera")
        }else{
            inforStr.append("Back Camera")
        }
        inforStr.append(" - ")
        val quality = settingProvider?.loadSetting(SettingLogic.SETTING_CAMERA_RESOLUTION)?.settingValue
        when(quality){
            "8" -> inforStr.append("UHD")
            "6" -> inforStr.append("FHD")
            "5" -> inforStr.append("HD")
            "4" -> inforStr.append("SD")
            "" -> inforStr.append("UHD")
        }
        viewBinding.txtInforCam.text = inforStr.toString().trim().uppercase(Locale.getDefault())
    }

    private fun initSettingProvider() {
        settingProvider = SettingProvider(this@MainActivity)
    }

    private fun initMRSProvider() {
        val inflater = LayoutInflater.from(this@MainActivity)
        val removeView = inflater.inflate(R.layout.item_preview_overlay, null) as RelativeLayout
        val removeImg = removeView.findViewById<View>(R.id.preview_container) as PreviewView
        val openApp = removeView.findViewById<View>(R.id.imgbtn_open_app) as ImageButton
        val isShowPreview = settingProvider?.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW)?.settingValue.toBoolean()
        if(isShowPreview){
            mrsProvider = MRSProvider(this@MainActivity, MainActivity::class.java,
                removeImg, viewBinding.txtDuration, viewBinding.btnRecord, removeView, openApp, viewBinding.cbIsShowPreview)
        }else{
            mrsProvider = MRSProvider(this@MainActivity, MainActivity::class.java,
                viewBinding.previewContainer, viewBinding.txtDuration, viewBinding.btnRecord, removeView, openApp, viewBinding.cbIsShowPreview)
        }
        if(mrsProvider?.requirePermission() == true){
            mrsProvider?.bindService()
        }else{
            requestPermissions()
        }
    }

    private fun checkPermisionOverlayScreen() : Boolean{
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this@MainActivity)) {
           requestPermissionOverlayScreen()
            return false
        } else {
            showPreviewOverlay()
            return true
        }
    }
    private fun requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){
            ActivityCompat.requestPermissions(this, MRSProvider.CAMERA_PERMISSION_FROM30, MRSProvider.CAMERA_PERMISSION_REQUEST_CODE)
        }else{
            ActivityCompat.requestPermissions(this, MRSProvider.CAMERA_PERMISSION_LOW30, MRSProvider.CAMERA_PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestPermissionOverlayScreen(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + packageName))
            startActivityForResult(intent, SYSTEM_ALERT_WINDOW_PERMISSION)
        }
    }
    override fun onItemSelected(tag: String?, item: Item?) {
            modalBottomSheetDialog.dismiss()
            if(item?.id   == R.id.action_quality){
                openQuality()
                modalBottomSheetDialog.show(supportFragmentManager, "Setting")
            }else if(item?.id  == R.id.action_camera){
                openCamera()
                modalBottomSheetDialog.show(supportFragmentManager, "Setting")
            }else if(item?.id == R.id.DEFAULT_BACK_CAMERA){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA, "CAMERA_BLACK")
                reloadSettup()
            }else if(item?.id == R.id.DEFAULT_FRONT_CAMERA){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA, "CAMERA_FRONT")
                reloadSettup()
            }else if(item?.id == R.id.QUALITY_UHD){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_UHD.toString())
                reloadSettup()
            }
            else if(item?.id == R.id.QUALITY_FHD){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_FHD.toString())
                reloadSettup()
            }
            else if(item?.id == R.id.QUALITY_HD){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_HD.toString())
                reloadSettup()
            }
            else if(item?.id == R.id.QUALITY_SD){
                settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_SD.toString())
                reloadSettup()
            }
    }

    private fun openCamera() {
        modalBottomSheetDialog.dismiss()
        modalBottomSheetDialog = ModalBottomSheetDialog.Builder()
            .setHeader("Camera Selector")
            .add(R.menu.setting_camera)
            .setRoundedModal(true)
            .setHeaderLayout(R.layout.setting_dialog_header)
            .setItemLayout(R.layout.setting_dialog_item)
            .build()
        if(!modalBottomSheetDialog.isAdded){
            modalBottomSheetDialog.show(supportFragmentManager, "Setting")
        }else{
        }
    }
    private fun openSetting(){
        modalBottomSheetDialog = ModalBottomSheetDialog.Builder()
            .setHeader("Setting")
            .add(R.menu.setting)
            .setRoundedModal(true)
            .setHeaderLayout(R.layout.setting_dialog_header)
            .setItemLayout(R.layout.setting_dialog_item)
            .build()
        if(!modalBottomSheetDialog.isAdded){
            modalBottomSheetDialog.show(supportFragmentManager, "Setting")
        }else{
        }
    }
    private fun openQuality() {
        modalBottomSheetDialog = ModalBottomSheetDialog.Builder()
            .setHeader("Quality Selector")
            .add(R.menu.setting_quality)
            .setRoundedModal(true)
            .setHeaderLayout(R.layout.setting_dialog_header)
            .setItemLayout(R.layout.setting_dialog_item)
            .build()
        if(!modalBottomSheetDialog.isAdded){
            modalBottomSheetDialog.show(supportFragmentManager, "Setting")
        }else{
        }
    }

    private fun reloadSettup(){
        Toast.makeText(this@MainActivity, mrsProvider?.getRecordService()?.getRecordingState().toString(), Toast.LENGTH_SHORT).show()
        if(mrsProvider?.getRecordService()?.getRecordingState()  != MediaRecordingService.RecordingState.RECORDING){
            initDefaultValue()
            mrsProvider?.un_bindService()
            mrsProvider?.bindService()
            val isStartRecord = mrsProvider?.onPauseRecordClicked()
            if(isStartRecord == false){
                initMRSProvider()
            }
        }
    }
    private fun reloadSettupForStopRecord(){
        Toast.makeText(this@MainActivity, mrsProvider?.getRecordService()?.getRecordingState().toString(), Toast.LENGTH_SHORT).show()
            initDefaultValue()
            mrsProvider?.un_bindService()
            mrsProvider?.bindService()
            val isStartRecord = mrsProvider?.onPauseRecordClicked()
            if(isStartRecord == false){
                initMRSProvider()
            }
    }
}