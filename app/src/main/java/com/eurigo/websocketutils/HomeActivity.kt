package com.eurigo.websocketutils

import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.eurigo.websocketutils.receiver.NetworkConnectChangedReceiver
import com.eurigo.websocketutils.utils.EVENT_CURRENT_TIME
import com.eurigo.websocketutils.utils.EVENT_HEART
import com.eurigo.websocketutils.utils.LiveDataBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    private val secondSdf = SimpleDateFormat("ss", Locale.CHINA)
    private val netWorkReceiver = NetworkConnectChangedReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        val index = PreferenceManager.getDefaultSharedPreferences(this@HomeActivity)
            .getInt("index", -1)
        findViewById<TextView>(R.id.tv_text).apply {
            text = index.toString()
        }

        when (index) {
            0 -> {
                Intent(this, MainActivity::class.java)
                    .also {
                        startActivity(it)
                    }
            }
            1 -> {
                Intent(this, OkHttpActivity::class.java)
                    .also {
                        startActivity(it)
                    }
            }
            2 -> {
                Intent(this, Main1Activity::class.java)
                    .also {
                        startActivity(it)
                    }
            }
            else -> {}
        }


        val filter = IntentFilter()
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        filter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        registerReceiver(netWorkReceiver, filter)

        initTimeThread()
    }

    fun onClick1(view: View) {
        Intent(this, MainActivity::class.java)
            .also {
                startActivity(it)
            }
    }

    fun onClick2(view: View) {
        Intent(this, OkHttpActivity::class.java)
            .also {
                startActivity(it)
            }
    }

    fun onClick3(view: View) {
        Intent(this, Main1Activity::class.java)
            .also {
                startActivity(it)
            }
    }

    private var timeMap = mutableMapOf<Int, Boolean>()

    private val timer = (0..Int.MAX_VALUE)
        .asSequence()
        .asFlow()
        .onEach {
            delay(500)
        }
    private var job: Job? = null

    /**
     * 启动线程，每一秒更改一次时间
     */
    private fun initTimeThread() {

        job = lifecycleScope.launch(Dispatchers.IO) {
            timer.collect {
                runOnUiThread {
                    // 发送时间
                    LiveDataBus.with<Any>(EVENT_CURRENT_TIME).postStickyData("")

                    val currentSecondTime = getCurrentSecondTime()
                    if (timeMap.contains(currentSecondTime)) {
                        timeMap.clear()
                    } else {
                        timeMap[currentSecondTime] = false
                    }
                    if (currentSecondTime % 30 == 0 && timeMap[currentSecondTime] != null && timeMap[currentSecondTime] == false) {
                        timeMap[currentSecondTime] = true
                        LiveDataBus.with<Any>(EVENT_HEART).postStickyData("")
                    }
                }
            }
        }
    }


    private fun getCurrentSecondTime(): Int {
        val secondSdf = secondSdf.format(Date())
        return secondSdf.toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(netWorkReceiver)
        job?.isCancelled
    }
}