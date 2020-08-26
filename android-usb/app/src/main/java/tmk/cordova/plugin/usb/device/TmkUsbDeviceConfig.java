package tmk.cordova.plugin.usb.device;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Setter
@Getter
@Builder
public class TmkUsbDeviceConfig {

    public static final TmkUsbDeviceConfig INSTANCE = new TmkUsbDeviceConfig();

    private Integer vendorId;
    private Integer productId;

    private String endLine;

    private Integer baudRate;
    private Integer dataBits;
    private Integer parity;
    private Integer flowControl;
    private Integer stopBits;

    public TmkUsbDeviceConfig() {
        reset();
    }

    private void reset() {
        this.vendorId = 0x2341; // 9026
        this.productId = 0x003E; // 62

//        this.vendorId = 9025;
//        this.productId = 61;

        this.endLine = "\r\n";
    }

    public void resetToDefaults() {
        reset();

        this.setBaudRate(9600);
//        this.setBaudRate(19200);
//        this.setDataBits(UsbSerialInterface.DATA_BITS_8);
//        this.setParity(UsbSerialInterface.PARITY_NONE);
//        this.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//        this.setStopBits(UsbSerialInterface.STOP_BITS_1);
    }

    public void configure(final UsbSerialDevice d) {
        if (this.baudRate != null) {
            d.setBaudRate(this.baudRate);
        }
        if (this.dataBits != null) {
            d.setDataBits(this.dataBits);
        }

        if (this.parity != null) {
            d.setParity(this.parity);
        }

        if (this.flowControl != null) {
            d.setFlowControl(this.flowControl);
        }

        if (this.stopBits != null) {
            d.setStopBits(this.stopBits);
        }
    }
}
