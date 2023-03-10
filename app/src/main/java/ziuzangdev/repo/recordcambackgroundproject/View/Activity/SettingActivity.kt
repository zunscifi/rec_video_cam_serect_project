package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.video.QualitySelector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codekidlabs.storagechooser.StorageChooser
import nl.invissvenska.modalbottomsheetdialog.Item
import nl.invissvenska.modalbottomsheetdialog.ModalBottomSheetDialog
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivitySettingBinding


class SettingActivity : AppCompatActivity(), ModalBottomSheetDialog.Listener {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var modalBottomSheetDialog : ModalBottomSheetDialog
    private var settingProvider : SettingProvider? = null
    private val REQUEST_CODE_PERMISSIONS = 101
    private val REQUIRED_PERMISSIONS = arrayOf(
        "android.permission.WRITE_EXTERNAL_STORAGE",
        "android.permission.READ_EXTERNAL_STORAGE"
    )
    private var chooser : StorageChooser ? = null
    var mSelected_files: List<String>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        addControls()
        addEvents()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

    }
    private fun addEvents() {
        binding.imgbtnBack.setOnClickListener{
            finish()
        }
        binding.llSettingVideoCamera.setOnClickListener {
            openCamera()
        }

        binding.llSettingVideoQuality.setOnClickListener {
            openQuality()
        }

        binding.llSettingPreviewSize.setOnClickListener {
            openPreviewSize()
        }

        binding.llSettingVideoPath.setOnClickListener {
            openDirectoryPicker()
        }
        chooser?.setOnSelectListener { path ->
            run {
                if(isPathInPublicDirectory(path)){
                    settingProvider?.saveSetting(SettingLogic.SETTING_SAVE_PATH, path)
                    reloadData()
                }else{
                    Toast.makeText(this@SettingActivity, "Your path must be DCIM, Movies or Pictures", Toast.LENGTH_SHORT).show()
                }
            }
        }

    }

    private fun addControls() {
        initSettingProvider()
        initDerectiryChooser()
        reloadData()
    }

    fun isPathInPublicDirectory(filePath: String): Boolean {
        // Get the root directories of the public storage directories
        val dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
        val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)

        // Check if the file path starts with the path of any of the public directories
        return filePath.startsWith(dcimDir.path) ||
                filePath.startsWith(moviesDir.path) ||
                filePath.startsWith(picturesDir.path)
    }


    private fun initDerectiryChooser() {
        // Initialize Builder
        chooser = StorageChooser.Builder()
            .withActivity(this@SettingActivity)
            .withFragmentManager(fragmentManager)
            .withMemoryBar(true)
            .allowCustomPath(true)
            .setType(StorageChooser.DIRECTORY_CHOOSER)
            .build()

    }

    private fun initSettingProvider() {
        settingProvider = SettingProvider(this@SettingActivity)
    }
    override fun onItemSelected(tag: String?, item: Item?) {
        modalBottomSheetDialog.dismiss()
        if(item?.id == R.id.DEFAULT_BACK_CAMERA){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA, "CAMERA_BLACK")
        }else if(item?.id == R.id.DEFAULT_FRONT_CAMERA){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA, "CAMERA_FRONT")
        }else if(item?.id == R.id.QUALITY_UHD){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_UHD.toString())
        }
        else if(item?.id == R.id.QUALITY_FHD){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_FHD.toString())
        }
        else if(item?.id == ziuzangdev.repo.recordcambackgroundproject.R.id.QUALITY_HD){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_HD.toString())
        }
        else if(item?.id == R.id.QUALITY_SD){
            settingProvider?.saveSetting(SettingLogic.SETTING_CAMERA_RESOLUTION, QualitySelector.QUALITY_SD.toString())
        }else if(item?.id == R.id.PREVIEW_SIZE_SMALL){
            settingProvider?.saveSetting(SettingLogic.SETTING_PREVIEW_SIZE, "SMALL")
        }else if(item?.id == R.id.PREVIEW_SIZE_MEDIUM){
            settingProvider?.saveSetting(SettingLogic.SETTING_PREVIEW_SIZE, "MEDIUM")
        }else if(item?.id == R.id.PREVIEW_SIZE_LARGE){
            settingProvider?.saveSetting(SettingLogic.SETTING_PREVIEW_SIZE, "LARGE")
        }
        reloadData()
    }

    private fun reloadData() {
        val settingValue = settingProvider?.loadSetting(SettingLogic.SETTING_CAMERA)?.settingValue
        if(settingValue == "CAMERA_FRONT") {
            binding.txtSettingVideoCamera.text = "Front camera"
        }else{
            binding.txtSettingVideoCamera.text = "Back camera"
        }
        val quality = settingProvider?.loadSetting(SettingLogic.SETTING_CAMERA_RESOLUTION)?.settingValue
        val inforStr = StringBuilder()
        when(quality){
            "8" -> inforStr.append("UHD")
            "6" -> inforStr.append("FHD")
            "5" -> inforStr.append("HD")
            "4" -> inforStr.append("SD")
            "" -> inforStr.append("UHD")
        }
        binding.txtSettingVideoQuality.text = inforStr.toString().trim()
        val previewSize = settingProvider?.loadSetting(SettingLogic.SETTING_PREVIEW_SIZE)?.settingValue
        val sizeStr = StringBuilder()
        when(previewSize){
            "SMALL" -> sizeStr.append("SMALL")
            "MEDIUM" -> sizeStr.append("MEDIUM")
            "LARGE" -> sizeStr.append("LARGE")
            "" -> sizeStr.append("MEDIUM")
        }
        binding.txtSettingPreviewSize.text = sizeStr.toString().trim()
        val pathSave = settingProvider?.loadSetting(SettingLogic.SETTING_SAVE_PATH)?.settingValue
        if(pathSave.equals("")){
            binding.txtSettingVideoPath.text = "DCIM/SerectVideo"
        }else{
            binding.txtSettingVideoPath.text = pathSave
        }

    }

    private fun openCamera() {
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

    private fun openPreviewSize() {
        modalBottomSheetDialog = ModalBottomSheetDialog.Builder()
            .setHeader("Quality Selector")
            .add(R.menu.setting_preview_size)
            .setRoundedModal(true)
            .setHeaderLayout(R.layout.setting_dialog_header)
            .setItemLayout(R.layout.setting_dialog_item)
            .build()
        if(!modalBottomSheetDialog.isAdded){
            modalBottomSheetDialog.show(supportFragmentManager, "Setting")
        }else{
        }
    }

    private fun openDirectoryPicker(){
        if (allPermissionsGranted()) {
            chooser?.show();
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }
    }
    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(
                    this@SettingActivity,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                openDirectoryPicker()
            } else {
                Toast.makeText(
                    this@SettingActivity,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}