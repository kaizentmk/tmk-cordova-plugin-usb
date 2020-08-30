package tmk.cordova.plugin.usb.device;

import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.CDCSerialDevice;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import static tmk.cordova.plugin.usb.device.TmkUsbDevice.TMK_USB_DEVICE_NAME;
import static tmk.cordova.plugin.usb.log.TmkUsbLogging.logtmk;

public class TmkUsbDeviceConnection {

    public static final String tag = "tudc";

    public static final String ACTION_USB_DEVICE_CONNECTED = "tmk.cordova.plugin.usb.device.USB_DEVICE_CONNECTED";

    private final Context context;
    private final UsbManager usbManager;
    private final TmkUsbDevice tmkUsbDevice;

    //    public static final int MESSAGE_FROM_SERIAL_PORT = 0;
    public static final int CTS_CHANGE = 1;
    public static final int DSR_CHANGE = 2;

    private UsbSerialInterface.UsbReadCallback readCallback;
    private UsbDevice device;
    private UsbDeviceConnection connection;
    private UsbSerialDevice serialPort;

    private boolean isSerialPortConnected;

    public boolean isSerialPortConnected() {
        return isSerialPortConnected;
    }

    public TmkUsbDeviceConnection(
            final Context context,
            final UsbManager usbManager,
            final TmkUsbDevice tmkUsbDevice,
            final UsbSerialInterface.UsbReadCallback readCallback) {
        this.context = context;
        this.usbManager = usbManager;
        this.tmkUsbDevice = tmkUsbDevice;
        this.readCallback = readCallback;

        this.isSerialPortConnected = false;
    }

    public void connect(final Intent intent) {
        device = (UsbDevice) intent.getExtras()
                .get(TMK_USB_DEVICE_NAME);
        logtmk(tag, "connectDevice: device = " + device);

        connection = usbManager.openDevice(device);

        new ConnectionThread().start();
    }

    public void write(byte[] data) {
        if (serialPort != null)
            serialPort.write(data);
    }

    /*
     * State changes in the CTS line will be received here
     */
    private UsbSerialInterface.UsbCTSCallback ctsCallback =
            state -> logtmk(tag, "onCTSChanged", "CTS_CHANGE = " + CTS_CHANGE);

    /**
     * State changes in the DSR line will be received here
     */
    private UsbSerialInterface.UsbDSRCallback dsrCallback =
            state -> logtmk(tag, "onDSRChanged", "DSR_CHANGE = " + DSR_CHANGE);

    public void disconnect() {
        if (isSerialPortConnected) {
            serialPort.close();
        }
        isSerialPortConnected = false;
    }

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {

        @Override
        public void run() {
            serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
            if (serialPort == null) {
                tmkUsbDevice.broadCastError(context, "cannot create usb serial device");
            }

            boolean opened = serialPort.open();

            if (!opened) {
                tmkUsbDevice.broadCastError(context,
                        serialPort instanceof CDCSerialDevice
                                ? "cannot open usb serial device - CDC"
                                : "cannot open usb serial device");
            }

            isSerialPortConnected = true;
            serialPort.setBaudRate(tmkUsbDevice.getBaudRate());
            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
            /**
             * Current flow control Options:
             * UsbSerialInterface.FLOW_CONTROL_OFF
             * UsbSerialInterface.FLOW_CONTROL_RTS_CTS only for CP2102 and FT232
             * UsbSerialInterface.FLOW_CONTROL_DSR_DTR only for CP2102 and FT232
             */
            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            serialPort.read(readCallback);
            serialPort.getCTS(ctsCallback);
            serialPort.getDSR(dsrCallback);

            // Some Arduinos would need some sleep because firmware wait some time to know whether a new sketch is going
            // to be uploaded or not
            //Thread.sleep(2000); // sleep some. YMMV with different chips.

            context.sendBroadcast(new Intent(ACTION_USB_DEVICE_CONNECTED));
        }
    }
}
