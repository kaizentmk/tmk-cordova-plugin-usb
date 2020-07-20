package tmk.cordova.plugin.usb;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.concurrent.LinkedBlockingDeque;

import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;

public class TmkUsbLogging {

    static final int MAX_LOG_BUFF_SIZE = 1024;
//    public static final LinkedList<String> LOG_BUFF = new LinkedList<>();

    public static final LinkedBlockingDeque<String> LOG_BUFF =
            new LinkedBlockingDeque();

    public static synchronized String getTime() {
        Date date = Calendar.getInstance().getTime();
        return new SimpleDateFormat("HH:mm:ss.SSS").format(date);
    }

    public static synchronized void logtmk(final String... msgs) {
        if (LOG_BUFF.size() > MAX_LOG_BUFF_SIZE) {
            LOG_BUFF.removeLast();
        }

        String msg = getTime() + "::" + Arrays.toString(msgs);
        Log.i(TAG, msg);
        LOG_BUFF.add(msg);
    }

    public static synchronized void logtmkerr(final String... msgs) {
        if (LOG_BUFF.size() > MAX_LOG_BUFF_SIZE) {
            LOG_BUFF.removeLast();
        }

        String msg = getTime() + "::" + "ERR:" + Arrays.toString(msgs);
        Log.e(TAG, msg);
        LOG_BUFF.add(msg);
    }

    public synchronized static void clearLogs() {
        LOG_BUFF.clear();
    }

    public static Collection<String> getLogs() {
        return LOG_BUFF;
    }
}
