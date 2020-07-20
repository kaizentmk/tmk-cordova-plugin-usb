package tmk.cordova.plugin.usb;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import java.util.HashMap;
import java.util.Map;

import lombok.NoArgsConstructor;

import static org.apache.cordova.PluginResult.Status.ERROR;
import static org.apache.cordova.PluginResult.Status.OK;
import static tmk.cordova.plugin.usb.TmkUsbLogging.getTime;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;

@NoArgsConstructor
public class TmkUsbGui {

    public static final TmkUsbGui INSTANCE = new TmkUsbGui();

    public static final String tag = "tug::";

    private Gson gson = new Gson();

    public CallbackContext connectWithGui(
            final CallbackContext callbackContext) {
        logtmk(tag, "connectWithGui: start");

        callbackContext.sendPluginResult(
                makeOkKeepPluginResult(
                        msg("gui", "connected")));

        logtmk(tag, "connectWithGui: end");
        return callbackContext;
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

    public String msg(final String type, final String msg) {
        Map<String, String> data = new HashMap<>();
        data.put("type", type);
        data.put("time", getTime());
        data.put("data", msg);

        return gson.toJson(data);
    }
}
