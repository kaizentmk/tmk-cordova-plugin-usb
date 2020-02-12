package tmk.cordova.plugin.usb;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import lombok.NoArgsConstructor;

import static org.apache.cordova.PluginResult.Status.ERROR;
import static org.apache.cordova.PluginResult.Status.OK;

@NoArgsConstructor
public class TmkUsbGui {

    public static final TmkUsbGui TMK_USB_GUI =
            new TmkUsbGui();


    public CallbackContext connectWithGui(
            final CallbackContext callbackContext) {

        callbackContext.sendPluginResult(makeOkKeepPluginResult("usb plugin connected"));

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
}
