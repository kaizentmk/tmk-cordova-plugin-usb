package tmk.cordova.plugin.usb;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;

public class TmkUsb extends CordovaPlugin {

    private int count = 0;

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        count++;

        if (action.equals("listUsbDevices")) {

            String name = data.getString(0);
            String message = count + "Hello, " + name;
            callbackContext.success(message);

            return true;

        } else {
            
            return false;

        }
    }
}
