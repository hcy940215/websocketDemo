package com.eurigo.websocketutils

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.LogUtils
import com.blankj.utilcode.util.ToastUtils
import com.eurigo.websocketlib.WsClient
import com.eurigo.websocketutils.okhttp.WebSocketCallBack
import com.eurigo.websocketutils.okhttp.WebSocketHandler
import org.java_websocket.framing.Framedata

class OkHttpActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var webSocketHandler: WebSocketHandler

    override fun onDestroy() {
        super.onDestroy()
        webSocketHandler.close()
    }


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
        var substring = DeviceIdUtil.getDeviceId(this)
        if ((substring.length > 10)) {
            substring = substring.substring(0, 10)
        }
        val url = "wss://ads.hfyuwo.com/webSocketServers/$substring"
        webSocketHandler = WebSocketHandler.getInstance(url)
        connectWebSocket()
        webSocketHandler.setSocketIOCallBack(object : WebSocketCallBack {
            override fun onOpen() {
                onConnected()
            }

            override fun onMessage(text: String) {
                onMessageText(text)
            }

            override fun onClose() {
                onDisconnect()
            }

            override fun onConnectError(t: Throwable) {
                onError(t)
            }
        })
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
    }

    @SuppressLint("NonConstantResourceId")
    override fun onClick(view: View) {
        when (view.id) {
            R.id.btn_connect ->{
                var substring = DeviceIdUtil.getDeviceId(this)
                if ((substring.length > 10)) {
                    substring = substring.substring(0, 10)
                }
                connectWebSocket()
            }
            R.id.btn_close -> {
                webSocketHandler.close()
                btnConnect!!.isEnabled = true
                btnClose!!.isEnabled = false
            }
            R.id.btn_send -> {
                val msg = getEditText(etMsg)
                if (TextUtils.isEmpty(msg)) {
                    ToastUtils.showShort("请输入消息")
                    return
                }
                webSocketHandler.send(getEditText(etMsg))
            }
            else -> {}
        }
    }

    private fun getEditText(editText: EditText?): String {
        return editText!!.text.toString().trim { it <= ' ' }
    }

    /**
     * 连接WebSocket
     */
    private fun connectWebSocket() {
        webSocketHandler.connect()
    }

    /**
     * 客户端回调start
     */
    fun onConnected() {
        runOnUiThread {
            mAdapter!!.addDataAndScroll("连接成功", true)
            btnConnect!!.isEnabled = false
            btnClose!!.isEnabled = true
        }
    }

    fun onDisconnect() {
        runOnUiThread {
            mAdapter!!.addDataAndScroll("连接断开", true)
            btnConnect!!.isEnabled = true
            btnClose!!.isEnabled = false
        }
    }

    fun onError(ex: Throwable) {
        LogUtils.e("客户端日志", "连接失败", ex.message)
    }

    fun onMessageText(message: String) {
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

    companion object {
        private const val PORT = 8800
        private const val REGEX = "ws:/"
    }
}