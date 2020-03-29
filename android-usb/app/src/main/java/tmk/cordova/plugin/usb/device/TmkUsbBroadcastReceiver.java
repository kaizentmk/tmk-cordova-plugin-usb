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
import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;

public class TmkUsbBroadcastReceiver extends BroadcastReceiver {

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
        if (!tmkUsbDevice.isDeviceProperOne(device)) {
            return;
        }

        try {
            switch (intent.getAction()) {
                case ACTION_USB_DEVICE_ATTACHED:
                    requestPermission(context, device);
                    break;

                case ACTION_USB_PERMISSION:
                    Boolean granted = intent.getParcelableExtra(UsbManager.EXTRA_PERMISSION_GRANTED);
                    if (granted) {
                        tmkUsbDevice.connect(device);
                    } else {
                        logtmk("permission not granted");
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
                    + intent.getAction()
                    + t.getMessage();
            Log.e(TAG, msg);
            logtmk(msg);
        }
    }

    public void requestPermission(
            final Context context,
            final UsbDevice device) {

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(ACTION_USB_PERMISSION),
                0);

        usbManager.requestPermission(device, permissionIntent);
    }

    public void register(final Context context) {

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_DEVICE_ATTACHED));

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_DEVICE_DETACHED));

        context.registerReceiver(
                this,
                new IntentFilter(ACTION_USB_PERMISSION));
    }
}
