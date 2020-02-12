package tmk.cordova.plugin.usb;

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
public class TmkUsbConfig {
    public Integer vendorId;
    public Integer productId;

    private Integer baudRate;
    private Integer dataBits;
    private Integer parity;
    private Integer flowControl;
    private Integer stopBits;

    public TmkUsbConfig() {
        this.resetToDefaults();
    }

    public void resetToDefaults() {
        this.vendorId = 0x2341; // 9026
        this.productId = 0x003E; // 62

        this.setBaudRate(9600); // 19200
        this.setDataBits(UsbSerialInterface.DATA_BITS_8);
        this.setParity(UsbSerialInterface.PARITY_NONE);
        this.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
        this.setStopBits(UsbSerialInterface.STOP_BITS_1);
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
