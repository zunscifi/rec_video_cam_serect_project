package ziuzangdev.repo.rec_service.Control.Service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.annotation.WorkerThread
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.ActiveRecording
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture.withOutput
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.rec_service.R
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Timer
import java.util.TimerTask


class MediaRecordingService : LifecycleService() {
    private var settingProvider : SettingProvider = SettingProvider(this)
    companion object {
        const val CHANNEL_ID: String = "media_recorder_service"
        private val TAG = MediaRecordingService::class.simpleName
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        const val CHANNEL_NAME: String = "Media recording service"
        const val ONGOING_NOTIFICATION_ID: Int = 2345
        const val ACTION_START_WITH_PREVIEW: String = "start_recording"
        const val BIND_USECASE: String = "bind_usecase"
    }

    enum class RecordingState {
        RECORDING, PAUSED, STOPPED
    }

    class RecordingServiceBinder(private val service: MediaRecordingService) : Binder() {
        fun getService(): MediaRecordingService {
            return service
        }
    }
    private var preview: Preview? = null
    private var windowManager: WindowManager? = null
    private lateinit var timer: Timer
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var recordingServiceBinder: RecordingServiceBinder
    private var activeRecording: ActiveRecording? = null
    private var videoCapture: androidx.camera.video.VideoCapture<Recorder>? = null
    private val listeners = HashSet<DataListener>(1)
    private val pendingActions: HashMap<String, Runnable> = hashMapOf()
    private var recordingState: RecordingState = RecordingState.STOPPED
    private var duration: Int = 0
    private var timerTask: TimerTask? = null
    private var isSoundEnabled: Boolean = true
    private var handler: Handler? = null
    private var isServiceRunning = false
    private var notificationBuilder: NotificationCompat.Builder? = null
    private var notificationManager: NotificationManager? = null
    private var removeView: RelativeLayout? = null
    private var removeImg: PreviewView ? = null
    private lateinit var cameraSelector: CameraSelector
    private lateinit var activityClass: Class<*>
    private lateinit var openApp: ImageButton
    var LAYOUT_FLAG = 0
    fun unBindAll(){
        cameraProvider?.unbindAll()
    }
    override fun onCreate() {
        super.onCreate()
        recordingServiceBinder = RecordingServiceBinder(this)
        timer = Timer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when(intent?.action) {
            ACTION_START_WITH_PREVIEW -> {
                if (cameraProvider == null) {
                    Log.i(TAG, "Camera provider is not initialized yet. Adding action to pending actions.")
                    initializeCamera()
                }
            }
        }
        return START_NOT_STICKY
    }
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
    @SuppressLint("ClickableViewAccessibility")
    fun initializeBubblePreviewCam(
        removeViewTemp: RelativeLayout,
        removeImgTemp: PreviewView,
        activityClass: Class<*>,
        openApp: ImageButton
    ) {
        try{
            val isShowPreview = settingProvider.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW).settingValue.toBoolean()
            if(isShowPreview) {
                if (cameraSelector != null) {
                    this.activityClass = activityClass
                    removeView = removeViewTemp
                    removeImg = removeImgTemp
                    this.openApp = openApp
                    val previewSize =
                        settingProvider.loadSetting(SettingLogic.SETTING_PREVIEW_SIZE).settingValue
                    when (previewSize) {
                        "SMALL" -> removeImg =
                            changeViewSizeInDp(removeImg!!, 50, 100) as PreviewView

                        "MEDIUM" -> removeImg =
                            changeViewSizeInDp(removeImg!!, 150, 250) as PreviewView

                        "LARGE" -> removeImg =
                            changeViewSizeInDp(removeImg!!, 250, 350) as PreviewView

                        "" -> removeImg = changeViewSizeInDp(removeImg!!, 150, 250) as PreviewView
                    }
                    // Create a Preview use case and bind it to the PreviewView
                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.setSurfaceProvider(removeImg!!.surfaceProvider)
                        }
                    try {
                        cameraProvider?.bindToLifecycle(this, cameraSelector!!, preview)
                    } catch (e: Exception) {
                    }
                    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

                    val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                    } else {
                        LAYOUT_FLAG = WindowManager.LayoutParams.TYPE_PHONE
                    }

                    val paramRemove = WindowManager.LayoutParams(
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        WindowManager.LayoutParams.WRAP_CONTENT,
                        LAYOUT_FLAG,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                        PixelFormat.TRANSLUCENT
                    )
                    paramRemove.gravity = Gravity.TOP or Gravity.LEFT
                    windowManager?.addView(removeView, paramRemove); //Ad Remove vIew

                    // Set up touch listener to allow removeView to be dragged
                    var initialX: Int? = null
                    var initialY: Int? = null
                    var initialTouchX: Float? = null
                    var initialTouchY: Float? = null
                    removeImg!!.setOnTouchListener { v, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN -> {
                                // Save initial position and touch position
                                initialX = paramRemove.x
                                initialY = paramRemove.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                            }

                            MotionEvent.ACTION_MOVE -> {
                                // Update removeView position as user moves it
                                val dx = event.rawX - initialTouchX!!
                                val dy = event.rawY - initialTouchY!!
                                paramRemove.x = initialX!! + dx.toInt()
                                paramRemove.y = initialY!! + dy.toInt()
                                windowManager?.updateViewLayout(removeView, paramRemove)
                            }
                        }
                        true
                    }
                    this.openApp.setOnClickListener {
                        removeView?.visibility = View.GONE
                    }
                }
            }
        }catch (ignore : Exception){}
    }

    fun removeBubblePreviewCam(){
        val isShowPreview = settingProvider.loadSetting(SettingLogic.SETTING_IS_SHOW_PREVIEW).settingValue.toBoolean()
        if(isShowPreview){
            try{
                if(removeView != null){
                    windowManager?.removeView(removeView)
                }
            }catch (ignore : Exception){}
        }
    }

    fun setCameraProvider(){
        cameraProvider = null
    }
    private fun initializeCamera() {
        try{
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                // Used to bind the lifecycle of cameras to the lifecycle owner
                cameraProvider = cameraProviderFuture.get()
                val qualitySelector = getQualitySelector()
                val recorder = Recorder.Builder()
                    .setQualitySelector(qualitySelector)
                    .build()
                videoCapture = withOutput(recorder)
                // Select back camera as a default
                try{
                    val settingValue = settingProvider.loadSetting(SettingLogic.SETTING_CAMERA).settingValue
                    if(settingValue == "CAMERA_FRONT") {
                        cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                    }else{
                        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    }
                }catch (e : Exception){
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
                try {
                    // Unbind use cases before rebinding
                    cameraProvider?.unbindAll()
                    // Bind use cases to camera
                    if (cameraSelector != null) {
                        cameraProvider?.bindToLifecycle(this, cameraSelector, videoCapture)
                    }
                } catch(exc: Exception) {
                    Log.e(MediaRecordingService::class.simpleName, "Use case binding failed", exc)
                }
                val action = pendingActions[BIND_USECASE]
                action?.run()
                pendingActions.remove(BIND_USECASE)
            }, ContextCompat.getMainExecutor(this))
        }catch (ignore : Exception){}
    }

    private fun getQualitySelector(): QualitySelector {
        val quality = settingProvider.loadSetting(SettingLogic.SETTING_CAMERA_RESOLUTION).settingValue
        val qualitySelector : QualitySelector
        try{
            when(quality.toInt()){
                8-> return QualitySelector
                    .firstTry(QualitySelector.QUALITY_UHD)
                    .thenTry(QualitySelector.QUALITY_FHD)
                    .thenTry(QualitySelector.QUALITY_HD)
                    .finallyTry(
                        QualitySelector.QUALITY_SD,
                        QualitySelector.FALLBACK_STRATEGY_LOWER
                    )
                6 -> return QualitySelector
                    .firstTry(QualitySelector.QUALITY_FHD)
                    .thenTry(QualitySelector.QUALITY_HD)
                    .finallyTry(
                        QualitySelector.QUALITY_SD,
                        QualitySelector.FALLBACK_STRATEGY_LOWER
                    )
                5 -> return QualitySelector
                    .firstTry(QualitySelector.QUALITY_HD)
                    .finallyTry(
                        QualitySelector.QUALITY_SD,
                        QualitySelector.FALLBACK_STRATEGY_LOWER)
                4 -> return QualitySelector
                    .firstTry(QualitySelector.QUALITY_SD)
                    .finallyTry(
                        QualitySelector.QUALITY_SD,
                        QualitySelector.FALLBACK_STRATEGY_LOWER)
            }
        }catch (e: Exception){
            Log.e(TAG, "getQualitySelector: ", e)
        }
        return QualitySelector
            .firstTry(QualitySelector.QUALITY_UHD)
            .thenTry(QualitySelector.QUALITY_FHD)
            .thenTry(QualitySelector.QUALITY_HD)
            .finallyTry(
                QualitySelector.QUALITY_SD,
                QualitySelector.FALLBACK_STRATEGY_LOWER)
    }

    @SuppressLint("MissingPermission")
    fun startRecording(removeImg: PreviewView) {
        try{
            val mediaStoreOutputOptions = createMediaStoreOutputOptions()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            var pendingRecording = videoCapture?.output?.prepareRecording(this, mediaStoreOutputOptions)
            if (isSoundEnabled) {
                pendingRecording = pendingRecording?.withAudioEnabled()
            }
            activeRecording = pendingRecording?.withEventListener(ContextCompat.getMainExecutor(this)
            ) {
                when (it) {
                    is VideoRecordEvent.Start -> {
                        startTrackingTime()
                        recordingState = RecordingState.RECORDING
                    }

                    is VideoRecordEvent.Finalize -> {
                        recordingState = RecordingState.STOPPED
                        duration = 0
                        timerTask?.cancel()
                    }
                }
                for (listener in listeners) {
                    listener.onRecordingEvent(it)
                    listener.onRecordingState(getRecordingState())
                }
            }
                ?.start()
            recordingState = RecordingState.RECORDING
        }catch (ignore : Exception){}
    }

    private fun startTrackingTime() {
        try{
            timerTask = object: TimerTask() {
                override fun run() {
                    if (recordingState == RecordingState.RECORDING) {
                        duration += 1
                        for (listener in listeners) {
                            listener.onNewData(duration)
                        }
                    }
                }
            }
            timer.scheduleAtFixedRate(timerTask, 1000, 1000)
        }catch (ignore : Exception){}
    }

    fun stopRecording() {
        try{
            activeRecording?.stop()
            activeRecording = null
        }catch (ignore : Exception){}
    }

    private fun createMediaStoreOutputOptions(): MediaStoreOutputOptions {
        val name = "CameraX-recording-" +
                SimpleDateFormat(FILENAME_FORMAT, Locale.getDefault())
                    .format(System.currentTimeMillis()) + ".mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
            val pathSave = settingProvider.loadSetting(SettingLogic.SETTING_SAVE_PATH).settingValue
            if(pathSave == ""){
                put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/SerectVideo")
            }else{
                put(MediaStore.MediaColumns.RELATIVE_PATH, pathSave.replace("/storage/emulated/0/", ""))
            }
        }
        return MediaStoreOutputOptions.Builder(
            contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()
    }

    fun bindPreviewUseCase(surfaceProvider: Preview.SurfaceProvider?) {
        try{
            activeRecording?.pause()
            if (cameraProvider != null) {
                bindInternal(surfaceProvider)
            } else {
                pendingActions[BIND_USECASE] = Runnable {
                    bindInternal(surfaceProvider)
                }
            }
        }catch (ignore : Exception){}
    }

    private fun bindInternal(surfaceProvider: Preview.SurfaceProvider?) {
        try{
            if (preview != null) {
                cameraProvider?.unbind(preview)
            }
            initPreviewUseCase()
            preview?.setSurfaceProvider(surfaceProvider)
            var cameraSelector : CameraSelector? = null
            try{
                val settingValue = settingProvider.loadSetting(SettingLogic.SETTING_CAMERA).settingValue
                if(settingValue == "CAMERA_FRONT") {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                }else{
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                }
            }catch (e : Exception){
                cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            }
            try{
                val cameraInfo: CameraInfo? = cameraProvider?.bindToLifecycle(
                    this@MediaRecordingService,
                    cameraSelector!!,
                    preview
                )?.cameraInfo
                observeCameraState(cameraInfo, this)
            }catch (e : Exception){ }
        }catch (ignore : Exception){}
    }


    private fun initPreviewUseCase() {
        try{
            preview?.setSurfaceProvider(null)
            preview = Preview.Builder()
                .build()
        }catch (ignore : Exception){}
    }

    fun unbindPreview() {
        try{
            // Just remove the surface provider. I discovered that for some reason if you unbind the Preview usecase the camera willl stop recording the video.
            preview?.setSurfaceProvider(null)
        }catch (ignore : Exception){}
    }


    fun startRunningInForeground(activityClass: Class<*>) {
        val parentStack = TaskStackBuilder.create(this)
            .addNextIntentWithParentStack(Intent(this, activityClass))

        val pendingIntent1 = parentStack.getPendingIntent(0, 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }

        // Create the notification builder and store it in the property
        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Video Are Recording...")
            .setContentText("")
            .setSmallIcon(R.drawable.icon_rec_video_recording)
            .setContentIntent(pendingIntent1)

        // Build the notification and start foreground service
        val notification: Notification = notificationBuilder!!.build()
        startForeground(ONGOING_NOTIFICATION_ID, notification)

        // Create and send the broadcast to update the notification
    }
    fun updateNotificationDescription(description: String) {
        // Update the content text of the notification builder
        notificationBuilder?.setContentText(description)

        // Build the updated notification and notify the manager to update the notification
        val updatedNotification = notificationBuilder?.build()
        notificationManager?.notify(ONGOING_NOTIFICATION_ID, updatedNotification)
    }
    fun isSoundEnabled(): Boolean {
        return isSoundEnabled
    }

    fun setSoundEnabled(enabled: Boolean) {
        isSoundEnabled = enabled
    }

    // Stop recording and remove SurfaceView
    override fun onDestroy() {
        super.onDestroy()
        activeRecording?.stop()
        timerTask?.cancel()
        isServiceRunning = false
        cameraProvider?.unbindAll()
        cameraProvider = null
        stopSelf()
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return recordingServiceBinder
    }

    fun addListener(listener: DataListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: DataListener) {
        listeners.remove(listener)
    }

    fun getRecordingState(): RecordingState {
        return recordingState
    }

    private fun observeCameraState(cameraInfo: androidx.camera.core.CameraInfo?, context: Context) {
        try{
            cameraInfo?.cameraState?.observe(this) { cameraState ->
                run {
                    when (cameraState.type) {
                        CameraState.Type.PENDING_OPEN -> {
                            // Ask the user to close other camera apps
                        }
                        CameraState.Type.OPENING -> {
                            // Show the Camera UI
                            for (listener in listeners) {
                                listener.onCameraOpened()
                            }
                        }
                        CameraState.Type.OPEN -> {
                            // Setup Camera resources and begin processing
                        }
                        CameraState.Type.CLOSING -> {
                            // Close camera UI
                        }
                        CameraState.Type.CLOSED -> {
                            // Free camera resources
                        }
                    }
                }

                cameraState.error?.let { error ->
                    when (error.code) {
                        // Open errors
                        CameraState.ERROR_STREAM_CONFIG -> {
                            // Make sure to setup the use cases properly
//                        Toast.makeText(context,
//                            "Stream config error. Restart application",
//                            Toast.LENGTH_SHORT).show()
                        }
                        // Opening errors
                        CameraState.ERROR_CAMERA_IN_USE -> {
                            // Close the camera or ask user to close another camera app that's using the
                            // camera
                            Toast.makeText(context,
                                "Camera in use. Close any apps that are using the camera",
                                Toast.LENGTH_SHORT).show()
                        }
                        CameraState.ERROR_MAX_CAMERAS_IN_USE -> {
                            // Close another open camera in the app, or ask the user to close another
                            // camera app that's using the camera
                        }
                        CameraState.ERROR_OTHER_RECOVERABLE_ERROR -> {

                        }
                        // Closing errors
                        CameraState.ERROR_CAMERA_DISABLED -> {
                            // Ask the user to enable the device's cameras
//                        Toast.makeText(context,
//                            "Camera disabled",
//                            Toast.LENGTH_SHORT).show()
                        }
                        CameraState.ERROR_CAMERA_FATAL_ERROR -> {
                            // Ask the user to reboot the device to restore camera function
//                        Toast.makeText(context,
//                            "Fatal error",
//                            Toast.LENGTH_SHORT).show()
                        }
                        // Closed errors
                        CameraState.ERROR_DO_NOT_DISTURB_MODE_ENABLED -> {
                            // Ask the user to disable the "Do Not Disturb" mode, then reopen the camera
//                        Toast.makeText(context,
//                            "Do not disturb mode enabled",
//                            Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }catch (ignore : Exception){}
    }

    interface DataListener {
        fun onNewData(duration: Int)
        fun onCameraOpened()
        fun onRecordingEvent(it: VideoRecordEvent?)
        @WorkerThread
        fun onRecordingState(it: RecordingState?)
    }

    class NotificationUpdateReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            // Get the description string from the intent
            val description = intent?.getStringExtra("description")

            // Update the notification
            val notificationManager =
                context?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Video Are Recording...")
                .setContentText(description)
            val notification = builder.build()
            notificationManager.notify(ONGOING_NOTIFICATION_ID, notification)
        }
    }
}