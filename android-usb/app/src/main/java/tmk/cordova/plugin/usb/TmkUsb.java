package tmk.cordova.plugin.usb;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * https://cordova.apache.org/docs/en/9.x/guide/platforms/android/plugin.html
 */
public class TmkUsb extends CordovaPlugin {

    private int count = 0;

    @Override
    public boolean execute(String action,
                           JSONArray data,
                           CallbackContext callbackContext)
            throws JSONException {

        count++;
        try {
            switch (action) {
                case "greet":
                    return greet(data, callbackContext);
                case "info":
                    return info(data, callbackContext);
                case "listUsbDevices":
                    return listDevices(data, callbackContext);
                default:
                    callbackContext.error("Unsupported action: " + action);
                    return false;
            }
        } catch (Throwable t) {
            callbackContext.error(t.getMessage());
            return false;
        }

    }


    private boolean greet(JSONArray data, CallbackContext callbackContext) throws JSONException {
        String name = data.getString(0);
        String message = count + " - Hello, " + name;
        callbackContext.success(message);

        return true;
    }

    private boolean info(JSONArray data, CallbackContext callbackContext)
            throws JSONException {

        System.out.println("------- tmk usb info");

        JSONObject jo = new JSONObject();
        jo.put("serviceName", this.getServiceName());
        jo.put("hasPermisssion", this.hasPermisssion());
        callbackContext.success(jo.toString());
        return true;
    }


    private boolean listDevices(JSONArray data, CallbackContext callbackContext) {
        JSONArray ja = new JSONArray();
        callbackContext.success(ja.toString());
        return true;
    }
}
