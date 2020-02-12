package tmk.cordova.plugin.usb;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;

public class TmkUsbLogger {

    static final int MAX_LOG_BUFF_SIZE = 1000;
    public static final LinkedList<String> LOG_BUFF = new LinkedList<>();

    public static String getTime() {
        Date date = Calendar.getInstance().getTime();
        return new SimpleDateFormat("HH:mm:ss.SSS").format(date);
    }

    public static synchronized void logtmk(final String msg) {
        if (LOG_BUFF.size() > MAX_LOG_BUFF_SIZE) {
            LOG_BUFF.removeLast();
        }
        Log.d(TAG, msg);
    }

    public synchronized static void clearLogs() {
        LOG_BUFF.clear();
    }

    public static LinkedList<String> getLogs() {
        return LOG_BUFF;
    }
}
