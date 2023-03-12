package ziuzangdev.repo.app_setting.Control.RecSetting

import android.content.Context
import ziuzangdev.repo.app_setting.Model.RecSetting.SettingModel
import ziuzangdev.repo.app_setting.R

object SettingLogic {
    const val SETTING_IS_SHOW_PREVIEW = "SETTING_NAME_1"
    const val SETTING_CAMERA_USING = "SETTING_NAME_2"
    const val SETTING_CAMERA_RESOLUTION = "SETTING_NAME_3"
    const val SETTING_CAMERA = "SETTING_NAME_4"
    const val SETTING_PREVIEW_SIZE = "SETTING_NAME_5"
    const val SETTING_SAVE_PATH = "SETTING_NAME_6"
    const val SETTING_BACKGROUND_IMAGE = "SETTING_NAME_7"
    const val TIME_OPEN_APP = "SETTING_NAME_8"
    fun saveSetting(context: Context, setting: SettingModel) {
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.appsettings_name_sharedpref), Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(setting.settingName, setting.settingValue)
            apply()
        }
    }

    fun loadSetting(context: Context, settingName: String): SettingModel {
        val defaultValue = false
        val sharedPreferences =
            context.getSharedPreferences(context.getString(R.string.appsettings_name_sharedpref), Context.MODE_PRIVATE)
        val settingValue = sharedPreferences.getString(settingName, "")
        return SettingModel(settingName, settingValue!!)
    }
}
