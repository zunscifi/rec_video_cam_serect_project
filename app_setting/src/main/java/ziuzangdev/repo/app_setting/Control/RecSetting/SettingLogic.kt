package ziuzangdev.repo.app_setting.Control.RecSetting

import android.content.Context
import ziuzangdev.repo.app_setting.Model.RecSetting.SettingModel
import ziuzangdev.repo.app_setting.R

object SettingLogic {
    const val SETTING_IS_SHOW_PREVIEW = "SETTING_NAME_1"
    const val SETTING_CAMERA_USING = "SETTING_NAME_2"

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
