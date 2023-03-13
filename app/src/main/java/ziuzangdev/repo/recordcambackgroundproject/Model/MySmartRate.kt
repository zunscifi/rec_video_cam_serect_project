package ziuzangdev.repo.recordcambackgroundproject.Model

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingLogic
import ziuzangdev.repo.app_setting.Control.RecSetting.SettingProvider
import ziuzangdev.repo.recordcambackgroundproject.R

object MySmartRate {
    interface CallBack_UserRating {
        fun userRating(rating: Int)
    }
    private const val DONT_ASK_AGAIN_VALUE: Long = -1
    private const val SP_LIBRARY_NAME = "SP_RATE_LIBRARY"
    private const val SP_KEY_LAST_ASK_TIME = "SP_KEY_LAST_ASK_TIME"
    private const val SP_KEY_INIT_TIME = "SP_KEY_INIT_TIME"
    private var selectedStar = 1
    private const val DEFAULT_TIME_BETWEEN_DIALOG_MS = 1000L * 60 * 60 * 24 * 6 // 3 days
    private const val DEFAULT_DELAY_TO_ACTIVATE_MS = 1000L * 60 * 60 * 24 * 3 // 3 days
    private const val DEFAULT_TEXT_TITLE = "Rate Us"
    private const val DEFAULT_TEXT_CONTENT = "Tell others what you think about this app"
    private const val DEFAULT_TEXT_CONTINUE = "Continue"
    private const val DEFAULT_TEXT_GOOGLE_PLAY = "Please take a moment and rate us on Google Play"
    private const val DEFAULT_TEXT_CLICK_HERE = "click here"
    private const val DEFAULT_TEXT_LATER = "Ask me later"
    private const val DEFAULT_TEXT_STOP = "Never ask again"
    private const val DEFAULT_TEXT_CANCEL = "Cancel"
    private const val DEFAULT_TEXT_THANKS = "Thanks for the feedback"
    public const val STOP_RATE_REQUEST_SIGNAL = "STOP"
    private var continueClicked = false
    fun Rate(
        activity: Activity?,
        mainColor: Int,
        openStoreFromXStars: Int,
        callBack_userRating: CallBack_UserRating?
    ) {
        MySmartRate.Rate(
            activity,
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            mainColor,
            openStoreFromXStars,
            callBack_userRating
        )
    }

    fun Rate(
        activity: Activity,
        _title: String?,
        _content: String?,
        _continue_text: String?,
        _googlePlay_text: String?,
        _clickHere_text: String?,
        _cancel_text: String?,
        _thanksForFeedback: String?,
        mainColor: Int,
        openStoreFromXStars: Int
    ) {
        Rate(
            activity,
            _title,
            _content,
            _continue_text,
            _googlePlay_text,
            _clickHere_text,
            "",
            "",
            _cancel_text,
            _thanksForFeedback,
            mainColor,
            openStoreFromXStars,
            -1,
            -1
        )
    }

    fun Rate(
        activity: Activity?,
        _title: String?,
        _content: String?,
        _continue_text: String?,
        _googlePlay_text: String?,
        _clickHere_text: String?,
        _cancel_text: String?,
        _thanksForFeedback: String?,
        mainColor: Int,
        openStoreFromXStars: Int,
        callBack_userRating: MySmartRate.CallBack_UserRating?
    ) {
        if (activity != null) {
            MySmartRate.Rate(
                activity,
                _title,
                _content,
                _continue_text,
                _googlePlay_text,
                _clickHere_text,
                "",
                "",
                _cancel_text,
                _thanksForFeedback,
                mainColor,
                openStoreFromXStars,
                -1,
                -1,
                callBack_userRating
            )
        }
    }

    @SuppressLint("MissingInflatedId", "SetTextI18n")
    @JvmOverloads
    fun Rate(
        activity: Activity,
        _title: String?,
        _content: String?,
        _continue_text: String?,
        _googlePlay_text: String?,
        _clickHere_text: String?,
        _later_text: String?,
        _stop_text: String?,
        _cancel_text: String?,
        _thanksForFeedback: String?,
        mainColor: Int,
        openStoreFromXStars: Int,
        _hoursBetweenCalls: Int,
        _hoursDelayToActivate: Int,
        callBack_userRating: MySmartRate.CallBack_UserRating? = null
    ) {
        val title = if (_title != null && _title != "") _title else MySmartRate.DEFAULT_TEXT_TITLE
        val content =
            if (_content != null && _content != "") _content else MySmartRate.DEFAULT_TEXT_CONTENT
        val continue_text =
            if (_continue_text != null && _continue_text != "") _continue_text else MySmartRate.DEFAULT_TEXT_CONTINUE
        val googlePlay_text =
            if (_googlePlay_text != null && _googlePlay_text != "") _googlePlay_text else MySmartRate.DEFAULT_TEXT_GOOGLE_PLAY
        val clickHere_text =
            if (_clickHere_text != null && _clickHere_text != "") _clickHere_text else MySmartRate.DEFAULT_TEXT_CLICK_HERE
        val later_text =
            if (_later_text != null && _later_text != "") _later_text else MySmartRate.DEFAULT_TEXT_LATER
        val stop_text =
            if (_stop_text != null && _stop_text != "") _stop_text else MySmartRate.DEFAULT_TEXT_STOP
        val cancel_text =
            if (_cancel_text != null && _cancel_text != "") _cancel_text else MySmartRate.DEFAULT_TEXT_CANCEL
        val thanksForFeedback =
            if (_thanksForFeedback != null && _thanksForFeedback != "") _thanksForFeedback else MySmartRate.DEFAULT_TEXT_THANKS
        val timeBetweenCalls_Ms =
            if (_hoursBetweenCalls >= 0 && _hoursBetweenCalls < 366 * 24) 1000L * 60 * 60 * _hoursBetweenCalls else MySmartRate.DEFAULT_TIME_BETWEEN_DIALOG_MS
        val timeDelayToActivate_Ms =
            if (_hoursDelayToActivate >= 0 && _hoursDelayToActivate < 366 * 24) 1000L * 60 * 60 * _hoursDelayToActivate else MySmartRate.DEFAULT_DELAY_TO_ACTIVATE_MS
        //        final long timeBetweenCalls_Ms = 0;
//        final long timeDelayToActivate_Ms = 0;
        MySmartRate.continueClicked = false
        var hideNeverAskAgain = false
        if (_hoursBetweenCalls != -1 && _hoursDelayToActivate != -1) {
            // no force asking mode
            var initTime = MySmartRate.getInitTime(activity)
            if (initTime == 0L) {
                initTime = System.currentTimeMillis()
                MySmartRate.setInitTime(activity, initTime)
            }
            if (System.currentTimeMillis() < initTime + timeDelayToActivate_Ms) {
                return
            }
            if (MySmartRate.getLastAskTime(activity) == 0L) {
                // first time asked
                hideNeverAskAgain = true
            }
            if (MySmartRate.getLastAskTime(activity) == MySmartRate.DONT_ASK_AGAIN_VALUE) {
                // user already rate or click on never ask button
                return
            }
            if (System.currentTimeMillis() < MySmartRate.getLastAskTime(activity) + timeBetweenCalls_Ms) {
                // There was not enough time between the calls.
                return
            }
        }
        MySmartRate.setLastAskTime(activity, System.currentTimeMillis())
        val dialogBuilder: AlertDialog.Builder = AlertDialog.Builder(activity)
        val inflater = activity.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_rate, null)
        dialogBuilder.setView(dialogView)
        val alertDialog: AlertDialog = dialogBuilder.create()
        alertDialog.setCancelable(false)
        alertDialog.setCanceledOnTouchOutside(true)
        alertDialog.setOnCancelListener {
            activity.finish()
        }
        alertDialog.setOnDismissListener {
            activity.finish()
        }
        val alert_LAY_back = dialogView.findViewById<RelativeLayout>(R.id.alert_LAY_back)
        val alert_BTN_ok: AppCompatButton = dialogView.findViewById(R.id.alert_BTN_ok)
        val alert_BTN_later = dialogView.findViewById<Button>(R.id.alert_BTN_later)
        val alert_BTN_stop = dialogView.findViewById<Button>(R.id.alert_BTN_stop)
        val alert_LBL_title = dialogView.findViewById<TextView>(R.id.alert_LBL_title)
        val alert_LBL_content = dialogView.findViewById<TextView>(R.id.alert_LBL_content)
        val alert_LBL_google = dialogView.findViewById<TextView>(R.id.alert_LBL_google)
        val alert_LAY_stars = dialogView.findViewById<View>(R.id.alert_LAY_stars)
        val alert_IMG_google = dialogView.findViewById<ImageView>(R.id.alert_IMG_google)
        val alert_BTN_star_1 = dialogView.findViewById<ImageButton>(R.id.alert_BTN_star_1)
        val alert_BTN_star_2 = dialogView.findViewById<ImageButton>(R.id.alert_BTN_star_2)
        val alert_BTN_star_3 = dialogView.findViewById<ImageButton>(R.id.alert_BTN_star_3)
        val alert_BTN_star_4 = dialogView.findViewById<ImageButton>(R.id.alert_BTN_star_4)
        val alert_BTN_star_5 = dialogView.findViewById<ImageButton>(R.id.alert_BTN_star_5)
        val stars = arrayOf(
            alert_BTN_star_1,
            alert_BTN_star_2,
            alert_BTN_star_3,
            alert_BTN_star_4,
            alert_BTN_star_5
        )
        alert_LBL_google.visibility = View.GONE
        alert_IMG_google.visibility = View.GONE
        alert_LAY_back.setBackgroundColor(mainColor)
        alert_BTN_ok.getBackground().setColorFilter(mainColor, PorterDuff.Mode.MULTIPLY)
        alert_LBL_title.setTextColor(mainColor)
        alert_LBL_content.setTextColor(mainColor)
        alert_BTN_later.setTextColor(
            Color.parseColor(
                MySmartRate.shadeColor(
                    String.format(
                        "#%06X",
                        0xFFFFFF and mainColor
                    ), -33
                )
            )
        )
        alert_BTN_stop.setTextColor(
            Color.parseColor(
                MySmartRate.shadeColor(
                    String.format(
                        "#%06X",
                        0xFFFFFF and mainColor
                    ), -33
                )
            )
        )
        val drawable_active: Int = R.drawable.ic_star_active
        val drawable_deactive: Int = R.drawable.ic_star_deactive
        val starsClickListener =
            View.OnClickListener { v ->
                var clickedIndex = -1
                for (i in stars.indices) {
                    if (stars[i].id == v.id) {
                        clickedIndex = i
                        break
                    }
                }
                if (clickedIndex != -1) {
                    for (i in 0..clickedIndex) {
                        stars[i].setImageResource(drawable_active)
                    }
                    for (i in clickedIndex + 1 until stars.size) {
                        stars[i].setImageResource(drawable_deactive)
                    }
                }
                alert_BTN_ok.setEnabled(true)
                alert_BTN_ok.setText(
                    """
                         ${clickedIndex + 1}/5
                         $continue_text
                         """.trimIndent()
                )
                selectedStar = clickedIndex + 1
                var _openStoreFrom_Stars = openStoreFromXStars
                if (openStoreFromXStars < 1 || openStoreFromXStars > 5) {
                    _openStoreFrom_Stars = 4
                }
                if (selectedStar >= _openStoreFrom_Stars) {
                    val settingProvider = SettingProvider(activity)
                    settingProvider.saveSetting(SettingLogic.TIME_OPEN_APP, STOP_RATE_REQUEST_SIGNAL)
                    launchMarket(activity)
                } else {
                    Toast.makeText(activity, thanksForFeedback, Toast.LENGTH_SHORT).show()
                }
                alertDialog.dismiss()
            }
        alert_BTN_star_1.setOnClickListener(starsClickListener)
        alert_BTN_star_2.setOnClickListener(starsClickListener)
        alert_BTN_star_3.setOnClickListener(starsClickListener)
        alert_BTN_star_4.setOnClickListener(starsClickListener)
        alert_BTN_star_5.setOnClickListener(starsClickListener)
        alert_LBL_title.text = title
        alert_LBL_content.text = content
        if (continue_text != null && continue_text != "") {
            alert_BTN_ok.setText(continue_text)
            alert_BTN_ok.setText("?/5\n$continue_text")
            alert_BTN_ok.setOnClickListener(View.OnClickListener {
                var _openStoreFrom_Stars = openStoreFromXStars
                if (openStoreFromXStars < 1 || openStoreFromXStars > 5) {
                    _openStoreFrom_Stars = 4
                }
                if (MySmartRate.continueClicked) {
                    MySmartRate.setLastAskTime(activity, MySmartRate.DONT_ASK_AGAIN_VALUE)
//                    if (MySmartRate.selectedStar >= _openStoreFrom_Stars) {
//                        MySmartRate.launchMarket(activity)
//                    } else {
//                        Toast.makeText(activity, thanksForFeedback, Toast.LENGTH_SHORT).show()
//                    }
                    alertDialog.dismiss()
                } else {
                    if (openStoreFromXStars != -1 && MySmartRate.selectedStar >= _openStoreFrom_Stars) {
                        MySmartRate.continueClicked = true
                        alert_LBL_title.visibility = View.GONE
                        alert_LBL_content.visibility = View.GONE
                        alert_LAY_stars.visibility = View.GONE
                        alert_BTN_stop.visibility = View.GONE
                        alert_LBL_google.visibility = View.VISIBLE
                        alert_IMG_google.visibility = View.VISIBLE
                        alert_LBL_google.text = googlePlay_text
                        alert_BTN_ok.setText(clickHere_text)
                        alert_BTN_later.text = cancel_text

                        //String text = googlePlay_text + "\n(" + clickHere_text + ")";
                        //Spannable span = new SpannableString(text);
                        //span.setSpan(new RelativeSizeSpan(0.7f), 0, googlePlay_text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        //span.setSpan(new RelativeSizeSpan(0.4f), googlePlay_text.length(), text.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        //alert_BTN_ok.setText(span);
                    } else {
                        alertDialog.dismiss()
                        Toast.makeText(activity, thanksForFeedback, Toast.LENGTH_SHORT).show()
                    }
                }
                callBack_userRating?.userRating(MySmartRate.selectedStar)
            })
        } else {
            alert_BTN_ok.setVisibility(View.INVISIBLE)
        }
        alert_BTN_ok.setEnabled(false)
        if (hideNeverAskAgain) {
            alert_BTN_stop.visibility = View.GONE
        }
        if (later_text != null && later_text != "") {
            alert_BTN_later.text = later_text
            alert_BTN_later.setOnClickListener {
                alertDialog.dismiss()
                activity.finish()
            }
        } else {
            alert_BTN_later.visibility = View.INVISIBLE
        }
        if (stop_text != null && stop_text != "") {
            alert_BTN_stop.text = stop_text
            alert_BTN_stop.setOnClickListener {
                MySmartRate.setLastAskTime(
                    activity,
                    MySmartRate.DONT_ASK_AGAIN_VALUE
                )
                alertDialog.dismiss()
            }
        } else {
            alert_BTN_stop.visibility = View.GONE
        }
        if (_hoursBetweenCalls == -1 && _hoursDelayToActivate == -1) {
            // force asking mode
            alert_BTN_later.text = cancel_text
            alert_BTN_stop.visibility = View.GONE
        }
        alertDialog.show()
    }

    private fun launchMarket(activity: Activity) {
        val uri = Uri.parse("market://details?id=" + activity.packageName)
        val myAppLinkToMarket = Intent(Intent.ACTION_VIEW, uri)
        try {
            activity.startActivity(myAppLinkToMarket)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, " unable to find google play app", Toast.LENGTH_LONG).show()
        }
    }

    private fun getLastAskTime(activity: Activity): Long {
        val sharedPreferences = activity.getSharedPreferences(
            MySmartRate.SP_LIBRARY_NAME,
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getLong(MySmartRate.SP_KEY_LAST_ASK_TIME, 0)
    }

    private fun setLastAskTime(activity: Activity, time: Long) {
        val editor =
            activity.getSharedPreferences(MySmartRate.SP_LIBRARY_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(MySmartRate.SP_KEY_LAST_ASK_TIME, time)
        editor.apply()
    }

    private fun getInitTime(activity: Activity): Long {
        val sharedPreferences = activity.getSharedPreferences(
            MySmartRate.SP_LIBRARY_NAME,
            Context.MODE_PRIVATE
        )
        return sharedPreferences.getLong(MySmartRate.SP_KEY_INIT_TIME, 0)
    }

    private fun setInitTime(activity: Activity, time: Long) {
        val editor =
            activity.getSharedPreferences(MySmartRate.SP_LIBRARY_NAME, Context.MODE_PRIVATE).edit()
        editor.putLong(MySmartRate.SP_KEY_INIT_TIME, time)
        editor.apply()
    }

    private fun shadeColor(color: String, percent: Int): String {
        var R = color.substring(1, 3).toInt(16)
        var G = color.substring(3, 5).toInt(16)
        var B = color.substring(5, 7).toInt(16)
        R = R * (100 + percent) / 100
        G = G * (100 + percent) / 100
        B = B * (100 + percent) / 100
        R = if (R < 255) R else 255
        G = if (G < 255) G else 255
        B = if (B < 255) B else 255
        val RR = if (Integer.toString(R, 16).length == 1) "0" + Integer.toString(
            R,
            16
        ) else Integer.toString(R, 16)
        val GG = if (Integer.toString(G, 16).length == 1) "0" + Integer.toString(
            G,
            16
        ) else Integer.toString(G, 16)
        val BB = if (Integer.toString(B, 16).length == 1) "0" + Integer.toString(
            B,
            16
        ) else Integer.toString(B, 16)
        return "#$RR$GG$BB"
    }

}