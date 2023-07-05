package com.eurigo.websocketutils

import android.annotation.SuppressLint
import android.content.*
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.eurigo.websocketlib.DisConnectReason
import com.eurigo.websocketlib.WsClient
import com.eurigo.websocketutils.okhttp.WebSocketHandler
import com.eurigo.websocketutils.service.JWebSocketClient
import com.eurigo.websocketutils.service.JWebSocketClientService
import com.eurigo.websocketutils.utils.EVENT_NET_WORK
import com.eurigo.websocketutils.utils.LiveDataBus
import org.java_websocket.framing.Framedata

class Main1Activity : AppCompatActivity(), View.OnClickListener {
    private var mAdapter: LogAdapter? = null
    private var etAddress: EditText? = null
    private var etMsg: EditText? = null
    private var btnClose: Button? = null
    private var btnConnect: Button? = null
    private var btnSend: Button? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        //        startLocalServer();
        PreferenceManager.getDefaultSharedPreferences(this)
            .edit()
            .putInt("index", 2)
            .apply()


        LiveDataBus.with<Any>(EVENT_NET_WORK).observe(this) { aBoolean: Any ->
            if (aBoolean as Boolean) {
                if (!isServiceConnection) {
                    //绑定服务
                    bindService()
                    doRegisterReceiver()
                }
            }
        }
    }

    private fun initView() {
        val mRecyclerView = findViewById<RecyclerView>(R.id.rcv_ap_log)
        mAdapter = LogAdapter(ArrayList())
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter = mAdapter
        btnConnect = findViewById(R.id.btn_connect)
        btnClose = findViewById(R.id.btn_close)
        btnSend = findViewById(R.id.btn_send)
        btnConnect?.setOnClickListener(this)
        btnClose?.setOnClickListener(this)
        btnSend?.setOnClickListener(this)
        etAddress = findViewById(R.id.et_address)
        etMsg = findViewById(R.id.et_message)
        etMsg?.setText("{\"type\":\"1\"}")

        btnConnect?.isVisible = false
        btnClose?.isVisible = false
    }


    @SuppressLint("NonConstantResourceId")
    override fun onClick(view: View) {
        when (view.id) {
//            R.id.btn_connect -> connectWebSocket(getEditText(etAddress))
            R.id.btn_close -> {

            }
            R.id.btn_send -> {
                val msg = getEditText(etMsg)
                if (TextUtils.isEmpty(msg)) {
                    ToastUtils.showShort("请输入消息")
                    return
                }

                jWebSClientService?.sendText()
            }
            else -> {}
        }
    }

    private fun getEditText(editText: EditText?): String {
        return editText!!.text.toString().trim { it <= ' ' }
    }

    /**
     * 客户端回调start
     */
    fun onConnected(client: WsClient) {
        runOnUiThread {
//            etAddress.setText(REGEX.concat(client.getRemoteSocketAddress().toString()));
            mAdapter!!.addDataAndScroll("连接成功", true)
            btnConnect!!.isEnabled = false
            btnClose!!.isEnabled = true
        }
    }

    fun onDisconnect(client: WsClient, reason: DisConnectReason) {
        runOnUiThread {
            mAdapter!!.addDataAndScroll("连接断开", true)
            btnConnect!!.isEnabled = true
            btnClose!!.isEnabled = false
        }
    }

    fun onError(webSocketClient: WsClient, ex: Exception) {
        LogUtils.e("客户端日志", "连接失败", ex.message)
    }

    fun onMessage(webSocketClient: WsClient, message: String) {
        runOnUiThread { mAdapter!!.addDataAndScroll(message, true) }
    }

    fun onPing(webSocketClient: WsClient, frameData: Framedata) {
        LogUtils.e("客户端日志", "收到Ping")
    }

    fun onPong(webSocketClient: WsClient, frameData: Framedata) {
        LogUtils.e("客户端日志", "收到Pong")
    }

    fun onSendMessage(client: WsClient, message: String) {
        LogUtils.e("客户端日志", "发送消息", message)
    }


   var isServiceConnection = false
    private var client: JWebSocketClient? = null
    private var binder: JWebSocketClientService.JWebSocketClientBinder? = null
    private var jWebSClientService: JWebSocketClientService? = null
    private var chatMessageReceiver: ChatMessageReceiver? = null
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName?, iBinder: IBinder?) {
            binder = iBinder as JWebSocketClientService.JWebSocketClientBinder?
            jWebSClientService = binder?.service
            client = jWebSClientService?.client
            Log.e("TAG", "onServiceConnected")
            isServiceConnection = true
        }

        override fun onServiceDisconnected(componentName: ComponentName?) {
            Log.e("TAG", "服务与活动成功断开")
            isServiceConnection = false
        }
    }

    private inner class ChatMessageReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val message = intent.getStringExtra("message")
            Log.e("websocket", "  收到消息：$message")
            runOnUiThread { mAdapter?.addDataAndScroll(message!!, true) }
        }
    }

    /**
     * 绑定服务
     */
    private fun bindService() {
        try {
            val bindIntent = Intent(
                this@Main1Activity,
                JWebSocketClientService::class.java
            )
            bindService(bindIntent, serviceConnection, BIND_AUTO_CREATE)
        } catch (e: Exception) {
            Log.e("websocket", "绑定服务异常")
        }
    }

    /**
     * 动态注册广播
     */
    private fun doRegisterReceiver() {
        chatMessageReceiver = ChatMessageReceiver()
        val filter = IntentFilter("com.dhy.health.healthhut")
        registerReceiver(chatMessageReceiver, filter)
    }

    override fun onResume() {
        super.onResume()
        if (!isServiceConnection) {
            //绑定服务
            bindService()
            doRegisterReceiver()
        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        unregisterReceiver(chatMessageReceiver)
    }

}