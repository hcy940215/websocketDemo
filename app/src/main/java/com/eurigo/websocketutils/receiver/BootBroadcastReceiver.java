package com.eurigo.websocketutils.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.eurigo.websocketutils.HomeActivity;

public class BootBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
            Intent intentTo = new Intent(context, HomeActivity.class);
            intentTo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentTo);
        }
        if (intent.getAction().equals(Intent.ACTION_PACKAGE_REPLACED)) {
            String packageName = intent.getData().getSchemeSpecificPart();
            if ("com.eurigo.websocketutils".equals(packageName)) {
                Intent intentTo = new Intent(context, HomeActivity.class);
                intentTo.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intentTo);
            }
        }
    }
}

