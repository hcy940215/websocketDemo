package com.eurigo.websocketutils.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import com.eurigo.websocketutils.DeviceIdUtil
import com.eurigo.websocketutils.R
import org.java_websocket.handshake.ServerHandshake
import java.net.URI

class JWebSocketClientService : Service() {
    var client: JWebSocketClient? = null
    private val mBinder = JWebSocketClientBinder()

    //灰色保活
    class GrayInnerService : Service() {
        override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
            startForeground(GRAY_SERVICE_ID, Notification())
            stopForeground(true)
            stopSelf()
            return super.onStartCommand(intent, flags, startId)
        }

        override fun onBind(intent: Intent): IBinder? {
            return null
        }
    }

    var wakeLock //锁屏唤醒
            : PowerManager.WakeLock? = null

    //获取电源锁，保持该服务在屏幕熄灭时仍然获取CPU时，保持运行
    @SuppressLint("InvalidWakeLockTag")
    private fun acquireWakeLock() {
        if (null == wakeLock) {
            val pm = this.getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = pm.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ON_AFTER_RELEASE,
                "PostLocationService"
            )
            if (null != wakeLock) {
                wakeLock!!.acquire()
            }
        }
    }

    //用于Activity和service通讯
    inner class JWebSocketClientBinder : Binder() {
        val service: JWebSocketClientService
            get() = this@JWebSocketClientService
    }

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        //初始化websocket
        initSocketClient()
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE) //开启心跳检测
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        //初始化websocket
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE) //开启心跳检测
        acquireWakeLock()
        return START_STICKY
    }

    override fun onDestroy() {
        closeConnect()
        super.onDestroy()
    }

    var notificationManager: NotificationManager? = null
    var notificationId = "channelId"
    var notificationName = "channelName"
    private fun startForegroundService() {
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        //创建NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                notificationId, notificationName, NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager!!.createNotificationChannel(channel)
        }
    }

    //设置Notification的ChannelID,否则不能正常显示
    private val notification: Notification
        private get() {
            val builder = Notification.Builder(this).setSmallIcon(
                R
                    .mipmap.ic_launcher
            )
                .setContentTitle("随访包服务").setContentText("随访包服务正在运行...")
            //设置Notification的ChannelID,否则不能正常显示
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                builder.setChannelId(notificationId)
            }
            return builder.build()
        }

    /**
     * 初始化websocket连接
     */
    fun initSocketClient() {
        var substring = DeviceIdUtil.getDeviceId(this)
        if ((substring.length > 10)) {
            substring = substring.substring(0, 10)
        }
        val uri = URI.create("wss://ads.hfyuwo.com/webSocketServers/$substring")
        Log.e("websocket", "连接的地址是************************：$uri")
        client = object : JWebSocketClient(uri) {
            override fun onMessage(message: String) {
                Log.e("websocket", "Service收到的消息：$message")
                val intent = Intent()
                intent.action = "com.dhy.health.healthhut"
                intent.putExtra("message", message)
                intent.setPackage(packageName)
                sendBroadcast(intent)

            }

            override fun onOpen(handshakedata: ServerHandshake) {
                super.onOpen(handshakedata)

                mHandler.removeCallbacks(heartBeatRunnable)
                mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE) //开启心跳检测
                if (null != client) {
                    Log.e(
                        "websocket",
                        "第一次连接成功发送的消息：-----------------------------------" + "Heartbeat"
                    )
//                    client!!.send("ok")
                }

                val intent = Intent()
                intent.action = "com.dhy.health.healthhut"
                intent.putExtra("message", "连接成功")
                intent.setPackage(packageName)
                sendBroadcast(intent)
            }
        }
        client?.connectionLostTimeout = 110 * 1000
        connect()
    }

    /**
     * 连接websocket
     */
    private fun connect() {
        object : Thread() {
            override fun run() {
                try {
                    //connectBlocking多出一个等待操作，会先连接再发送，否则未连接发送会报错
                    client!!.connectBlocking()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    /**
     * 发送消息
     *
     * @param msg
     */
    fun sendMsg(msg: String) {
        if (null != client) {
            if (client!!.isOpen) {
                client!!.send(msg + "")
            } else {
                Log.e(
                    "websocket", "重连中"
                )
                reconnectWs()
            }
        } else {
            Log.e(
                "websocket", "重连中"
            )
            initSocketClient()
        }
    }


    override fun unbindService(conn: ServiceConnection) {
        super.unbindService(conn)
        closeConnect()
    }

    /**
     * 断开连接
     */
    private fun closeConnect() {
        try {
            if (null != client) {
                mHandler.removeCallbacks(heartBeatRunnable)
                client!!.close()
                client = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            client = null
        }
    }

    private val mHandler = Handler()
    private val heartBeatRunnable: Runnable = object : Runnable {
        override fun run() {
            if (client != null) {
                Log.e("心跳包检测websocket连接状态", client!!.isClosed.toString() + "/")
                if (client!!.isClosed) {
                    reconnectWs()
                } else {
                    sendMsg("{\"type\": \"1\"}")
                }
            } else {
                //如果client已为空，重新初始化连接
                client = null
                initSocketClient()
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler.postDelayed(this, HEART_BEAT_RATE)
        }
    }
    private val mHandler2 = Handler()
    private val heartBeatRunnable2: Runnable = object : Runnable {
        override fun run() {
            Log.e("", "发送Heartbeat心跳包检测websocket连接状态" + "/")
            if (client != null) {
                if (!client!!.isClosed) {
                    sendMsg("{\"type\": \"1\"}")
                } else {
                    reconnectWs()
                }
                //                sendMsg("Heartbeat");
            } else {
                //如果client已为空，重新初始化连接
                client = null
                initSocketClient()
            }
            //每隔一定的时间，对长连接进行一次心跳检测
            mHandler2.postDelayed(this, HEART_BEAT_RATE2)
        }
    }

    /**
     * 开启重连
     */
    private fun reconnectWs() {
        mHandler.removeCallbacks(heartBeatRunnable)
        object : Thread() {
            override fun run() {
                try {
                    Log.e("开启重连", "")
                    client!!.reconnectBlocking()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun sendText() {
        if (client != null) {
            Log.e("心跳包检测websocket连接状态", client!!.isClosed.toString() + "/")
            if (client!!.isClosed) {
                reconnectWs()
            } else {
                sendMsg("{\"type\": \"1\"}")
            }
        } else {
            //如果client已为空，重新初始化连接
            client = null
            initSocketClient()
        }
    }

    companion object {
        private const val GRAY_SERVICE_ID = 1001

        //    -------------------------------------websocket心跳检测------------------------------------------------
        private const val HEART_BEAT_RATE = (30 * 1000).toLong()

        //    -------------------------------------websocket心跳检测------------------------------------------------
        private const val HEART_BEAT_RATE2 = (30 * 1000).toLong()
    }
}