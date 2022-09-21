package tmk.cordova.plugin.usb.device;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;

public class TmkUsbDevice {
    public static final String tag = "tud::";

    public static final String TMK_USB_DEVICE_NAME = "tmk.cordova.plugin.usb.device.TmkUsbDevice";
    public static final String ACTION_USB_DEVICE_ERROR = "tmk.cordova.plugin.usb.device.USB_DEVICE_ERROR";

    private final int vendorId;
    private final int productId;
    private final int baudRate;

    public TmkUsbDevice() {
        this.vendorId = 0x2341; // 9026
        this.productId = 0x003E; // 62
        this.baudRate = 9600;
    }

    public boolean isDeviceProperOne(final UsbDevice device) {
        return device != null
                && device.getVendorId() == vendorId
                && device.getProductId() == productId;
    }

    public void verifyDevice(final UsbDevice device) throws TmkDeviceUsbException {
        if (!isDeviceProperOne(device)) {
            throw new TmkDeviceUsbException("Not proper device", device);
        }
    }

    public String toString(final UsbDevice device) {
        return device == null
                ? "null"
                : String.format("TmkUsbDevice(%X:%X)",
                device.getVendorId(),
                device.getProductId());
    }

    public void broadCastError(final Context context, final String msg) {
        context.sendBroadcast(new Intent(ACTION_USB_DEVICE_ERROR)
                .putExtra(ACTION_USB_DEVICE_ERROR, new TmkDeviceUsbException(msg)));
    }

    public void broadCastError(Context context, String msg, Throwable t) {
        context.sendBroadcast(new Intent(ACTION_USB_DEVICE_ERROR)
                .putExtra(ACTION_USB_DEVICE_ERROR,
                        new TmkDeviceUsbException(msg + "," + t.getMessage(), t)));
    }

    public int getVendorId() {
        return vendorId;
    }

    public int getProductId() {
        return productId;
    }

    public int getBaudRate() {
        return baudRate;
    }
}
