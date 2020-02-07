package tmk.cordova.plugin.usb;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.Calendar;
import java.util.Date;

import static java.lang.Thread.sleep;
import static org.apache.cordova.PluginResult.Status.ERROR;
import static org.apache.cordova.PluginResult.Status.OK;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.getTime;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.logtmk;

public class TmkUsbGui {

    final CordovaInterface cordova;

    public TmkUsbGui(final CordovaInterface cordova) {
        this.cordova = cordova;
    }

    /**
     * Simple method for testing basic comunication
     *
     * @param data
     * @param callbackContext
     * @return
     * @throws JSONException
     */
    public boolean greet(final JSONArray data,
                         final CallbackContext callbackContext)
            throws JSONException {
        logtmk("greet: start");

        String name = data.getString(0);
        callbackContext.success(getTime() + " Hello, " + name);

        logtmk("greet: end");

        return true;
    }


    public CallbackContext connectWithGui(final CallbackContext callbackContext)
            throws TmkUsbException {

        logtmk("connectWithGui: start");
        if (callbackContext == null) {
            throw new TmkUsbException("cannot connect with gui: callbackContext is null");
        }

        callbackContext.sendPluginResult(makeOkKeepPluginResult("connected"));
        logtmk("connectWithGui: end");
        return callbackContext;
    }

    public boolean testAsync(final CallbackContext callbackContext)
            throws TmkUsbException {

        logtmk("testAsync: start");

        if (callbackContext == null
                || callbackContext.isFinished()) {
            throw new TmkUsbException("callbackContext = null or is finished: "
                    + callbackContext);
        }

        logtmk("testAsync: running new thread; this.callbackContext = " + callbackContext);
        cordova.getThreadPool().execute(() -> {
            int count = 0;

            do {
                Date time = Calendar.getInstance().getTime();
                String msg = getTime() + " testAsync " + ++count + ": " + time;
                callbackContext.sendPluginResult(makeOkKeepPluginResult(msg));
                logtmk(msg);

                try {
                    sleep(100);
                } catch (InterruptedException e) {
                    logtmk(e.getMessage());
                }

            } while (count < 7);

        });

        logtmk("testAsync: end");

        return true;
    }

    public PluginResult makeOkKeepPluginResult(String msg) {
        PluginResult result = new PluginResult(OK, msg);
        result.setKeepCallback(true);
        return result;
    }

    public PluginResult makeErrorPluginResult(String msg) {
        PluginResult result = new PluginResult(ERROR, msg);
        return result;
    }
}
