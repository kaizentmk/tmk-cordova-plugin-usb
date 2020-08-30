package tmk.cordova.plugin.usb.log;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.concurrent.LinkedBlockingDeque;


public class TmkUsbLogging {

    public static final String TAG = "drinker";
    public static final String LOG_DOMAIN = "log";

    private static final Calendar calendar = Calendar.getInstance();

    static final int MAX_LOG_BUFF_SIZE = 10240;
//    public static final LinkedList<String> LOG_BUFF = new LinkedList<>();

    public static final LinkedBlockingDeque<String> LOG_BUFF =
            new LinkedBlockingDeque();

    public static synchronized String getTime() {
        return new SimpleDateFormat("HH:mm:ss.SSS")
                .format(calendar.getTime());
    }

    public static synchronized void logtmk(final String... msgs) {
        removeLastEntries(msgs);

        String msg = getTime() + "::" + Arrays.toString(msgs);
        Log.i(TAG, msg);
        LOG_BUFF.add(msg);
    }

    public static synchronized void logtmkerr(final String... msgs) {
        removeLastEntries(msgs);

        String msg = getTime() + "::" + "ERR:" + Arrays.toString(msgs);
        Log.e(TAG, msg);
        LOG_BUFF.add(msg);
    }

    public static synchronized void logtmkerr(
            final String tag,
            final String desc,
            final Throwable t) {

        String[] msgs = {
                tag,
                desc,
                t.getMessage(),
                Arrays.toString(t.getStackTrace())
        };

        removeLastEntries(msgs);

        String msg = getTime() + "::" + "ERR:" + Arrays.toString(msgs);
        Log.e(TAG, msg);
        LOG_BUFF.add(msg);
    }

    public synchronized static void clearLogs() {
        LOG_BUFF.clear();
    }

    public static Collection<String> getLogs() {
        Object[] logObjsArr = LOG_BUFF.toArray();
        return Arrays.asList(Arrays.copyOf(logObjsArr, logObjsArr.length, String[].class));
    }

    private static void removeLastEntries(String[] msgs) {
        int nml = msgs != null ? msgs.length : 0;
        if (LOG_BUFF.size() + nml > MAX_LOG_BUFF_SIZE) {
            for (int i = 0; i < nml; i++) {
                LOG_BUFF.removeLast();
            }
        }
    }
}
