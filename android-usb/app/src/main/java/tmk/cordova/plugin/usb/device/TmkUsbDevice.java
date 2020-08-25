package tmk.cordova.plugin.usb.device;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.apache.cordova.CordovaInterface;

import java.util.Arrays;
import java.util.Map;

import tmk.cordova.plugin.usb.TmkUsbException;

import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;
import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmkerr;

public class TmkUsbDevice {

    public static final String tag = "tud::";

    public static final String DEVICE_CONNECTING_ERR_MSG = "device.connecting.err";

    final private CordovaInterface cordova;
    final private UsbManager usbManager;
    final private TmkUsbDeviceConfig tmkUsbDeviceConfig;
    final UsbSerialInterface.UsbReadCallback readCallback;

    final int vendorId;
    final int productId;

    /**
     * for writing data to the usb device
     */
    UsbSerialDevice usbSerialDevice;
    UsbDeviceConnection connection;

    public TmkUsbDevice(
            final CordovaInterface cordova,
            final UsbManager usbManager,
            final TmkUsbDeviceConfig tmkUsbDeviceConfig,
            final UsbSerialInterface.UsbReadCallback readCallback) {

        this.cordova = cordova;
        this.usbManager = usbManager;
        this.tmkUsbDeviceConfig = tmkUsbDeviceConfig;
        this.readCallback = readCallback;

        this.vendorId = tmkUsbDeviceConfig.getVendorId();
        this.productId = tmkUsbDeviceConfig.getProductId();
    }

    public UsbSerialDevice connect(final UsbDevice device)
            throws TmkUsbException {

        logtmk(tag, "connect: start");

        final UsbDeviceConnection connection = usbManager.openDevice(device);
        final UsbSerialDevice usbSerialDevice = CDCSerialDevice
                .createUsbSerialDevice(device, connection);

        if (!usbSerialDevice.open()) {
            throw new TmkUsbException(DEVICE_CONNECTING_ERR_MSG);
        }

        tmkUsbDeviceConfig.configure(usbSerialDevice);

        cordova.getThreadPool()
                .execute(() -> usbSerialDevice.read(readCallback));

        logtmk(tag, "connect: end");

        return usbSerialDevice;
    }

    public boolean write(final String text)
            throws TmkUsbException {

        if (usbSerialDevice == null
                || !usbSerialDevice.isOpen()) {
            throw new TmkUsbException("Cannot write to the usb device - is null or not opened");
        }

        String s = text + tmkUsbDeviceConfig.getEndLine();

        cordova.getThreadPool().execute(() -> {
            try {
                usbSerialDevice.write(s.getBytes());
            } catch (Throwable t) {
                String msg = "cannot write: " + t.getMessage();
                logtmkerr(msg, Arrays.toString(t.getStackTrace()));
            }
        });

        return true;
    }

    public UsbDevice listDevicesAndFindProperOne() throws TmkUsbDeviceNotFoundException {

        logtmk(tag, "listDevicesAndFindProperOne: start");

        // API 21 does not have streams...
        for (Map.Entry<String, UsbDevice> kv : usbManager.getDeviceList().entrySet()) {
            logtmk(tag, "listDevicesAndFindProperOne: ", "checking device",
                    "" + kv.getValue().getVendorId(),
                    "" + kv.getValue().getDeviceId());
            if (isDeviceProperOne(kv.getValue())) {
                logtmk(tag, "listDevicesAndFindProperOne: end");
                return kv.getValue();
            }
        }

        logtmk(tag, "listDevicesAndFindProperOne: no proper device found");
        logtmk(tag, "listDevicesAndFindProperOne: end");
        throw new TmkUsbDeviceNotFoundException("device not found");
    }

    public boolean isDeviceProperOne(UsbDevice device) {
        return device != null
                && device.getVendorId() == vendorId
                && device.getProductId() == productId;
    }

    public void onDestroy() {
        if (usbSerialDevice != null) {
            usbSerialDevice.close();
        }

        if (connection != null) {
            connection.close();
        }
    }

    public UsbSerialDevice getUsbSerialDevice() {
        return usbSerialDevice;
    }

    public UsbDeviceConnection getConnection() {
        return connection;
    }
}
