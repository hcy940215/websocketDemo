package com.eurigo.websocketutils.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.eurigo.websocketutils.utils.LiveDataBus;

import static com.eurigo.websocketutils.utils.ConstantKt.EVENT_NET_WORK;


public class NetworkConnectChangedReceiver extends BroadcastReceiver {

    private static final String TAG = "Network";

    @Override
    public void onReceive(Context context, Intent intent) {
        //检测API是不是小于23，因为到了API23之后getNetworkInfo(int networkType)方法被弃用
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) {

            //获得ConnectivityManager对象
            ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            //获取ConnectivityManager对象对应的NetworkInfo对象
            //获取WIFI连接的信息
            NetworkInfo wifiNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            //获取移动数据连接的信息
            NetworkInfo dataNetworkInfo = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            LiveDataBus.INSTANCE.with(EVENT_NET_WORK).postStickyData(wifiNetworkInfo.isConnected() || dataNetworkInfo.isConnected());

        } else {

            // 这个监听网络连接的设置，包括wifi和移动数据的打开和关闭。.
            // 最好用的还是这个监听。wifi如果打开，关闭，以及连接上可用的连接都会接到监听。
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

                NetworkInfo activeNetwork = manager.getActiveNetworkInfo();
                if (activeNetwork != null) { // connected to the internet
                    if (activeNetwork.isConnected()) {
                        if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                            // connected to wifi
                            Log.e(TAG, "当前WiFi连接可用 ");
                        } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                            Log.e(TAG, "当前移动网络连接可用 ");
                        }
                        LiveDataBus.INSTANCE.with(EVENT_NET_WORK).postStickyData(true);
                    } else {
                        Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                        LiveDataBus.INSTANCE.with(EVENT_NET_WORK).postStickyData(false);
                    }

                } else {   // not connected to the internet
                    Log.e(TAG, "当前没有网络连接，请确保你已经打开网络 ");
                    LiveDataBus.INSTANCE.with(EVENT_NET_WORK).postStickyData(false);
                }
            }
        }

    }
}
