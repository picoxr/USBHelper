package com.picovr.usbservice;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbConstants;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;


public class USBHelper {

    private static final String TAG = USBHelper.class.getSimpleName();
    private static final String USB_PERMISSION ="com.android.usb.USB_PERMISSION";

    private static USBHelper instance;

    private Context mContext;
    private UsbManager usbManager;
    private UsbDeviceConnection mUsbConnection;
    private UsbInterface mUsbInterface;
    private UsbEndpoint usbEndpointIn;
    private UsbEndpoint usbEndpointOut;

    private UsbDevice mUsbDevice;

    public static final int VID = 0x0000;
    public static final int PID = 0x0000;

    public interface USBPermissionCallBack {
        void success();

        void cancel();
    }

    private USBPermissionCallBack mUSBPermissionCallBack;


    public static USBHelper getInstance() {
        if (instance == null) {
            synchronized (USBHelper.class) {
                if (instance == null) {
                    instance = new USBHelper();
                }
            }
        }
        return instance;
    }

    /**
     * Initialization
     * @param context
     */
    public void init(Context context) {
        mContext = context;
        usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
    }


    /**
     * Query the USB device specified by VID and PID.
     * @return
     */
    public UsbDevice findAssignUsbDevice() {
        HashMap<String, UsbDevice> deviceList = getUsbManager().getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        Log.e(TAG, "findAssignUsbDevice: " + deviceList.size());
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            Log.d(TAG, "findUsbDevice: Iterator: " + device.getDeviceName() + ", VID = " + device.getVendorId() + ", PID = " + device.getProductId());
            if ((VID == device.getVendorId()) && (PID == device.getProductId())) {
                Log.i(TAG, "findUsbDevice# find USB device success!");
                mUsbDevice = device;
                return device;
            }
        }
        Log.e(TAG, "findUsbDevice: selected USB device = null");
        return null;
    }

    /***
     * Check if the USB device specified by VID and PID exists.
     * @return
     */
    public boolean hasAssignUsbDevice() {
        HashMap<String, UsbDevice> deviceList = getUsbManager().getDeviceList();
        Iterator<UsbDevice> iterator = deviceList.values().iterator();
        while (iterator.hasNext()) {
            UsbDevice device = iterator.next();
            Log.d(TAG, "UsbDevice->" + device.getDeviceName() + ", VID = " + device.getVendorId() + ", PID = " + device.getProductId());
            if ((VID == device.getVendorId()) && (PID == device.getProductId())) {
                Log.i(TAG, "UsbDevice-> find USB device success!");
                return true;
            }
        }
        Log.e(TAG, "UsbDevice-> selected USB device = null");
        return false;
    }

    /**
     * Register USB permission.
     * @param device
     * @param callBack
     */
    public void registerUsbPermission(UsbDevice device, USBPermissionCallBack callBack) {
        this.mUSBPermissionCallBack = callBack;

        if (getUsbManager().hasPermission(device)) {
            Log.d(TAG, "getUsbPermission: Has permission");
            if (mUSBPermissionCallBack != null) {
                mUSBPermissionCallBack.success();
            }
        } else {
            IntentFilter filter = new IntentFilter(USB_PERMISSION);
            mContext.registerReceiver(usbPermissionReceiver, filter);
            PendingIntent intent = PendingIntent.getBroadcast(mContext, 0, new Intent(USB_PERMISSION), 0);
            getUsbManager().requestPermission(device, intent);
        }
    }

    /**
     * Unregister USB permission.
     */
    public void unRegisterUsbPermision() {
        if (usbPermissionReceiver != null) {
            mContext.unregisterReceiver(usbPermissionReceiver);
        }
    }

    /**
     * Check permission.
     * Returns true if the caller has permission to access the device.
     * RequiresFeature("android.hardware.usb.host")
     * @param device
     * @return
     */
    public boolean checkPermissions(UsbDevice device) {
        return getUsbManager().hasPermission(device);
    }

    /**
     * Open USB device.
     * @param device
     * @return
     */
    public boolean openUSBDevice(UsbDevice device) {

        if (null == device) {
            Log.d(TAG, "openUSBDevice: null = device");
            return false;
        }

        mUsbInterface = device.getInterface(0);
        try {
            mUsbConnection = usbManager.openDevice(device);
        } catch (Exception e) {
            Log.e(TAG, "openUSBDevice failed: " + e.getMessage());
            return false;
        }
        if (null == mUsbConnection) {
            Log.d(TAG, "openUSBDevice failed: null == UsbConnection");
            return false;
        }

        if (!mUsbConnection.claimInterface(mUsbInterface, true)) {
            mUsbConnection.close();
            Log.d(TAG, "openUSBDevice: USB interface not found");
            return false;
        } else {
            Log.d(TAG, "openUSBDevice: USB interface found");
        }

        for (int i = 0; i < mUsbInterface.getEndpointCount(); i++) {
            UsbEndpoint end = mUsbInterface.getEndpoint(i);
            if (end.getDirection() == UsbConstants.USB_DIR_IN) {
                usbEndpointIn = end;
                Log.d(TAG, "openUSBDevice: find usbEndpointIn:" + usbEndpointIn.toString());
            } else {
                usbEndpointOut = end;
                Log.d(TAG, "openUSBDevice: find usbEndpointOut:" + usbEndpointOut.toString());
            }
        }
        Log.i(TAG, "openUSBDevice# USB device connected");
        return true;
    }

    /**
     * Send data.
     */
    public int bulkTransfer(byte[] data, int length,int timeout) {
        if (null == mUsbConnection) return -2;
        int codeOut = mUsbConnection.bulkTransfer(usbEndpointOut, data, length,  timeout);
        Log.e(TAG, "bulkTransfer: codeOut: " + codeOut );
        return codeOut;
    }

    /**
     * Receive data.
     */
    public byte[] readFromUsb() {
        if (usbEndpointIn != null) {
            int inMax = usbEndpointIn.getMaxPacketSize();
            ByteBuffer byteBuffer = ByteBuffer.allocate(inMax);
            UsbRequest usbRequest = new UsbRequest();
            usbRequest.initialize(mUsbConnection, usbEndpointIn);
            usbRequest.queue(byteBuffer, inMax);
            if (mUsbConnection.requestWait() == usbRequest) {
                byte[] retData = byteBuffer.array();
                Log.e(TAG, "received data: " + Arrays.toString(retData));
                return retData;
            }
        }
        return null;
    }

    /**
     * Close USB device.
     */
    public void closeUSBDevice() {
        if (mUsbConnection == null) {
            return;
        }
        try {
            mUsbConnection.close();
            mUsbConnection.releaseInterface(mUsbInterface);
            mUsbConnection = null;
            usbEndpointIn = null;
            mUsbInterface = null;
            Log.d(TAG, "Device closed.");
        } catch (Exception var3) {
            Log.e(TAG, "Exception: " + var3.getMessage());
        }
    }


    public UsbManager getUsbManager() {
        if (usbManager == null) {
            usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        }
        return usbManager;
    }


    /**
     * USB permission receiver.
     */
    private final BroadcastReceiver usbPermissionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (null != device) {
                            Log.d(TAG, "onReceive: Permisson grant");
                            if (mUSBPermissionCallBack != null) {
                                mUSBPermissionCallBack.success();
                            }
                        }
                    } else {
                        Log.d(TAG, "onReceive: Permission denied for device");
                        if (mUSBPermissionCallBack != null) {
                            mUSBPermissionCallBack.cancel();
                        }
                    }
                }
            }
        }
    };
}
