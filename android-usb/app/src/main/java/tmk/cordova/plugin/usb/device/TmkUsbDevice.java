package tmk.cordova.plugin.usb.device;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import org.apache.cordova.CordovaInterface;

import java.util.Map;

import tmk.cordova.plugin.usb.TmkUsbException;

import static tmk.cordova.plugin.usb.TmkUsbLogging.logtmk;

public class TmkUsbDevice {

    public static final String DEVICE_CONNECTING_ERR_MSG = "device.connecting.err";

    final private CordovaInterface cordova;
    final private UsbManager usbManager;
    final private TmkUsbConfig tmkUsbConfig;
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
            final TmkUsbConfig tmkUsbConfig,
            final UsbSerialInterface.UsbReadCallback readCallback) {

        this.cordova = cordova;
        this.usbManager = usbManager;
        this.tmkUsbConfig = tmkUsbConfig;
        this.readCallback = readCallback;

        this.vendorId = tmkUsbConfig.getVendorId();
        this.productId = tmkUsbConfig.getProductId();
    }

    public UsbSerialDevice connect(final UsbDevice device)
            throws TmkUsbException {

        final UsbDeviceConnection connection = usbManager.openDevice(device);
        final UsbSerialDevice usbSerialDevice = CDCSerialDevice
                .createUsbSerialDevice(device, connection);

        if (!usbSerialDevice.open()) {
            throw new TmkUsbException(DEVICE_CONNECTING_ERR_MSG);
        }

        tmkUsbConfig.configure(usbSerialDevice);

        cordova.getThreadPool()
                .execute(() -> usbSerialDevice.read(readCallback));

        return usbSerialDevice;
    }

    public boolean write(final String text)
            throws TmkUsbException {

        if (usbSerialDevice == null
                || !usbSerialDevice.isOpen()) {
            throw new TmkUsbException("Cannot write to the usb device - is null or not opened");
        }

        String s = text + tmkUsbConfig.getEndLine();

        cordova.getThreadPool().execute(() -> {
            try {
                usbSerialDevice.write(s.getBytes());
            } catch (Throwable t) {
                String msg = "cannot write: " + t.getMessage();
                logtmk(msg);
            }
        });

        return true;
    }

    public UsbDevice listDevicesAndFindProperOne() throws TmkUsbException {

        // API 21 does not have streams...
        for (Map.Entry<String, UsbDevice> kv : usbManager.getDeviceList().entrySet()) {
            if (isDeviceProperOne(kv.getValue())) {
                return kv.getValue();
            }
        }

        throw new TmkUsbException("device not found");
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

}
