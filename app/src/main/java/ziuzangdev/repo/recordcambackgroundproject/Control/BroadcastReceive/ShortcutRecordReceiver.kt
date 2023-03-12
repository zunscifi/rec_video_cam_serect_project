package ziuzangdev.repo.recordcambackgroundproject.Control.BroadcastReceive

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.Control.Service.MRSProvider
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.View.Activity.MainActivity

class ShortcutRecordReceiver : BroadcastReceiver() {
    private var mrsProvider: MRSProvider? = null
    private var settingProvider : SettingProvider? = null
    override fun onReceive(context: Context?, intent: Intent?) {
        //initSettingProvider(context)
        //initMRSProvider(context)
        Toast.makeText(context, "HAHAHAHAH", Toast.LENGTH_SHORT).show()
    }

    private fun initSettingProvider(context: Context?) {
        if(context != null){
            settingProvider = SettingProvider(context)
        }
    }
    private fun initMRSProvider(context: Context?) {
        val inflater = LayoutInflater.from(context)
        val removeView = inflater.inflate(R.layout.item_preview_overlay, null) as RelativeLayout
        val removeImg = removeView.findViewById<View>(R.id.preview_container) as PreviewView
        val openApp = removeView.findViewById<View>(R.id.imgbtn_open_app) as ImageButton

        val tempView = inflater.inflate(R.layout.activity_main, null)  as RelativeLayout
        val txtDuration = tempView.findViewById<TextView>(R.id.txt_duration)
        val btnRecord = tempView.findViewById<ImageButton>(R.id.btn_record)
        val cbIsShowPreview = tempView.findViewById<CheckBox>(R.id.cb_is_show_preview)
        if(context != null){
            mrsProvider = MRSProvider(context, MainActivity::class.java,
                removeImg, txtDuration, btnRecord, removeView, openApp, cbIsShowPreview)
            if(mrsProvider?.requirePermission() == true){
                mrsProvider?.bindService()
            }else{
                //requestPermissions()
            }
        }

    }
}