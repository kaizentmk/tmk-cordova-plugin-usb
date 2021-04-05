package tmk.cordova.plugin.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Objects;

import tmk.cordova.plugin.usb.device.TmkDeviceUsbException;
import tmk.cordova.plugin.usb.device.TmkUsbService;
import tmk.cordova.plugin.usb.gui.TmkUsbGui;

import static android.content.Context.USB_SERVICE;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static tmk.cordova.plugin.usb.device.TmkUsbDevice.ACTION_USB_DEVICE_ERROR;
import static tmk.cordova.plugin.usb.device.TmkUsbDeviceConnection.ACTION_USB_DEVICE_CONNECTED;
import static tmk.cordova.plugin.usb.device.TmkUsbDevicePermission.ACTION_USB_PERMISSION_NOT_GRANTED;
import static tmk.cordova.plugin.usb.device.TmkUsbService.ACTION_USB_DEVICE_FIND;
import static tmk.cordova.plugin.usb.device.TmkUsbService.DEVICE_DOMAIN_CONNECTED;
import static tmk.cordova.plugin.usb.device.TmkUsbService.DEVICE_DOMAIN_CONNECTING;
import static tmk.cordova.plugin.usb.device.TmkUsbService.DEVICE_DOMAIN_DATA;
import static tmk.cordova.plugin.usb.device.TmkUsbService.DEVICE_DOMAIN_DETACHED;
import static tmk.cordova.plugin.usb.device.TmkUsbService.DEVICE_DOMAIN_ERROR;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.LOG_DOMAIN;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.clearLogs;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.getLogs;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmkerr;

/**
 * https://cordova.apache.org/docs/en/9.x/guide/platforms/android/plugin.html
 * <p>
 * Plugins should use the initialize method for their start-up logic.
 * </p>
 */
public class TmkUsbPlugin extends CordovaPlugin {

    public static final String tag = "tup";
    public static final String PLUGIN_DOMAIN = "plugin";

    public static final String ACTION_CONNECT_GUI = "connectGui";
    public static final String ACTION_CONNECT_DEVICE = "connectDevice";
    public static final String ACTION_WRITE = "write";
    public static final String ACTION_DISPATCH = "dispatch";
    public static final String ACTION_DISPATCH_GET_LOGS = "getLogs";
    public static final String ACTION_DISPATCH_CLEAR_LOGS = "clearLogs";

    private Context context;
    private TmkUsbGui tmkUsbGui;
    private TmkUsbService tmkUsbService;
    private Gson gson;

    private UsbSerialInterface.UsbReadCallback readCallback = data -> {
        try {
            String s = new String(data, "UTF-8");
            if (s == null) {
                String msg = "data from device is null";
                this.tmkUsbGui.sendErrMsg(DEVICE_DOMAIN_ERROR, msg,
                        new TmkDeviceUsbException(msg));
            }

            logtmk(tag, "readCallback", " s = " + s);
            this.tmkUsbGui.sendOkMsg(DEVICE_DOMAIN_DATA, s);
        } catch (UnsupportedEncodingException e) {
            logtmkerr(tag, "cannot read data from USB ",
                    e.getMessage(), Arrays.toString(e.getStackTrace()));
            context.sendBroadcast(new Intent(ACTION_USB_DEVICE_ERROR)
                    .putExtra(ACTION_USB_DEVICE_ERROR,
                            new TmkDeviceUsbException("Cannot read data from usb", e)));
        }
    };

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            String action = intent.getAction();
            logtmk(tag, "onReceive: action = " + action);

            try {
                if (ACTION_USB_DEVICE_CONNECTED.equals(action)) {
                    tmkUsbGui.sendOkMsg(DEVICE_DOMAIN_CONNECTED, "connected");
                }

                if (ACTION_USB_PERMISSION_NOT_GRANTED.equals(action)) {
                    throw new TmkUsbException("ACTION_USB_PERMISSION_NOT_GRANTED");
                }

                if (ACTION_USB_DEVICE_DETACHED.equals(action)) {
                    tmkUsbGui.sendOkMsg(DEVICE_DOMAIN_DETACHED, "detached");
                }

                if (ACTION_USB_DEVICE_ERROR.equals(action)) {
                    throw (TmkDeviceUsbException) Objects.requireNonNull(intent.getExtras()
                            .get(ACTION_USB_DEVICE_ERROR));
                }
            } catch (final Throwable t) {
                String msg = "onReceive error: action = " + action;
                logtmkerr(tag, msg, t);
                tmkUsbGui.sendErrMsg(DEVICE_DOMAIN_ERROR, msg, t);
            }
        }
    };

    @Override
    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {

        try {
            logtmk(tag, "initialize: start");

            super.initialize(cordova, webView);
            this.gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .disableHtmlEscaping()
                    .create();
            this.tmkUsbGui = new TmkUsbGui(gson);

            this.context = cordova.getContext();

            UsbManager usbManager = (UsbManager) context.getSystemService(USB_SERVICE);

            this.tmkUsbService = new TmkUsbService(
                    context,
                    usbManager,
                    readCallback);

            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_USB_DEVICE_CONNECTED);
            filter.addAction(ACTION_USB_DEVICE_DETACHED);
            filter.addAction(ACTION_USB_PERMISSION_NOT_GRANTED);
            filter.addAction(ACTION_USB_DEVICE_ERROR);
            context.registerReceiver(broadcastReceiver, filter);

            logtmk(tag, "initialize: end");
        } catch (Throwable t) {
            logtmkerr(tag, "initialize: error: ", t);
        }
    }

    @Override
    public boolean execute(
            final String action,
            final JSONArray data,
            final CallbackContext clbckCtx) {

        try {
            logtmk(tag, "execute: start", " action = " + action, " data = " + data);

            switch (action) {

                case ACTION_CONNECT_GUI:
                    tmkUsbGui.connectWithGui(clbckCtx);
                    logtmk(tag, "execute: end", " action = " + action);
                    return true;

                case ACTION_CONNECT_DEVICE:
                    clbckCtx.success(tmkUsbGui.msg(DEVICE_DOMAIN_CONNECTING, "connecting"));

                    if (tmkUsbService.isSerialPortConnected()) {
                        tmkUsbGui.sendOkMsg(DEVICE_DOMAIN_CONNECTED, "connected");
                    } else {
                        context.sendBroadcast(new Intent(ACTION_USB_DEVICE_FIND));
                    }

                    logtmk(tag, "execute: end", " action = " + action);
                    return true;

                case ACTION_WRITE:
                    tmkUsbService.write(data.getString(0).getBytes());
                    return true;

                case ACTION_DISPATCH:
                    return dispatch(data, clbckCtx);

                default:
                    throw new TmkUsbException("action not supported: " + action);

            }
        } catch (Throwable t) {
            handleError(clbckCtx, "execute: action = " + action, t);
            return false;
        }
    }

    private void handleError(
            final CallbackContext clbckCtx,
            final String msg,
            final Throwable t) {
        logtmkerr(tag, msg, t);
        clbckCtx.error(tmkUsbGui.msg(PLUGIN_DOMAIN, msg, t));
    }

    private boolean dispatch(
            final JSONArray data,
            final CallbackContext clbckCtx)
            throws JSONException {

        String cmdName = data.getString(0);

        try {
            logtmk(tag, "dispatch: start", " cmdName = " + cmdName);

            switch (cmdName) {
                case ACTION_DISPATCH_GET_LOGS:
                    clbckCtx.success(tmkUsbGui.msg(LOG_DOMAIN, "sending"));
                    handleDispatchCmdResp(cmdName, LOG_DOMAIN, gson.toJson(getLogs()));
                    return true;

                case ACTION_DISPATCH_CLEAR_LOGS:
                    clbckCtx.success(tmkUsbGui.msg(LOG_DOMAIN, "clearing"));
                    clearLogs();
                    handleDispatchCmdResp(cmdName, LOG_DOMAIN, "cleared");
                    return true;

                default:
                    throw new TmkUsbException("dispatch command not supported: cmdName =" + cmdName);
            }

        } catch (Throwable t) {
            handleError(clbckCtx, "dispatch: cmdName = " + cmdName, t);
            return false;
        }
    }

    private void handleDispatchCmdResp(final String cmdName,
                                       final String domain,
                                       final String cmdResult) {
        tmkUsbGui.sendOkMsg(domain, cmdResult);
        logtmk(tag, "dispatch: end", " cmdName = " + cmdName);
    }

    @Override
    public void onDestroy() {
        try {
            super.onDestroy();
            if (this.cordova != null && this.cordova.getContext() != null) {
                this.cordova.getContext()
                        .unregisterReceiver(broadcastReceiver);
            }

            tmkUsbService.onDestroy();
        } catch (Throwable t) {
            String msg = "cannot onDestroy: t = " + t.getMessage();
            logtmkerr(tag, msg, Arrays.toString(t.getStackTrace()));
            tmkUsbGui.sendErrMsg(PLUGIN_DOMAIN, msg, t);
        }
    }
}


//    public static final String TAG = "drinker";
//    public static final String tag = "tup::";
//
//    private Gson gson;
//
//    private CordovaInterface cordova;
//
//    /**
//     * For keeping connection with the gui.
//     * Used in the TmkUsbBroadcastReceiver.
//     * Sends data from usb to the gui.
//     */
//    CallbackContext callbackContext;
//
//    TmkUsbGui tmkUsbGui;
//    TmkUsbDevice tmkUsbDevice;
//    TmkUsbBroadcastReceiver tmkUsbBroadcastReceiver;
//
//    UsbSerialInterface.UsbReadCallback readCallback = data -> {
//        try {
//            String s = new String(data, "UTF-8");
//            sendOkMsgToGui(s, "device.read");
//        } catch (UnsupportedEncodingException e) {
//            logtmk(tag, "cannot read data from USB ",
//                    e.getMessage(), Arrays.toString(e.getStackTrace()));
//        }
//    };
//
//    @Override
//    public void initialize(final CordovaInterface cordova, final CordovaWebView webView) {
//        logtmk(tag, "initialize: start");
//
//        super.initialize(cordova, webView);
//        this.cordova = cordova;
//
//        try {
//            this.gson = new GsonBuilder()
//                    .setPrettyPrinting()
//                    .create();
//
//            UsbManager usbManager = (UsbManager) cordova.getContext()
//                    .getSystemService(USB_SERVICE);
//
//            this.tmkUsbGui = TmkUsbGui.INSTANCE;
//            this.tmkUsbDevice =
//                    new TmkUsbDevice(
//                            cordova,
//                            usbManager,
//                            TmkUsbDeviceConfig.INSTANCE,
//                            readCallback);
//
//            this.tmkUsbBroadcastReceiver =
//                    new TmkUsbBroadcastReceiver(
//                            this,
//                            usbManager,
//                            this.tmkUsbDevice);
//            this.tmkUsbBroadcastReceiver.register(cordova.getContext());
//
//            logtmk(tag, "initialize: end");
//        } catch (Throwable t) {
//            logtmkerr(tag, "initialize: error: ",
//                    t.getMessage(), Arrays.toString(t.getStackTrace()));
//        }
//    }
//
//    @Override
//    public boolean execute(final String action,
//                           final JSONArray data,
//                           final CallbackContext guiClbckCtx)
//            throws JSONException {
//
//        logtmk(tag, "execute: start: action: " + action, " , data: " + data);
//
//        try {
//            switch (action) {
//                case "connectGui":
//                    this.callbackContext = tmkUsbGui.connectWithGui(guiClbckCtx);
//                    logtmk(tag, "execute: end: action: " + action);
//                    return true;
//                case "connectDevice":
//                    guiClbckCtx.success(
//                            this.tmkUsbGui.msg("device", "connecting"));
//                    UsbDevice device = null;
//                    try {
//                        device = this.tmkUsbDevice.listDevicesAndFindProperOne();
//                    } catch (final TmkUsbDeviceNotFoundException e) {
//                        guiClbckCtx.success(
//                                this.tmkUsbGui.msg("device", "not_found"));
//                        logtmk(tag, "execute: end: action: " + action);
//                        return true;
//                    }
//                    tmkUsbBroadcastReceiver.requestPermission(cordova.getContext(), device);
//                    logtmk(tag, "execute: end: action: " + action);
//                    return true;
//                case "write":
//                    guiClbckCtx.success(
//                            this.tmkUsbGui.msg("device", "writing"));
//                    tmkUsbDevice.write(data.getString(0));
//                    logtmk(tag, "execute: end: action: " + action);
//                    return true;
//                case "dispatch":
//                    return dispatch(data, guiClbckCtx);
//                default:
//                    throw new TmkUsbException("action not supported: " + action);
//            }
//        } catch (Throwable t) {
//            StringWriter sw = new StringWriter();
//            t.printStackTrace(new PrintWriter(sw));
//            String stackTraceStr = sw.toString();
//
//            String msg = "execute: error: action = " + action + ", " + stackTraceStr;
//            logtmkerr(tag, msg, Arrays.toString(t.getStackTrace()));
//
//            if (guiClbckCtx != null) {
//                guiClbckCtx.error(msg);
//            }
//
//            sendErrMsgToGui(msg, "plugin.execute.error");
//
//            return false;
//        }
//    }
//
//    private boolean dispatch(JSONArray data, CallbackContext callbackContext)
//            throws JSONException {
//
//        String name = data.getString(0);
//        logtmk(tag, "dispatch: start: name = ", name);
//
//        switch (name) {
//            case "getLogs":
//                logtmk(tag, "logs.sending");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("plugin.logs", "sending"));
//                sendOkMsgToGui("plugin.logs", gson.toJson(getLogs()));
//                logtmk(tag, "logs.sent");
//                return true;
//            case "clearLogs":
//                logtmk(tag, "logs.clearing");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("plugin.logs.clearing", "pending"));
//                TmkUsbLogging.clearLogs();
//                sendOkMsgToGui("cleared", "plugin.logs");
//                logtmk(tag, "logs.cleared");
//                return true;
//            case "getUsbDeviceInfo":
//                logtmk(tag, "device.info.getting");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("device.info.send", "pending"));
//                UsbDeviceConnection connection = tmkUsbDevice.getConnection();
//                if (connection != null) {
//                    sendOkMsgToGui(
//                            DeviceDescriptor.fromDeviceConnection(connection).toString(),
//                            "device.info");
//                    logtmk(tag, "device.info.sent");
//                    return true;
//                }
//
//                sendErrMsgToGui("connection.absence", "device.info.error");
//                logtmk(tag, "device.info.error", "connection.absence");
//                return false;
//
//            case "getUsbDeviceConfig":
//                logtmk(tag, "device.config.getting");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("device.config", "pending"));
//                sendOkMsgToGui(
//                        gson.toJson(TmkUsbDeviceConfig.INSTANCE),
//                        "device.config");
//                logtmk(tag, "device.config.sent");
//                return true;
//
//            case "kioskOn":
//                logtmk(tag, "kiosk.on.enabling");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("kiosk.on", "pending"));
//                cordova.getActivity().startLockTask();
//                sendOkMsgToGui(
//                        gson.toJson(TmkUsbDeviceConfig.INSTANCE),
//                        "kiosk.on.enabled");
//                logtmk(tag, "kiosk.on.sent");
//                return true;
//
//            case "kioskOff":
//                logtmk(tag, "kiosk.off.enabling");
//                callbackContext.success(
//                        this.tmkUsbGui.msg("kiosk.off", "pending"));
//                cordova.getActivity().stopLockTask();
//                sendOkMsgToGui(
//                        gson.toJson(TmkUsbDeviceConfig.INSTANCE),
//                        "kiosk.off.enabled");
//                logtmk(tag, "kiosk.off.sent");
//                return true;
//        }
//
//        String msg = "dispatch.unsupported: name = " + name;
//        callbackContext.error(msg);
//        sendErrMsgToGui(msg, "dispatch");
//        logtmkerr(tag, msg);
//        return false;
//    }
//
//    public void sendOkMsgToGui(final String msg, final String type) {
//        if (this.callbackContext == null) {
//            logtmk(tag, "sendOkMsgToGui: callbackContext is null");
//            return;
//        }
//
//        this.callbackContext.sendPluginResult(
//                this.tmkUsbGui.makeOkKeepPluginResult(
//                        this.tmkUsbGui.msg(type, msg)));
//    }
//
//    private void sendErrMsgToGui(final String msg, final String type) {
//        if (this.callbackContext == null) {
//            logtmkerr(tag, "sendErrMsgToGui: callbackContext is null");
//            return;
//        }
//
//        this.callbackContext.sendPluginResult(
//                this.tmkUsbGui.makeErrorKeepPluginResult(
//                        this.tmkUsbGui.msg(type, msg)));
//    }
//
//
//    @Override
//    public void onDestroy() {
//        try {
//            super.onDestroy();
//            if (this.cordova != null && this.cordova.getContext() != null) {
//                this.cordova.getContext()
//                        .unregisterReceiver(tmkUsbBroadcastReceiver);
//            }
//
//            tmkUsbDevice.onDestroy();
//        } catch (Throwable t) {
//            String msg = "cannot onDestroy: t = " + t.getMessage();
//            logtmkerr(tag, msg, Arrays.toString(t.getStackTrace()));
//            sendErrMsgToGui(msg, "destroy.error");
//        }
//    }
//}
