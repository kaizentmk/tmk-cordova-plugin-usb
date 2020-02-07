package tmk.cordova.plugin.usb;


import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;

import static android.content.Context.USB_SERVICE;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    private static final int VENDOR_ID = 0x2341; // 9026
    private static final int PRODUCT_ID = 0x003E; // 62

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

    int count = 1;

    static final int MAX_LOG_BUFF_SIZE = 1000;
    private static final LinkedList<String> LOG_BUFF = new LinkedList<>();

    public static String getTime() {
        Date date = Calendar.getInstance().getTime();
        return new SimpleDateFormat("HH:mm:ss.SSS").format(date);
    }

    public static synchronized void logtmk(final String msg) {
        LOG_BUFF.add(getTime() + ": " + msg);
        if (LOG_BUFF.size() > MAX_LOG_BUFF_SIZE) {
            LOG_BUFF.removeLast();
        }
        Log.d(TAG, msg);
    }

    UsbSerialInterface.UsbReadCallback usbReadCallback = data -> {
        String s = ("" + new String(data, UTF_8)).trim();
        logtmk(s);
        if (s.isEmpty()) {
            return;
        }

        callbackContext.sendPluginResult(
                tmkUsbGui.makeOkKeepPluginResult(getTime() + ": [[" + s + "]]"));
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        Context context = cordova.getContext();

        UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);

        tmkUsbBroadcastReceiver = new TmkUsbBroadcastReceiver(
                this,
                usbManager,
                VENDOR_ID,
                PRODUCT_ID);

        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_DEVICE_ATTACHED));

        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_DEVICE_DETACHED));

        context.registerReceiver(
                tmkUsbBroadcastReceiver,
                new IntentFilter(ACTION_USB_PERMISSION));

        tmkUsbGui = new TmkUsbGui(cordova);

        tmkUsbBroadcastReceiver.listDevicesAndFindProperOne(context, usbManager);
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
        callbackContext.success(new JSONArray(LOG_BUFF));
        return true;
    }

    private boolean reset(final CallbackContext callbackContext) {
        LOG_BUFF.clear();
        this.count = 1;
        callbackContext.success("cleared");
        return true;
    }

    public void setUsbSerialDevice(UsbSerialDevice usbSerialDevice) {
        this.usbSerialDevice = usbSerialDevice;
    }
}
