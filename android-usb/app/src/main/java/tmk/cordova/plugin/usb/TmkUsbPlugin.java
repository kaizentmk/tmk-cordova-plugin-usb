package tmk.cordova.plugin.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import tmk.cordova.plugin.usb.device.TmkUsbBroadcastReceiver;
import tmk.cordova.plugin.usb.device.TmkUsbConfig;
import tmk.cordova.plugin.usb.device.TmkUsbDevice;

import static android.content.Context.USB_SERVICE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static tmk.cordova.plugin.usb.TmkUsbLogging.getTime;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;

/**
 * https://cordova.apache.org/docs/en/9.x/guide/platforms/android/plugin.html
 * <p>
 * Plugins should use the initialize method for their start-up logic.
 * </p>
 */
public class TmkUsbPlugin extends CordovaPlugin {

    public static final String TAG = "drinker";

    public static final String DEVICE_CONNECTING_MSG = "tmk.usb.plugin.device.connecting";
    public static final String DEVICE_WRITING_MSG = "tmk.usb.plugin.device.writing";
    public static final String ACTION_ERR_MSG = "tmk.usb.plugin.action.unsupported";

    private Gson gson;

    /**
     * For keeping connection with the gui.
     * Used in the TmkUsbBroadcastReceiver.
     * Sends data from usb to the gui.
     */
    CallbackContext callbackContext;

    TmkUsbGui tmkUsbGui;
    TmkUsbDevice tmkUsbDevice;
    TmkUsbBroadcastReceiver tmkUsbBroadcastReceiver;

    UsbSerialInterface.UsbReadCallback readCallback = data -> {
        String s = ("" + new String(data, UTF_8)).trim();
        if (s.isEmpty()) {
            return;
        }

        sendOkMsgToGui(s);
    };

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {

        super.initialize(cordova, webView);

        try {
            this.gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            UsbManager usbManager = (UsbManager) cordova.getContext()
                    .getSystemService(USB_SERVICE);

            this.tmkUsbGui = TmkUsbGui.INSTANCE;
            this.tmkUsbDevice =
                    new TmkUsbDevice(cordova, usbManager, TmkUsbConfig.INSTANCE, readCallback);

            this.tmkUsbBroadcastReceiver =
                    new TmkUsbBroadcastReceiver(usbManager, this.tmkUsbDevice);
            this.tmkUsbBroadcastReceiver.register(cordova.getContext());

        } catch (Throwable t) {
            sendErrMsgToGui("cannot initialize: t = " + t.getMessage());
        }
    }

    @Override
    public boolean execute(final String action,
                           final JSONArray data,
                           final CallbackContext callbackContext)
            throws JSONException {

        sendOkMsgToGui("executing: action = " + action);

        try {
            switch (action) {
                case "connectGui":
                    this.callbackContext = tmkUsbGui.connectWithGui(callbackContext);
                    return true;
                case "connectDevice":
                    callbackContext.success(DEVICE_CONNECTING_MSG);
                    UsbDevice device = this.tmkUsbDevice.listDevicesAndFindProperOne();
                    tmkUsbBroadcastReceiver.requestPermission(cordova.getContext(), device);
                    return true;
                case "write":
                    callbackContext.success(DEVICE_WRITING_MSG);
                    tmkUsbDevice.write(data.getString(0));
                    return true;
//                case "resetConfig":
//                    tmkUsbConfig.resetToDefaults();
//                    sendOkMsgToGui("config set to default");
//                    return true;
//                case "getConfig":
//                    return sendConfig(callbackContext);
//                case "getLogs":
//                    return sendLogs(callbackContext);
//                case "reset":
//                    return reset(callbackContext);
                default:
                    throw new TmkUsbException(ACTION_ERR_MSG + ":" + action);
            }
        } catch (Throwable t) {
            String msg = "TmkUsbPluginError: " + t.getMessage();
            if (callbackContext != null) {
                callbackContext.error(msg);
            }
            sendErrMsgToGui(msg);

            return false;
        }
    }

//
//
//    private boolean sendConfig(final CallbackContext callbackContext) {
//        String device = UsbHelper.readDevice(usbDevice);
//
//        DeviceDescriptor deviceDescriptor =
//                DeviceDescriptor.fromDeviceConnection(connection);
//
//        callbackContext.success(
//                gson.toJson(this.tmkUsbConfig)
//                        + "\r\n" + gson.toJson(deviceDescriptor)
//                        + "\r\n" + device);
//
//        return true;
//    }
//
//    private boolean sendLogs(final CallbackContext callbackContext) {
//        callbackContext.success(gson.toJson(getLogs()));
//        return true;
//    }
//
//    private boolean reset(final CallbackContext callbackContext) {
//        clearLogs();
//        this.count = 1;
//        callbackContext.success("cleared");
//        return true;
//    }


    public void sendOkMsgToGui(final String s) {
        if (this.callbackContext == null) {
            logtmk("sendOkMsgToGui: callbackContext is null");
            return;
        }

        this.callbackContext.sendPluginResult(
                this.tmkUsbGui.makeOkKeepPluginResult(
                        getTime() + ": [[" + s + "]]"));
    }

    private void sendErrMsgToGui(String msg) {
        if (this.callbackContext == null) {
            logtmk("sendErrMsgToGui: callbackContext is null");
            return;
        }

        this.callbackContext.sendPluginResult(
                this.tmkUsbGui.makeErrorKeepPluginResult(msg));
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            if (this.cordova != null && this.cordova.getContext() != null) {
                this.cordova.getContext()
                        .unregisterReceiver(tmkUsbBroadcastReceiver);
            }

            tmkUsbDevice.onDestroy();
        } catch (Throwable t) {
            sendErrMsgToGui("cannot onDestroy: t = " + t.getMessage());
        }
    }

}
