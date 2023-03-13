package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.view.Gravity.CENTER
import android.view.Gravity.END
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.QualitySelector
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.geniusforapp.fancydialog.builders.FancyDialogBuilder
import guy4444.smartrate.SmartRate
import nl.invissvenska.modalbottomsheetdialog.Item
import nl.invissvenska.modalbottomsheetdialog.ModalBottomSheetDialog
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.Control.Service.MRSProvider
import ziuzangdev.repo.rec_service.Control.Service.MediaRecordingService
import ziuzangdev.repo.recordcambackgroundproject.Model.MySmartRate
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding
import java.util.Locale


class MainActivity : AppCompatActivity(), ModalBottomSheetDialog.Listener {
    private lateinit var viewBinding: ActivityMainBinding
    private var mrsProvider: MRSProvider? = null
    private var settingProvider : SettingProvider? = null
    private lateinit var modalBottomSheetDialog : ModalBottomSheetDialog
    private val SYSTEM_ALERT_WINDOW_PERMISSION = 2084
    private var isFinishCountdown = true
    private var isRest = false
    private val callback = object : OnBackPressedCallback(true /* enabled by default */) {
        override fun handleOnBackPressed() {
            showRatingDialog()
        }
    }
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
        try{
            onBackPressedDispatcher.addCallback(this, callback)
            addControls()
            addEvents()
        }catch (e : Exception){
            reloadSettup()
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

    }

    private fun showRatingDialog(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val timeOpenApp = settingProvider?.loadSetting(SettingLogic.TIME_OPEN_APP)?.settingValue
            if(timeOpenApp != MySmartRate.STOP_RATE_REQUEST_SIGNAL){
                if(timeOpenApp == ""){
                    settingProvider?.saveSetting(SettingLogic.TIME_OPEN_APP, "1")
                    finish()
                }
                else {
                    try{
                        var timeOpenAppInt = timeOpenApp?.toInt()
                        timeOpenAppInt = timeOpenAppInt?.plus(1)
                        settingProvider?.saveSetting(SettingLogic.TIME_OPEN_APP, timeOpenAppInt.toString())
                        if (timeOpenAppInt != null) {
                            Toast.makeText(this@MainActivity, timeOpenAppInt.toString(), Toast.LENGTH_SHORT).show()
                            if(timeOpenAppInt % 2 == 0) {
                                MySmartRate.Rate(
                                    this@MainActivity,
                                    "Rate Us",
                                    "Tell others what you think about this app",
                                    "Continue",
                                    "Please take a moment and rate us on Google Play",
                                    "click here",
                                    "Cancel",
                                    "Thanks for the feedback",
                                    resources.getColor(R.color.secondary_color, null),
                                    4
                                )
                            }else{
                                finish()
                            }
                        }
                    }catch (ignored: Exception){}
                }
            }
            else{
                finish()
            }
        }
    }
    override fun onResume() {
        super.onResume()
        reloadSettup()
    }

    override fun onBackPressed() {
        super.onBackPressed()
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
        viewBinding.imgbtnGift.setOnClickListener(){
            val dialog = FancyDialogBuilder(this, R.style.CustomDialog)
                .withTextGravity(CENTER)
                .withPanelGravity(END)
                .withTitle("Special gift for you!\n")
                .withSubTitle("Click the button below to discover our new app on CHPlay and unlock a world of possibilities! As a thank you, we're also giving you exclusive access to a selection of top-rated apps on our platform. Don't miss out on this amazing opportunity!")
                .withPositive("Get Gift!") { view, dialog ->
                    dialog.dismiss()
                    val browserIntent = Intent(Intent.ACTION_VIEW, "https://www.youtube.com/watch?v=QH2-TGUlwu4".toUri())
                    startActivity(browserIntent)
                }
            dialog.show()
        }
        viewBinding.btnRecord.setOnClickListener(){
            if(isFinishCountdown){
                val countDownTimer = object: CountDownTimer(1000, 1000) {
                    override fun onTick(millisUntilFinished: Long) {
                        // Called every second, update UI
                        val secondsRemaining = millisUntilFinished / 1000
                        println("Seconds remaining: $secondsRemaining")
                    }

                    override fun onFinish() {
                        // Called when the countdown finishes
                        println("Countdown finished!")
                        isFinishCountdown = true
                    }
                }
                isFinishCountdown = false
                val isStartRecord = mrsProvider?.onPauseRecordClicked()
                if(isStartRecord == false){
                    reloadSettupForStopRecord()
                }
                countDownTimer.start()
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
        initDefaultValue()
        initMRSProvider()
        initSettingProvider()
    }
    private fun initDefaultValue() {
        try{
            val resourceID = settingProvider?.loadSetting(SettingLogic.SETTING_BACKGROUND_IMAGE)?.settingValue
            Glide.with(this).load(resourceID?.toInt()).into(viewBinding.imgBackground)
        }catch (e : Exception){
            val uriBackground = settingProvider?.loadSetting(SettingLogic.SETTING_BACKGROUND_IMAGE)?.settingValue?.toUri()
            Glide.with(this).load(uriBackground).into(viewBinding.imgBackground)
        }catch (e : Exception){
            println("fffffffffff ${e.message}")
        }
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
            initDefaultValue()
            mrsProvider?.un_bindService()
            mrsProvider?.bindService()
            val isStartRecord = mrsProvider?.onPauseRecordClicked()
            if(isStartRecord == false){
                initMRSProvider()
            }
    }

}