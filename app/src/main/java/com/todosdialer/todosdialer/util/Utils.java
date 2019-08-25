package com.todosdialer.todosdialer.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.getkeepsafe.relinker.ReLinker;
import com.todosdialer.todosdialer.R;
import com.todosdialer.todosdialer.manager.RealmManager;
import com.todosdialer.todosdialer.model.Friend;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import me.everything.providers.android.contacts.Contact;

public class Utils {
    public static String[] checkPermissions(Context context) {
        ArrayList<String> permissionToRequest = new ArrayList<>();
        if (checkNoPermission(context, Manifest.permission.ACCESS_NETWORK_STATE)) {
            permissionToRequest.add(Manifest.permission.ACCESS_NETWORK_STATE);
        }

        if (checkNoPermission(context, Manifest.permission.READ_PHONE_STATE)) {
            permissionToRequest.add(Manifest.permission.READ_PHONE_STATE);
        }

        if (checkNoPermission(context, Manifest.permission.ACCESS_WIFI_STATE)) {
            permissionToRequest.add(Manifest.permission.ACCESS_WIFI_STATE);
        }

        if (checkNoPermission(context, Manifest.permission.CHANGE_WIFI_STATE)) {
            permissionToRequest.add(Manifest.permission.CHANGE_WIFI_STATE);
        }

        if (checkNoPermission(context, Manifest.permission.READ_CONTACTS)) {
            permissionToRequest.add(Manifest.permission.READ_CONTACTS);
        }

        if (checkNoPermission(context, Manifest.permission.RECORD_AUDIO)) {
            permissionToRequest.add(Manifest.permission.RECORD_AUDIO);
        }

        if (checkNoPermission(context, Manifest.permission.USE_SIP)) {
            permissionToRequest.add(Manifest.permission.USE_SIP);
        }

        /*if (checkNoPermission(context, Manifest.permission.CALL_PHONE)) {
            permissionToRequest.add(Manifest.permission.CALL_PHONE);
        }*/

        if (checkNoPermission(context, Manifest.permission.MODIFY_AUDIO_SETTINGS)) {
            permissionToRequest.add(Manifest.permission.MODIFY_AUDIO_SETTINGS);
        }

        if (checkNoPermission(context, Manifest.permission.BLUETOOTH)) {
            permissionToRequest.add(Manifest.permission.BLUETOOTH);
        }
/*
        if (checkNoPermission(context, Manifest.permission.PROCESS_OUTGOING_CALLS)) {
            permissionToRequest.add(Manifest.permission.PROCESS_OUTGOING_CALLS);
        }
*/
        if (checkNoPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        if (checkNoPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        /*if (checkNoPermission(context, Manifest.permission.SEND_SMS)) {
            permissionToRequest.add(Manifest.permission.SEND_SMS);
        }*/

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkNoPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)) {
            permissionToRequest.add(Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        }

        String[] permissionArray = new String[permissionToRequest.size()];
        for (int i = 0; i < permissionToRequest.size(); i++) {
            permissionArray[i] = permissionToRequest.get(i);
        }

        return permissionArray;
    }

    public static boolean checkNoPermission(Context context, String permission) {
        int permissionState = ActivityCompat.checkSelfPermission(context, permission);
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !(permissionState == PackageManager.PERMISSION_GRANTED);
    }

    /*네트워크 타입 파악하여 연결 가능한지 확인하여 return
    * */

    public static boolean isNetworkStateFine(Context context) {
        if (context == null) {
            return true;
        }
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mobile = manager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        NetworkInfo wifi = manager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        NetworkInfo lte_4g = manager.getNetworkInfo(ConnectivityManager.TYPE_WIMAX);
        boolean blte_4g = false;
        if (lte_4g != null)
            blte_4g = lte_4g.isConnected();//lte 상태 체크
        if (mobile != null) {
            if (mobile.isConnected() || wifi.isConnected() || blte_4g) //모바일 네트워크
                return true;
        } else {
            if (wifi.isConnected() || blte_4g) //Wifi 네트워크
                return true;
        }
        return false;
    }



    public static boolean isEmailInvalid(String email) {
        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(email);
        return !matcher.matches();
    }

    public static boolean isPasswordInvalid(String password) {
        String expression = "^[a-zA-Z0-9!@.#$%^&*?_~]{8,20}$";

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(password);
        return !matcher.matches();
    }

    @SuppressLint("HardwareIds")
    public static String getUUID(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED && telephonyManager != null) {
            UUID uuid;
            final String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            try {
                if (!"9774d56d682e549c".equals(androidId)) {
                    uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                } else {
                    final String deviceId = telephonyManager.getDeviceId();
                    uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                }
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }

            return uuid.toString();
        } else {
            return "";
        }
    }

    public static String getPhoneNumber(Context context) {
        return getPhoneNumber(context, true);
    }

    @SuppressLint("HardwareIds")
    public static String getPhoneNumber(Context context, boolean hasHyphen) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            return "0";
        }
        String phoneNumber = telephonyManager == null || TextUtils.isEmpty(telephonyManager.getLine1Number()) ?
                "0" :
                telephonyManager.getLine1Number();

        if ("0".equals(phoneNumber)) {
            return phoneNumber;
        }

        if (phoneNumber.startsWith("+82")) {
            phoneNumber = "0" + phoneNumber.substring(3);
        } else if (phoneNumber.startsWith("82")) {
            phoneNumber = "0" + phoneNumber.substring(2);
        }

        if (hasHyphen && !phoneNumber.contains("-")) {
            String formatted = phoneNumber.substring(0, 3);
            formatted += "-";
            formatted += phoneNumber.substring(3, 7);
            formatted += "-";
            formatted += phoneNumber.substring(7, phoneNumber.length());
            return formatted;
        }
        return phoneNumber;
    }

    public static String convertToLocal(long time, String format) {
        SimpleDateFormat formatOutgoing = new SimpleDateFormat(format, Locale.getDefault());
        TimeZone tz = TimeZone.getDefault();
        formatOutgoing.setTimeZone(tz);

        Calendar calendar = Calendar.getInstance(tz, Locale.getDefault());
        calendar.setTimeInMillis(time);
        return formatOutgoing.format(calendar.getTime());
    }

    public static String formatTime(Date time, String format) {
        SimpleDateFormat formatOutgoing = new SimpleDateFormat(format, Locale.getDefault());
        TimeZone tz = TimeZone.getDefault();
        formatOutgoing.setTimeZone(tz);
        return formatOutgoing.format(time);
    }

    public static Friend findFriend(List<Contact> contacts, String phoneNumber) {
        phoneNumber = phoneNumber.replace(" ", "").trim();
        String another = phoneNumber;
        if (another.contains("-")) {
            another = another.replace("-", "");
        } else {
            another = Utils.formattedPhoneNumber(phoneNumber);
        }

        for (int i = 0; i < contacts.size(); i++) {
            String contactPhone = contacts.get(i).phone;
            contactPhone = contactPhone.trim();
            contactPhone = contactPhone.replace(" ", "");

            String anotherContactPhone = contactPhone.replace("-", "");

            if (contactPhone.equals(phoneNumber) ||
                    contactPhone.equals(another) ||
                    anotherContactPhone.equals(phoneNumber) ||
                    anotherContactPhone.equals(another)) {
                Friend friend = new Friend();
                friend.setPid(contacts.get(i).id);
                friend.setName(contacts.get(i).displayName);
                friend.setNumber(contactPhone);
                friend.setUriPhoto(contacts.get(i).uriPhoto);
                return friend;
            }
        }

        return null;
    }

    public static void loadNativeLibraries(Context context) {
        try {
            ReLinker.recursively().loadLibrary(context, "pjsua2");
        } catch (UnsatisfiedLinkError error) {
            error.printStackTrace();
            RealmManager.newInstance().writeLog("[Todos] loadNativeLibraries Exception: " + Log.getStackTraceString(error));
        }
    }

    public static String formattedPhoneNumber(String phone) {
        String anotherPhoneNumber = phone;
        if (!anotherPhoneNumber.contains("-")) {
            if (anotherPhoneNumber.length() > 7) {
                String temp = anotherPhoneNumber.substring(0, 3) + "-";
                temp += anotherPhoneNumber.substring(3, 7) + "-";
                temp += anotherPhoneNumber.substring(7);

                anotherPhoneNumber = temp;
            }
        }
        return anotherPhoneNumber;
    }

    public static String formattedBirthday(String birthday) {
        String formattedBirthday = birthday;
        if (!formattedBirthday.contains("-")) {
            if (formattedBirthday.length() > 6) {
                String temp = formattedBirthday.substring(0, 4) + "-";
                temp += formattedBirthday.substring(4, 6) + "-";
                temp += formattedBirthday.substring(6);

                formattedBirthday = temp;
            }
        }
        return formattedBirthday;
    }

    public static String getAppVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String getIp() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface
                    .getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf
                        .getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return "Can't find..";
    }

    /**
     * 클립보드에 주소 복사 기능
     * @param context
     * @param link
     */
    public static void setClipBoardLink(Context context, String link){

        ClipboardManager clipboardManager = (ClipboardManager)context.getSystemService(context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("label", link);
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(context, context.getString(R.string.toast_text_clipboard_adress), Toast.LENGTH_SHORT).show();

    }
    //출처: http://iw90.tistory.com/154 [woong's]
}
