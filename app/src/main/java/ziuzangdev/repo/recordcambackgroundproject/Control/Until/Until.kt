package ziuzangdev.repo.recordcambackgroundproject.Control.Until

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import java.io.File


object Until {
    fun changeViewSizeInDp(view: View, widthDp: Int, heightDp: Int) : View {
        val density = view.context.resources.displayMetrics.density
        val widthPx = (widthDp * density).toInt()
        val heightPx = (heightDp * density).toInt()

        val layoutParams = view.layoutParams
        layoutParams.width = widthPx
        layoutParams.height = heightPx
        view.layoutParams = layoutParams

        return view;
    }

    fun getAllFilesVideo(context: Context): List<File> {
        val settingProvider = SettingProvider(context)
        val pathSave = settingProvider.loadSetting(SettingLogic.SETTING_SAVE_PATH).settingValue
        var path = ""
        if(pathSave == ""){
            path = "DCIM/SerectVideo"
        }else{
            path = pathSave.replace("/storage/emulated/0/", "")
        }
        val files = mutableListOf<File>()

        val projection = arrayOf(
            MediaStore.Images.Media.DATA
        )

        val uri: Uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} like ?"
        val selectionArgs = arrayOf("%${path}%")

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            null
        )

        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (it.moveToNext()) {
                val filePath = it.getString(columnIndex)
                files.add(File(filePath))
            }
        }

        return files
    }
    fun delete(context: Context, file: File): Boolean {
        file.setWritable(true)
        val where = MediaStore.Images.Media.DATA + "=?"
        val selectionArgs = arrayOf(
            file.absolutePath
        )
        val contentResolver = context.contentResolver
        val filesUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        contentResolver.delete(filesUri, where, selectionArgs)
        if (file.exists()) {
            contentResolver.delete(filesUri, where, selectionArgs)
        }
        return !file.exists()
    }
}