package tmk.cordova.plugin.usb.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static tmk.cordova.plugin.usb.device.TmkUsbDevice.TMK_USB_DEVICE_NAME;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmkerr;


public class TmkUsbDevicePermission extends BroadcastReceiver {

    public static final String tag = "tudp";

    public static final String ACTION_USB_PERMISSION = "tmk.cordova.plugin.usb.device.USB_PERMISSION";
    public static final String ACTION_USB_PERMISSION_GRANTED = "tmk.cordova.plugin.usb.device.USB_PERMISSION_GRANTED";
    public static final String ACTION_USB_PERMISSION_NOT_GRANTED = "tmk.cordova.plugin.usb.device.USB_PERMISSION_NOT_GRANTED";

    private final UsbManager usbManager;

    public TmkUsbDevicePermission(
            final Context context,
            final UsbManager usbManager) {

        logtmk(tag, "TmkUsbBroadcastReceiver: start");

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_PERMISSION);
        context.registerReceiver(this, filter);

        this.usbManager = usbManager;

        logtmk(tag, "TmkUsbBroadcastReceiver: end");
    }

    @Override
    public void onReceive(
            final Context context,
            final Intent intent) {

        String action = intent.getAction();
        logtmk(tag, "onReceive: action = " + action);

        try {
            if (ACTION_USB_PERMISSION.equals(action)) {
                handlePermission(context, intent);
            }
        } catch (final Throwable t) {
            logtmkerr(tag, "onReceive error: action = " + action, t);
        }
    }

    public void requestUserPermission(
            final Context context,
            final UsbDevice device) {

        logtmk(tag, "requestUserPermission: start");

        PendingIntent usbPermissionIntent =
                PendingIntent.getBroadcast(context, 0,
                        new Intent(ACTION_USB_PERMISSION)
                                .putExtra(TMK_USB_DEVICE_NAME, device),
                        0);
        usbManager.requestPermission(device, usbPermissionIntent);
        logtmk(tag, "requestUserPermission: end");
    }

    private void handlePermission(
            final Context context,
            final Intent intent) {

        logtmk(tag, "handlePermission: start");

        boolean granted = intent.getExtras()
                .getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
        logtmk(tag, "handlePermission: granted = " + granted);

        context.sendBroadcast(new Intent(
                granted
                        ? ACTION_USB_PERMISSION_GRANTED
                        : ACTION_USB_PERMISSION_NOT_GRANTED)
                .putExtra(
                        TMK_USB_DEVICE_NAME,
                        getDevice(intent)));

        logtmk(tag, "handlePermission: end");
    }

    private UsbDevice getDevice(final Intent intent) {
        return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
    }

}
