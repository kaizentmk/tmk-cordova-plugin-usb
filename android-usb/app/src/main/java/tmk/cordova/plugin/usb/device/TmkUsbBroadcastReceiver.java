package tmk.cordova.plugin.usb.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmkerr;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;

public class TmkUsbBroadcastReceiver extends BroadcastReceiver {

    public static final String tag = "tubr::";

    public static final String ACTION_USB_PERMISSION =
            "tmk.cordova.plugin.usb.USB_PERMISSION";

    final UsbManager usbManager;
    final TmkUsbDevice tmkUsbDevice;

    public TmkUsbBroadcastReceiver(
            final UsbManager usbManager,
            final TmkUsbDevice tmkUsbDevice) {
        this.usbManager = usbManager;
        this.tmkUsbDevice = tmkUsbDevice;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        logtmk(tag, "onReceive: device: ",
                device.getManufacturerName(), device.getDeviceName());

        if (!tmkUsbDevice.isDeviceProperOne(device)) {
            return;
        }

        String action = intent.getAction();
        logtmk(tag, "onReceive: action = ", action);

        try {
            switch (action) {
                case ACTION_USB_DEVICE_ATTACHED:
                    requestPermission(context, device);
                    break;

                case ACTION_USB_PERMISSION:
                    Boolean granted = intent.getParcelableExtra(UsbManager.EXTRA_PERMISSION_GRANTED);
                    logtmk(tag, "onReceive: granted = ", "" + granted);

                    if (granted) {
                        tmkUsbDevice.connect(device);
                    } else {
                        requestPermission(context, device);
                    }
                    break;

                case ACTION_USB_DEVICE_DETACHED:
                    tmkUsbDevice.onDestroy();
                    break;

                default:
                    //nope
            }
        } catch (final Throwable t) {
            String msg = "Cannot handle: intent.action = "
                    + action
                    + t.getMessage();
            Log.e(TAG, msg);
            logtmkerr(tag, msg);
        }
    }

    public void requestPermission(
            final Context context,
            final UsbDevice device) {

        logtmk(tag, "requestPermission: start");

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(ACTION_USB_PERMISSION),
                0);

        usbManager.requestPermission(device, permissionIntent);

        logtmk(tag, "requestPermission: end");
    }

    public void register(final Context context) {

        logtmk(tag, "register: start");

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_DEVICE_ATTACHED));

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_DEVICE_DETACHED));

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_PERMISSION));

        logtmk(tag, "register: start");
    }
}
