package tmk.cordova.plugin.usb;

import com.felhr.usbserial.UsbSerialDevice;

public class TmkUsbConfig {
    private int baudRate;
    private int dataBits;
    private int parity;
    private int flowControl;
    private int stopBits;
    //        usbSerialDevice.setBaudRate(9600); // 19200
//        usbSerialDevice.setDataBits(UsbSerialInterface.DATA_BITS_8);
//        usbSerialDevice.setParity(UsbSerialInterface.PARITY_NONE);
//        usbSerialDevice.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
//        usbSerialDevice.setStopBits(UsbSerialInterface.STOP_BITS_1);


    public void configure(final UsbSerialDevice d) {
        d.setBaudRate(baudRate);
        d.setDataBits(dataBits);
        d.setParity(parity);
        d.setFlowControl(flowControl);
        d.setStopBits(stopBits);
    }
}
