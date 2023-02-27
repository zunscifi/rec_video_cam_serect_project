package ziuzangdev.repo.app_setting.Control.RecSetting

import android.content.Context
import ziuzangdev.repo.app_setting.Model.RecSetting.SettingModel

class SettingProvider(private val context: Context) {

    fun saveSetting (settingName: String ,settingValue: String) {
        val setting = SettingModel(settingName, settingValue)
        SettingLogic.saveSetting(context, setting)
    }

    fun loadSetting(settingName: String): SettingModel {
        return SettingLogic.loadSetting(context, settingName)
    }

}
