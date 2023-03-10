package ziuzangdev.repo.recordcambackgroundproject.View.Activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore.AUTHORITY
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.ravikoradiya.liveadapter.LiveAdapter
import ziuzangdev.repo.recordcambackgroundproject.Control.Until.Until
import ziuzangdev.repo.recordcambackgroundproject.Model.VideoData
import ziuzangdev.repo.recordcambackgroundproject.R
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityMainBinding
import ziuzangdev.repo.recordcambackgroundproject.databinding.ActivityVideoManagerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Hashtable
import java.util.Locale

class VideoManagerActivity : AppCompatActivity() {
    private lateinit var viewBinding: ActivityVideoManagerBinding
    private lateinit var videoList: ArrayList<VideoData>
    private lateinit var fileIsChooseToDelete: Hashtable<String, Boolean>
    private var isSelect: Boolean = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityVideoManagerBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        addControls()
        addEvents()
    }

    private fun addEvents() {
        viewBinding.imgbtnBack.setOnClickListener {
            finish()
            overridePendingTransition(0, 0)
        }

        viewBinding.imgbtnDelete.setOnClickListener {
            viewBinding.llDeleteProcess.visibility = View.VISIBLE
            var count = 0
            for (i in 0 until videoList.size) {
                if ( fileIsChooseToDelete[videoList[i].path] == true) {
                    count++
                }
            }
            var count2 = 0;
            viewBinding.txtDeleteProcess.text = "Deleting...0/$count"
            for (i in 0 until videoList.size) {
                if ( fileIsChooseToDelete[videoList[i].path] == true) {
                    try{
                       val delete = videoList[i].fileOf?.delete()
                        if(delete == true){
                            count2++
                            viewBinding.txtDeleteProcess.text = "Deleting...$count2/$count"
                        }
                    }catch (e: SecurityException){
                        Toast.makeText(this, "Delete fail: ${e.toString()}", Toast.LENGTH_SHORT).show()
                    }

                }
            }
            viewBinding.llDeleteProcess.visibility = View.GONE
            Toast.makeText(this, "Delete success!", Toast.LENGTH_SHORT).show()
            val intent = Intent(this@VideoManagerActivity, VideoManagerActivity::class.java)
            finish()
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        viewBinding.imgbtnMultipSelect.setOnClickListener {
            for(i in 0 until videoList.size){
                fileIsChooseToDelete[videoList[i].path] = !isSelect
            }
            isSelect = !isSelect
            initLiveAdapter()
        }
    }

    private fun addControls() {
        initData()
        initLiveAdapter()
    }

    private fun initLiveAdapter() {
        viewBinding.rcvViewVideoManager.layoutManager = LinearLayoutManager(this)
        LiveAdapter(videoList.toList())
            .map<VideoData>(R.layout.item_rcv_video_manager) {
                onBind { v ->
                    val path = v.binding.data?.path
                    val date = v.binding.data?.dateRecorded
                    val monthFormat = SimpleDateFormat("MMMM", Locale.US)
                    val month = monthFormat.format(date)
                    val dayOfWeekFormat = SimpleDateFormat("EEEE", Locale.US)
                    val dayOfWeek = dayOfWeekFormat.format(date)
                    val dateStr = "$dayOfWeek, $month ${date?.date} ${date?.year?.plus(1900)}"
                    val duration = v.binding.data?.duration
                    val seconds = duration?.div(1000)?.toInt() ?: 0
                    val minutes = seconds / 60
                    val remainingSeconds = seconds % 60
                    val formattedDuration = if (minutes == 0) {
                        String.format("%d:%02d", 0, remainingSeconds)
                    } else {
                        String.format("%d:%02d:%02d", minutes, remainingSeconds, 0)
                    }
                    val cbIsDelete = v.binding.getViewById<CheckBox>(R.id.cb_is_delete)
                    v.binding.getViewById<TextView>(R.id.txt_date)?.text = dateStr
                    v.binding.getViewById<TextView>(R.id.txt_video_name)?.text = v.binding.data?.title
                    v.binding.getViewById<TextView>(R.id.txt_video_infor)?.text = "Duration: ${formattedDuration} \nSize: ${v.binding.data?.size} MB \n$dateStr"
                    v.binding.getViewById<ImageView>(R.id.img_thumb)?.setImageBitmap(v.binding.data?.thumbnail)
                    println("boooool ${fileIsChooseToDelete[path]}")
                    if(fileIsChooseToDelete[path] == true){
                        if (cbIsDelete != null) {
                            cbIsDelete.isChecked = true
                        }
                    }
                    v.binding.getViewById<LinearLayout>(R.id.llItemVideo)?.setOnClickListener {
                        openFile(v.binding.data?.fileOf!!)
                    }
                    cbIsDelete?.setOnCheckedChangeListener { buttonView, isChecked ->
                            fileIsChooseToDelete[path] = isChecked
                            println("boooyyyool ${fileIsChooseToDelete[path]}")
                    }
                }
            }
            .into(viewBinding.rcvViewVideoManager)
    }
    fun openFile(selectedItem: File) {
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = FileProvider.getUriForFile(this@VideoManagerActivity, "ziuzangdev.repo.rec_service.fileprovider", selectedItem)
        intent.setDataAndType(uri, "video/mp4")
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        startActivity(intent)
    }
    private fun initData() {
        videoList = ArrayList()
        fileIsChooseToDelete = Hashtable()
        val videoListTemp = Until.getAllFilesVideo(this@VideoManagerActivity)
        for(video in videoListTemp){
            try{
                val videoData = VideoData(video)
                videoList.add(videoData)
                fileIsChooseToDelete[videoData.path] = false
            }catch (e : Exception){
                println("error: ${e.toString()}")
            }
        }
        videoList.reverse()
    }
}