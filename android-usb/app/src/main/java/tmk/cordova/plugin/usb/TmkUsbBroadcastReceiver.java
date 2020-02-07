package tmk.cordova.plugin.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.ACTION_USB_PERMISSION;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.TAG;
import static tmk.cordova.plugin.usb.TmkUsbPlugin.logtmk;

public class TmkUsbBroadcastReceiver extends BroadcastReceiver {
    final TmkUsbPlugin tmkUsbPlugin;
    final UsbManager usbManager;
    final int vendorId;
    final int productId;

    public TmkUsbBroadcastReceiver(
            final TmkUsbPlugin tmkUsbPlugin,
            final UsbManager usbManager,
            final int vendorId,
            final int productId) {
        this.tmkUsbPlugin = tmkUsbPlugin;
        this.usbManager = usbManager;
        this.vendorId = vendorId;
        this.productId = productId;
    }

    public boolean isDeviceProperOne(UsbDevice device) {
        return device != null
                && device.getVendorId() == vendorId
                && device.getProductId() == productId;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
        if (!isDeviceProperOne(device)) {
            return;
        }

        try {
            switch (intent.getAction()) {
                case ACTION_USB_DEVICE_ATTACHED:
                    requestPermission(
                            context,
                            this.usbManager,
                            device);
                    break;

                case ACTION_USB_PERMISSION:
                    tmkUsbPlugin.setUsbSerialDevice(
                            connectIfPermissionGranted(
                                    this.usbManager,
                                    intent,
                                    device,
                                    tmkUsbPlugin.cordova.getThreadPool(),
                                    tmkUsbPlugin.usbReadCallback));
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


    public void listDevicesAndFindProperOne(
            final Context context,
            final UsbManager usbManager) {

        // API 21 does not have streams...
        List<UsbDevice> devices = emptyList();
        for (Map.Entry<String, UsbDevice> kv : usbManager.getDeviceList().entrySet()) {
            if (isDeviceProperOne(kv.getValue())) {
                devices = singletonList(kv.getValue());
                break;
            }
        }

        if (!devices.isEmpty()) {
            requestPermission(
                    context,
                    usbManager,
                    devices.get(0));
        }
    }

    private UsbSerialDevice connectIfPermissionGranted(
            final UsbManager usbManager,
            final Intent intent,
            final UsbDevice device,
            final ExecutorService threadPool,
            final UsbSerialInterface.UsbReadCallback readCallback)
            throws TmkUsbException {

        if (!intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            String msg = "connectIfPermissionGranted; action extra permission not granted";
            logtmk(msg);
            throw new TmkUsbException(msg);
        }

        return connectDevice(usbManager, device, threadPool, readCallback);
    }

    public UsbSerialDevice connectDevice(
            final UsbManager usbManager,
            final UsbDevice device,
            final ExecutorService threadPool,
            final UsbSerialInterface.UsbReadCallback readCallback)
            throws TmkUsbException {

        logtmk("device = " + UsbHelper.readDevice(device));

        final UsbSerialDevice usbSerialDevice = CDCSerialDevice
                .createUsbSerialDevice(
                        device,
                        usbManager.openDevice(device));

//        usbSerialDevice.setBaudRate(9600); // 19200
//        usbSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
//        usbSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);
//        usbSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//        usbSerialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);

        if (!usbSerialDevice.open()) {
            throw new TmkUsbException("Device could not be opened");
        }

        threadPool.execute(() -> usbSerialDevice.read(readCallback));
        return usbSerialDevice;
    }

    private void requestPermission(
            final Context context,
            final UsbManager usbManager,
            final UsbDevice device) {

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                new Intent(ACTION_USB_PERMISSION),
                0);

        usbManager.requestPermission(device, permissionIntent);
    }
}
