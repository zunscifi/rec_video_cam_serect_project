package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.ravikoradiya.liveadapter.LiveAdapter
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.recordcambackgroundproject.Model.VideoData
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityAppBackgroundPickerBinding
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityVideoManagerBinding
import java.text.SimpleDateFormat
import java.util.Locale

class AppBackgroundPickerActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityAppBackgroundPickerBinding
    private val listBackground = listOf(
        R.drawable.bg_app_01,
        R.drawable.bg_app_02,
        R.drawable.bg_app_03,
        R.drawable.bg_app_04,
        R.drawable.bg_app_05,
        R.drawable.bg_app_06)
    private var settingProvider : SettingProvider? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityAppBackgroundPickerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        addControls()
        addEvents()
    }
    private fun initSettingProvider() {
        settingProvider = SettingProvider(this@AppBackgroundPickerActivity)
    }
    private fun addEvents() {
        viewBinding.imgbtnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }
    }

    private fun addControls() {
        initSettingProvider()
        initLiveAdapter()
    }

    private fun initLiveAdapter() {
        viewBinding.rcvViewAppBackgroundPicker.layoutManager = GridLayoutManager(this, 3)
        LiveAdapter(listBackground)
            .map<Int>(R.layout.item_rcv_app_background_picker) {
                onBind { v ->
                    val resourceIDSetting = settingProvider?.loadSetting(SettingLogic.SETTING_BACKGROUND_IMAGE)?.settingValue
                    val resourceID = v.binding.data
                    val imgCheck = v.binding.getViewById<ImageButton>(R.id.imgbtn_check)
                    if(resourceIDSetting == resourceID.toString()){
                        imgCheck?.visibility = ImageView.VISIBLE
                    }
                    val imgBackground = v.binding.getViewById<ImageView>(R.id.img_background)
                    if(imgBackground != null){
                        Glide.with(this@AppBackgroundPickerActivity).load(resourceID).into(imgBackground)
                        imgBackground.setOnClickListener (){
                            settingProvider?.saveSetting(SettingLogic.SETTING_BACKGROUND_IMAGE, resourceID.toString())
                            initLiveAdapter()
                        }
                    }
                }
            }
            .into(viewBinding.rcvViewAppBackgroundPicker)
    }
}