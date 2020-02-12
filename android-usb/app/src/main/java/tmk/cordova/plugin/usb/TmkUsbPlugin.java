package tmk.cordova.plugin.usb;


import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import static android.content.Context.USB_SERVICE;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static tmk.cordova.plugin.usb.TmkUsbLogger.clearLogs;
import static tmk.cordova.plugin.usb.TmkUsbLogger.getLogs;
import static tmk.cordova.plugin.usb.TmkUsbLogger.getTime;
import static tmk.cordova.plugin.usb.TmkUsbLogger.logtmk;

/**
 * https://cordova.apache.org/docs/en/9.x/guide/platforms/android/plugin.html
 * <p>
 * Plugins should use the initialize method for their start-up logic.
 * </p>
 */
public class TmkUsbPlugin extends CordovaPlugin {

    public static final String ACTION_USB_PERMISSION =
            "tmk.cordova.plugin.usb.USB_PERMISSION";

    public static final String TAG = "drinker";

    /**
     * For keeping connection with the gui.
     * Used in the TmkUsbBroadcastReceiver.
     * Sends data from usb to the gui.
     */
    CallbackContext callbackContext;

    /**
     * for writing data to the usb device
     */
    UsbSerialDevice usbSerialDevice;

    TmkUsbGui tmkUsbGui;
    TmkUsbBroadcastReceiver tmkUsbBroadcastReceiver;
    TmkUsbConfig tmkUsbConfig;

    int count = 1;

    UsbSerialInterface.UsbReadCallback usbReadCallback = data -> {
        String s = ("" + new String(data, UTF_8)).trim();
        logtmk(s);
        if (s.isEmpty()) {
            return;
        }

        sendMsgToGui(s);
    };

    public void sendMsgToGui(final String s) {
        callbackContext.sendPluginResult(
                tmkUsbGui.makeOkKeepPluginResult(getTime() + ": [[" + s + "]]"));
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Context context = cordova.getContext();

        UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);

        tmkUsbConfig = new TmkUsbConfig();

        tmkUsbBroadcastReceiver = new TmkUsbBroadcastReceiver(
                this,
                usbManager,
                tmkUsbConfig);

        registerReceiver(context);

        tmkUsbGui = new TmkUsbGui(cordova);

        tmkUsbBroadcastReceiver.listDevicesAndFindProperOne(context, usbManager);
    }

    public void setUsbSerialDevice(UsbSerialDevice usbSerialDevice) {
        this.usbSerialDevice = usbSerialDevice;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.cordova != null && this.cordova.getContext() != null) {
            this.cordova.getContext()
                    .unregisterReceiver(tmkUsbBroadcastReceiver);
        }

        if (usbSerialDevice != null) {
            usbSerialDevice.close();
        }
    }

    @Override
    public boolean execute(final String action,
                           final JSONArray data,
                           final CallbackContext callbackContext)
            throws JSONException {

        try {
            switch (action) {
                case "greet":
                    return tmkUsbGui.greet(data, callbackContext);
                case "write":
                    return writeToTheUsbSerialDevice(data, callbackContext);
                case "connect":
                    this.callbackContext = tmkUsbGui.connectWithGui(callbackContext);
                    return true;
                case "testAsync":
                    return tmkUsbGui.testAsync(this.callbackContext);
                case "getLogs":
                    return sendLogs(callbackContext);
                case "reset":
                    return reset(callbackContext);
                default:
                    throw new TmkUsbException("Unsupported action: " + action);
            }
        } catch (Throwable t) {
            if (callbackContext != null) {
                callbackContext.error("TmkUsbPluginError: " + t.getMessage());
            }
            return false;
        }
    }

    private boolean writeToTheUsbSerialDevice(JSONArray data, CallbackContext callbackContext) throws JSONException, TmkUsbException {
        if (usbSerialDevice == null
                || !usbSerialDevice.isOpen()) {
            throw new TmkUsbException("Cannot write to the usb device - is null or not opened");
        }

        String s = data.getString(0) + " c=" + count++ + "\r\n";

        cordova.getThreadPool().execute(() -> {
            try {
                usbSerialDevice.write(s.getBytes());

                this.callbackContext.sendPluginResult(
                        this.tmkUsbGui.makeOkKeepPluginResult("passed to write: s = " + s));
            } catch (Throwable t) {
                logtmk("cannot write: " + t.getMessage());
            }
        });


        return true;
    }

    private boolean sendLogs(final CallbackContext callbackContext) {
        callbackContext.success(new JSONArray(getLogs()));
        return true;
    }

    private boolean reset(final CallbackContext callbackContext) {
        clearLogs();
        this.count = 1;
        callbackContext.success("cleared");
        return true;
    }

    private void registerReceiver(Context context) {
        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_DEVICE_ATTACHED));

        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_DEVICE_DETACHED));

        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_PERMISSION));
    }
}
