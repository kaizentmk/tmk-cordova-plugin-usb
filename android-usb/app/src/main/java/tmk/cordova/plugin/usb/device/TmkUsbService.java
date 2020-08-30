package tmk.cordova.plugin.usb.device;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.util.HashMap;
import java.util.Map;

import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_ATTACHED;
import static android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED;
import static tmk.cordova.plugin.usb.device.TmkUsbDevicePermission.ACTION_USB_PERMISSION_GRANTED;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmkerr;

public class TmkUsbService {

    public static final String tag = "tus";
    public static final String DEVICE_DOMAIN = "device";
    public static final String DEVICE_DOMAIN_ERROR = "device.error";
    public static final String DEVICE_DOMAIN_CONNECTING = "device.connecting";
    public static final String DEVICE_DOMAIN_CONNECTED = "device.connected";
    public static final String DEVICE_DOMAIN_DETACHED = "device.detached";

    public static final String ACTION_USB_DEVICE_FIND = "tmk.cordova.plugin.usb.device.USB_DEVICE_FIND";

    private BroadcastReceiver broadcastReceiver;

    private final Context context;
    private final UsbManager usbManager;
    private final TmkUsbDevice tmkUsbDevice;
    private final TmkUsbDevicePermission tmkUsbDevicePermission;
    private final TmkUsbDeviceConnection tmkUsbDeviceConnection;

    public TmkUsbService(
            final Context context,
            final UsbManager usbManager,
            final UsbSerialInterface.UsbReadCallback readCallback) {

        this.context = context;
        this.usbManager = usbManager;
        this.tmkUsbDevice = new TmkUsbDevice();
        this.tmkUsbDevicePermission = new TmkUsbDevicePermission(
                context,
                usbManager);

        this.tmkUsbDeviceConnection = new TmkUsbDeviceConnection(
                context,
                usbManager,
                tmkUsbDevice,
                readCallback);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                String action = intent.getAction();
                logtmk(tag, "onReceive: action = " + action);
                try {
                    if (ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                        context.sendBroadcast(new Intent(ACTION_USB_DEVICE_FIND));
                    }

                    if (ACTION_USB_DEVICE_FIND.equals(action)) {
                        UsbDevice device = findSerialPortDevice();
                        logtmk(tag, "onReceive",
                                " ACTION_USB_DEVICE_FIND",
                                " device = " + tmkUsbDevice.toString(device));
                        tmkUsbDevicePermission.requestUserPermission(context, device);
                    }

                    if (ACTION_USB_PERMISSION_GRANTED.equals(action)) {
                        tmkUsbDeviceConnection.connect(intent);
                    }

                    if (ACTION_USB_DEVICE_DETACHED.equals(action)) {
                        tmkUsbDeviceConnection.disconnect();
                    }
                } catch (final Throwable t) {
                    logtmkerr(tag, "onReceive error: action = " + action, t);
                    tmkUsbDevice.broadCastError(context, "onReceive error: action = " + action, t);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(ACTION_USB_DEVICE_FIND);
        filter.addAction(ACTION_USB_PERMISSION_GRANTED);
        filter.addAction(ACTION_USB_DEVICE_DETACHED);
        context.registerReceiver(broadcastReceiver, filter);
    }

    public boolean isSerialPortConnected() {
        return this.tmkUsbDeviceConnection.isSerialPortConnected();
    }

    public void write(byte[] data) {
        this.tmkUsbDeviceConnection.write(data);
    }

    private UsbDevice findSerialPortDevice() throws TmkDeviceUsbException {
        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();

        if (usbDevices.isEmpty()) {
            throw new TmkDeviceUsbException("UsbManager return empty list of devices");
        }

        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            UsbDevice device = entry.getValue();
            if (UsbSerialDevice.isSupported(device)) {
                return device;
            }
        }

        throw new TmkDeviceUsbException("No supported device found");
    }

    public void onDestroy() {
        tmkUsbDeviceConnection.disconnect();
        context.unregisterReceiver(broadcastReceiver);
    }
}
