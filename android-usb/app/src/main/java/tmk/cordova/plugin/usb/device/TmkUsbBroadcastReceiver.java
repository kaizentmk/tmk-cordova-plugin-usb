package tmk.cordova.plugin.usb.device;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Parcelable;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;

import java.io.PrintWriter;
import java.io.StringWriter;

import tmk.cordova.plugin.usb.TmkUsbPlugin;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmkerr;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;

public class TmkUsbBroadcastReceiver extends BroadcastReceiver {

    public static final String tag = "tubr::";

    public static final String ACTION_USB_PERMISSION =
            "tmk.cordova.plugin.usb.device.USB_PERMISSION";

    private UsbSerialDevice usbSerialDevice;

    final TmkUsbPlugin tmkUsbPlugin;
    final UsbManager usbManager;
    final TmkUsbDevice tmkUsbDevice;

    public TmkUsbBroadcastReceiver(
            final TmkUsbPlugin tmkUsbPlugin,
            final UsbManager usbManager,
            final TmkUsbDevice tmkUsbDevice) {
        this.usbManager = usbManager;
        this.tmkUsbDevice = tmkUsbDevice;
        this.tmkUsbPlugin = tmkUsbPlugin;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        logtmk(tag, "onReceive: device: ",
                device.getManufacturerName(),
                device.getDeviceName(),
                "" + device.getVendorId(),
                "" + device.getProductId(),
                "" + device.getConfiguration(0));

        logtmk(tag, "onReceive: usbSerialDevice: ",
                "" + usbSerialDevice);

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
                    if (usbSerialDevice != null) {
                        return;
                    }

                    Parcelable granted = intent.getParcelableExtra(
                            UsbManager.EXTRA_PERMISSION_GRANTED);
                    logtmk(tag, "onReceive: granted = ",
                            "" + (granted == null ? null : granted.getClass().getName()));
                    logtmk(tag, "onReceive: granted = ", "" + granted);

                    tmkUsbPlugin.sendOkMsgToGui("connecting", "device");
                    usbSerialDevice = tmkUsbDevice.connect(device);
                    tmkUsbPlugin.sendOkMsgToGui("connected", "device");
                    break;

                case ACTION_USB_DEVICE_DETACHED:
                    usbSerialDevice = null;
                    tmkUsbPlugin.sendOkMsgToGui("detached", "device");
                    tmkUsbDevice.onDestroy();
                    break;

                default:
                    //nope
            }
        } catch (final Throwable t) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String stackTraceStr = sw.toString();

            String msg = "Cannot handle: intent.action = "
                    + action
                    + " " + t.getMessage()
                    + " " + stackTraceStr;
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

        logtmk(tag, "register: end");
    }
}
