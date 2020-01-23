package tmk.cordova.plugin.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;

/**
 * https://cordova.apache.org/docs/en/9.x/guide/platforms/android/plugin.html
 * <p>
 * Plugins should use the initialize method for their start-up logic.
 */
public class TmkUsb extends CordovaPlugin {

    private static final String TAG = "TmkUsb";

    private static final String ACTION_USB_PERMISSION =
            "tmk.cordova.plugin.usb.USB_PERMISSION";

    private int count = 0;
    private String mStatusView = "";
    private String mResultView = "";

    private List<String> tmklogbuff = new ArrayList<>();

    private UsbDevice usbDevice;

    /* USB system service */
    private UsbManager mUsbManager;

    /**
     * Broadcast receiver to handle USB disconnect events.
     */
    BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            tmklogbuff.add("mUsbReceiver: action = " + action);

            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            //call method to set up device communication
                        }
                    } else {
                        tmklogbuff.add("permission denied for device " + device);
                        Log.d(TAG, "permission denied for device " + device);
                    }
                }
            }

            if (ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    usbDevice = device;
                    printStatus("Device Attached");
                    printDeviceDescription(device);

                    PendingIntent permissionIntent = PendingIntent.getBroadcast(context, 0,
                            new Intent(ACTION_USB_PERMISSION), 0);
                    mUsbManager.requestPermission(device, permissionIntent);
                }
            }

            if (ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    printStatus("Device Detached");
                    printDeviceDescription(device);

                    // call your method that cleans up and closes communication with the device

                }
            }
        }
    };

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        reset();

        tmklogbuff.add("initialize: start");

        Context context = cordova.getContext();

        context.registerReceiver(this.mUsbReceiver,
                new IntentFilter(ACTION_USB_DEVICE_ATTACHED));

        // Detach events are sent as a system-wide broadcast
        context.registerReceiver(this.mUsbReceiver,
                new IntentFilter(ACTION_USB_DEVICE_DETACHED));

        context.registerReceiver(this.mUsbReceiver,
                new IntentFilter(ACTION_USB_PERMISSION));

//        mUsbManager = context.getSystemService(UsbManager.class);
        mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

        tmklogbuff.add("initialize: end");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.cordova != null && this.cordova.getContext() != null) {
            this.cordova.getContext().unregisterReceiver(mUsbReceiver);
        }
    }

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
                case "reset":
                    return reset();
                case "connect":
                    return connect();
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
        jo.put("mStatusView", mStatusView);
        jo.put("mResultView", mResultView);
        jo.put("tmklogbuff", new JSONArray(tmklogbuff));
        callbackContext.success(jo.toString(2));
        return true;
    }

    private boolean listDevices(JSONArray data, CallbackContext callbackContext) throws JSONException {

        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();

        JSONObject jo = new JSONObject();

        for (Map.Entry<String, UsbDevice> kv : deviceList.entrySet()) {
            jo.put(kv.getValue().getDeviceName(), printDeviceDescription(kv.getValue()));
        }

        callbackContext.success(jo.toString(2));
        return true;
    }

    private boolean reset() {
        mStatusView = "";
        mResultView = "";
        tmklogbuff = new ArrayList<>();
        count = 0;
        return true;
    }

    private boolean connect() {
        try {
            byte[] bytes = "T tmk".getBytes();
            int TIMEOUT = 0;
            boolean forceClaim = true;

            tmklogbuff.add("sending data device = " + usbDevice);
            if (usbDevice != null) {
//
                UsbInterface intf = usbDevice.getInterface(0);
                UsbEndpoint endpoint = intf.getEndpoint(0);
                UsbDeviceConnection connection = mUsbManager.openDevice(usbDevice);
                tmklogbuff.add("connection = " + connection);

                if (connection != null) {
                    connection.claimInterface(intf, forceClaim);

                    connection.bulkTransfer(endpoint, bytes, bytes.length, TIMEOUT); //do in another thread
                }
            }
        } catch (Throwable t) {
            tmklogbuff.add("Cannot connect to the usb device: " + t.getMessage());
        }

        return true;
    }

    /**
     * Print a basic description about a specific USB device.
     *
     * @param device USB device to query.
     */
    private String printDeviceDescription(UsbDevice device) {
        String result = UsbHelper.readDevice(device) + "\n\n";
        printResult(result);
        return result;
    }

    /* Helpers to display user content */

    private void printStatus(String status) {
        mStatusView = status;
        Log.i(TAG, status);
    }

    private void printResult(String result) {
        mResultView = result;
        Log.i(TAG, result);
    }
}
