package tmk.cordova.plugin.usb.gui;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;

import static org.apache.cordova.PluginResult.Status.ERROR;
import static org.apache.cordova.PluginResult.Status.OK;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.getTime;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmk;

@NoArgsConstructor
public class TmkUsbGui {

    public static final TmkUsbGui INSTANCE = new TmkUsbGui();

    public static final String tag = "tug";

    private Gson gson = new Gson();

    private CallbackContext callbackContext;

    public void connectWithGui(
            final CallbackContext callbackContext) {

        logtmk(tag, "connectWithGui: start");

        callbackContext.sendPluginResult(
                makeOkKeepPluginResult(
                        msg("gui", "connected")));

        logtmk(tag, "connectWithGui: end");
        this.callbackContext = callbackContext;
    }

    public PluginResult makeOkKeepPluginResult(String msg) {
        PluginResult result = new PluginResult(OK, msg);
        result.setKeepCallback(true);
        return result;
    }

    public PluginResult makeErrorKeepPluginResult(String msg) {
        PluginResult result = new PluginResult(ERROR, msg);
        result.setKeepCallback(true);
        return result;
    }

    public void sendOkMsg(final String domain, final String msg) {
        if (clbckCtxIsNotSet()) {
            return;
        }

        this.callbackContext.sendPluginResult(
                makeOkKeepPluginResult(msg(domain, msg)));
    }

    private void sendErrMsg(final String msg, final String type) {
        if (clbckCtxIsNotSet()) {
            return;
        }

        this.callbackContext.sendPluginResult(
                makeErrorKeepPluginResult(msg(type, msg)));
    }

    public String msg(final String domain, final String msg) {
        Map<String, String> data = new HashMap<>();
        data.put("type", domain);
        data.put("data", msg);
        data.put("time", getTime());

        return gson.toJson(data);
    }

    public String msg(final String domain, final String msg, final Throwable t) {
        String[] msgs = {
                msg,
                t.getMessage(),
                Arrays.toString(t.getStackTrace())
        };

        return msg(domain, Arrays.toString(msgs));
    }

    private boolean clbckCtxIsNotSet() {
        if (this.callbackContext == null) {
            logtmk(tag, "callbackContext is not set. Connect with gui first.");
            return true;
        }

        return false;
    }
}
