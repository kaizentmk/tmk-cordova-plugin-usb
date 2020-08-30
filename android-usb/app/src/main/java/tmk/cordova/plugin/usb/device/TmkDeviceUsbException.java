package tmk.cordova.plugin.usb.device;

import android.hardware.usb.UsbDevice;

import lombok.Getter;

@Getter
public class TmkDeviceUsbException extends Exception {

    private UsbDevice device;

    public TmkDeviceUsbException(final String msg) {
        super(msg);
    }

    public TmkDeviceUsbException(final String msg, final Throwable t) {
        super(msg, t);
    }

    public TmkDeviceUsbException(final String msg, UsbDevice device) {
        super(msg);
        this.device = device;
    }
}
