package com.eurigo.websocketutils;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.UUID;

public class DeviceIdUtil {
    /**
     * 获得设备硬件标识
     *
     * @param context 上下文
     *
     * @return 设备硬件标识
     */
    public static String getDeviceId(Context context) {
        StringBuilder sbDeviceId = new StringBuilder();

        //获得设备默认IMEI（>=6.0 需要ReadPhoneState权限）
        String imei = getIMEI(context);
        //获得AndroidId（无需权限）
        String androidid = getAndroidId(context);
        //获得设备序列号（无需权限）
        String serial = getSERIAL();
        //获得硬件uuid（根据硬件相关属性，生成uuid）（无需权限）
        String uuid = getDeviceUUID().replace("-", "");

        //追加imei
//        if (imei != null && imei.length() > 0) {
//            sbDeviceId.append(imei);
//            sbDeviceId.append("|");
//        }
        //追加androidid
        if (androidid != null && androidid.length() > 0) {
            sbDeviceId.append(androidid);
            sbDeviceId.append("|");
        }
        //追加serial
        if (serial != null && serial.length() > 0) {
            sbDeviceId.append(serial);
            sbDeviceId.append("|");
        }
        //追加硬件uuid
        if (uuid != null && uuid.length() > 0) {
            sbDeviceId.append(uuid);
        }

        //生成SHA1，统一DeviceId长度
        if (sbDeviceId.length() > 0) {
            try {
                byte[] hash = getHashByString(sbDeviceId.toString());
                String sha1 = bytesToHex(hash);
                if (sha1 != null && sha1.length() > 0) {
                    //返回最终的DeviceId
                    return sha1;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        //如果以上硬件标识数据均无法获得，
        //则DeviceId默认使用系统随机数，这样保证DeviceId不为空
        return UUID.randomUUID().toString().replace("-", "");
    }

    //需要获得READ_PHONE_STATE权限，>=6.0，默认返回null
    private static String getIMEI(Context context) {
        try {
            TelephonyManager tm = (TelephonyManager)
                    context.getSystemService(Context.TELEPHONY_SERVICE);
            return tm.getDeviceId();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 获得设备的AndroidId
     *
     * @param context 上下文
     *
     * @return 设备的AndroidId
     */
    private static String getAndroidId(Context context) {
        try {
            return Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 获得设备序列号（如：WTK7N16923005607）, 个别设备无法获取
     *
     * @return 设备序列号
     */
    private static String getSERIAL() {
        try {
            return Build.SERIAL;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "";
    }

    /**
     * 获得设备硬件uuid
     * 使用硬件信息，计算出一个随机数
     *
     * @return 设备硬件uuid
     */
    private static String getDeviceUUID() {
        try {
            String dev = "3883756" +
                    Build.BOARD.length() % 10 +
                    Build.BRAND.length() % 10 +
                    Build.DEVICE.length() % 10 +
                    Build.HARDWARE.length() % 10 +
                    Build.ID.length() % 10 +
                    Build.MODEL.length() % 10 +
                    Build.PRODUCT.length() % 10 +
                    Build.SERIAL.length() % 10;
            return new UUID(dev.hashCode(),
                    Build.SERIAL.hashCode()).toString();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "";
        }
    }

    /**
     * 取SHA1
     *
     * @param data 数据
     *
     * @return 对应的hash值
     */
    private static byte[] getHashByString(String data) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA1");
            messageDigest.reset();
            messageDigest.update(data.getBytes("UTF-8"));
            return messageDigest.digest();
        } catch (Exception e) {
            return "".getBytes();
        }
    }

    /**
     * 转16进制字符串
     *
     * @param data 数据
     *
     * @return 16进制字符串
     */
    public static String bytesToHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        String stmp;
        for (int n = 0; n < data.length; n++) {
            stmp = (Integer.toHexString(data[n] & 0xFF));
            if (stmp.length() == 1)
                sb.append("0");
            sb.append(stmp);
        }
        return sb.toString().toUpperCase(Locale.CHINA);
    }

    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");
        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }


    /**
     * 适配android9的安装方法。
     * 全部替换安装
     *
     * @return
     */
    private static int mSessionId = -1;

    public static Boolean installApk(Context context, String apkFilePath) {
        File apkFile = new File(apkFilePath);
        if (!apkFile.exists()) {
            return null;
        }
        PackageInfo packageInfo = context.getPackageManager().getPackageArchiveInfo(apkFilePath, PackageManager.GET_ACTIVITIES | PackageManager.GET_SERVICES);
        if (packageInfo != null) {
            String packageName = packageInfo.packageName;
            int versionCode = packageInfo.versionCode;
            String versionName = packageInfo.versionName;
            Log.e("ApkActivity", "packageName=" + packageName + ", versionCode=" + versionCode + ", versionName=" + versionName);
        }

        PackageInstaller packageInstaller = context.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams sessionParams
                = new PackageInstaller.SessionParams(PackageInstaller
                .SessionParams.MODE_FULL_INSTALL);
        sessionParams.setSize(apkFile.length());
        try {
            mSessionId = packageInstaller.createSession(sessionParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mSessionId != -1) {
            boolean copySuccess = onTransfersApkFile(context, apkFilePath);
            if (copySuccess) {
                execInstallAPP(context);
            }
        }
        return null;
    }

    /**
     * 通过文件流传输apk
     *
     * @param apkFilePath
     *
     * @return
     */
    private static boolean onTransfersApkFile(Context context, String apkFilePath) {
        InputStream in = null;
        OutputStream out = null;
        PackageInstaller.Session session = null;
        boolean success = false;
        try {
            File apkFile = new File(apkFilePath);
            session = context.getPackageManager().getPackageInstaller().openSession(mSessionId);
            Log.e("ApkActivity", "apkFile:" + apkFilePath);
            Log.e("ApkActivity", "apkFile:" + apkFile.getName());
            //看别人传的是base.apk；个人建议如果你应用被授权了，base.apk无效后，建议更换为传入的apk路径
            out = session.openWrite("base.apk", 0, apkFile.length());
            in = new FileInputStream(apkFile);
            int total = 0, c;
            byte[] buffer = new byte[1024 * 1024];
            while ((c = in.read(buffer)) != -1) {
                total += c;
                out.write(buffer, 0, c);
            }
            session.fsync(out);
            success = true;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != session) {
                session.close();
            }
            try {
                if (null != out) {
                    out.close();
                }
                if (null != in) {
                    in.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return success;
    }

    /**
     * 执行安装并通知安装结果
     */
    private static void execInstallAPP(Context context) {
        PackageInstaller.Session session = null;
        try {
            Log.e("ApkActivity", "mSessionId:" + mSessionId);
            session = context.getPackageManager().getPackageInstaller().openSession(mSessionId);
            Intent intent = new Intent(context, InstallResultReceiver.class);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            session.commit(pendingIntent.getIntentSender());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != session) {
                session.close();
            }
        }
    }

    public static class InstallResultReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d("install", "已收到安装反馈广播 action:" + action);
            if (intent != null) { //安装的广播
                final int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS,
                        PackageInstaller.STATUS_FAILURE);
                if (status == PackageInstaller.STATUS_SUCCESS) {
                    // success
                    // InstallAPP.getInstance().sendInstallSucces();
                } else {
                    String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
                    //InstallAPP.getInstance().sendFailure(msg);
                }
            }
        }
    }

    /**
     * 执行具体的静默安装逻辑，需要手机ROOT。
     *
     * @param apkPath 要安装的apk文件的路径
     *
     * @return 安装成功返回true，安装失败返回false。
     */
    public static boolean install(String apkPath) {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
            String command = "pm install -r " + apkPath + " && ";

            String command2 = "am start -n com.yuwo.vendingmachine/com.yuwo.vendingmachine.ui.MainActivity\n";

            String cmd = command + command2;
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
        } catch (Exception e) {
            Log.e("TAG", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
            } catch (IOException e) {
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }

    public static boolean reStartApp() {
        boolean result = false;
        DataOutputStream dataOutputStream = null;
        try {
            // 申请su权限
            Process process = Runtime.getRuntime().exec("su");
            dataOutputStream = new DataOutputStream(process.getOutputStream());
            // 执行pm install命令
//            String command = "adb shell am force-stop com.yuwo.vendingmachine.ui && ";
//            String command = "adb shell pm clear com.yuwo.vendingmachine\n";

            String command2 = "am start -n com.yuwo.vendingmachine/com.yuwo.vendingmachine.ui.MainActivity\n";

            String cmd = command2;
            dataOutputStream.writeBytes(cmd);
            dataOutputStream.flush();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("TAG", e.getMessage(), e);
        } finally {
            try {
                if (dataOutputStream != null) {
                    dataOutputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e("TAG", e.getMessage(), e);
            }
        }
        return result;
    }
}


