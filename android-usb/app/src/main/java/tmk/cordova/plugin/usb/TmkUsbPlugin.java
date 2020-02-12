package tmk.cordova.plugin.usb;


import android.content.Context;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
    private Gson gson;

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

    UsbDevice usbDevice;

    UsbDeviceConnection connection;


    public void setUsbSerialDevice(final UsbSerialDevice usbSerialDevice) {
        this.usbSerialDevice = usbSerialDevice;
    }

    public void setUsbDevice(UsbDevice usbDevice) {
        this.usbDevice = usbDevice;
    }

    public void setConnection(UsbDeviceConnection connection) {
        this.connection = connection;
    }

    Context context;
    UsbManager usbManager;

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

        sendOkMsgToGui(s);
    };

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
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        try {
            this.gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();

            this.context = cordova.getContext();

            this.usbManager = (UsbManager) context.getSystemService(USB_SERVICE);

            this.tmkUsbGui = new TmkUsbGui();

            this.tmkUsbConfig = new TmkUsbConfig();
        } catch (Throwable t) {
            sendErrMsgToGui("cannot initialize: t = " + t.getMessage());
        }
    }

    private void init() {
        try {
            tmkUsbBroadcastReceiver = new TmkUsbBroadcastReceiver(
                    this,
                    usbManager,
                    tmkUsbConfig);

            registerReceiver(context, tmkUsbBroadcastReceiver);

            tmkUsbBroadcastReceiver.listDevicesAndFindProperOne(context, usbManager);
        } catch (Throwable t) {
            sendErrMsgToGui("cannot init: t = " + t.getMessage());
        }
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            if (this.cordova != null && this.cordova.getContext() != null) {
                this.cordova.getContext()
                        .unregisterReceiver(tmkUsbBroadcastReceiver);
            }

            if (usbSerialDevice != null) {
                usbSerialDevice.close();
            }
        } catch (Throwable t) {
            sendErrMsgToGui("cannot onDestroy: t = " + t.getMessage());
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
                case "connect":
                    this.callbackContext = tmkUsbGui.connectWithGui(callbackContext);
                    return true;
                case "init":
                    this.init();
                    callbackContext.success("initialized");
                    return true;
                case "write":
                    writeToTheUsbSerialDevice(data);
                    callbackContext.success("written");
                    return true;
                case "resetConfig":
                    tmkUsbConfig.resetToDefaults();
                    sendOkMsgToGui("config set to default");
                    return true;
                case "getConfig":
                    return sendConfig(callbackContext);
                case "getLogs":
                    return sendLogs(callbackContext);
                case "reset":
                    return reset(callbackContext);
                default:
                    throw new TmkUsbException("Unsupported action: " + action);
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

    private boolean writeToTheUsbSerialDevice(
            final JSONArray data)
            throws JSONException, TmkUsbException {

        if (usbSerialDevice == null
                || !usbSerialDevice.isOpen()) {
            throw new TmkUsbException("Cannot write to the usb device - is null or not opened");
        }

        String s = data.getString(0) + " c=" + count++ + tmkUsbConfig.getEndLine();

        cordova.getThreadPool().execute(() -> {
            try {
                usbSerialDevice.write(s.getBytes());

                sendOkMsgToGui("passed to write: s = " + s);
            } catch (Throwable t) {
                String msg = "cannot write: " + t.getMessage();
                logtmk(msg);
                sendErrMsgToGui(msg);
            }
        });

        return true;
    }

    private boolean sendConfig(final CallbackContext callbackContext) {
        String device = UsbHelper.readDevice(usbDevice);

        DeviceDescriptor deviceDescriptor =
                DeviceDescriptor.fromDeviceConnection(connection);

        callbackContext.success(
                gson.toJson(this.tmkUsbConfig)
                        + "\r\n" + gson.toJson(deviceDescriptor)
                        + "\r\n" + device);

        return true;
    }

    private boolean sendLogs(final CallbackContext callbackContext) {
        callbackContext.success(gson.toJson(getLogs()));
        return true;
    }

    private boolean reset(final CallbackContext callbackContext) {
        clearLogs();
        this.count = 1;
        callbackContext.success("cleared");
        return true;
    }

    private void registerReceiver(
            final Context context,
            final TmkUsbBroadcastReceiver tmkUsbBroadcastReceiver) {
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
