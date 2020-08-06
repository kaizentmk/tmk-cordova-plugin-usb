package tmk.cordova.plugin.usb.device;

public class TmkUsbDeviceNotFoundException extends Exception {
    public TmkUsbDeviceNotFoundException(String message) {
        super(message);
    }

    public TmkUsbDeviceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
