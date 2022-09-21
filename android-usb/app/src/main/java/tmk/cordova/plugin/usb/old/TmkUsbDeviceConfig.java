package tmk.cordova.plugin.usb.old;

import com.felhr.usbserial.UsbSerialDevice;

public class TmkUsbDeviceConfig {

    public static final TmkUsbDeviceConfig INSTANCE = new TmkUsbDeviceConfig();

    public TmkUsbDeviceConfig(Integer vendorId, Integer productId, String endLine, Integer baudRate, Integer dataBits, Integer parity, Integer flowControl, Integer stopBits) {
        this.vendorId = vendorId;
        this.productId = productId;
        this.endLine = endLine;
        this.baudRate = baudRate;
        this.dataBits = dataBits;
        this.parity = parity;
        this.flowControl = flowControl;
        this.stopBits = stopBits;
    }

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

    public Integer getVendorId() {
        return vendorId;
    }

    public void setVendorId(Integer vendorId) {
        this.vendorId = vendorId;
    }

    public Integer getProductId() {
        return productId;
    }

    public void setProductId(Integer productId) {
        this.productId = productId;
    }

    public String getEndLine() {
        return endLine;
    }

    public void setEndLine(String endLine) {
        this.endLine = endLine;
    }

    public Integer getBaudRate() {
        return baudRate;
    }

    public void setBaudRate(Integer baudRate) {
        this.baudRate = baudRate;
    }

    public Integer getDataBits() {
        return dataBits;
    }

    public void setDataBits(Integer dataBits) {
        this.dataBits = dataBits;
    }

    public Integer getParity() {
        return parity;
    }

    public void setParity(Integer parity) {
        this.parity = parity;
    }

    public Integer getFlowControl() {
        return flowControl;
    }

    public void setFlowControl(Integer flowControl) {
        this.flowControl = flowControl;
    }

    public Integer getStopBits() {
        return stopBits;
    }

    public void setStopBits(Integer stopBits) {
        this.stopBits = stopBits;
    }
}
