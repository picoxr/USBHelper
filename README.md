# USBHelper

# USBHelper Description

**Note**: If you want to create your own JAR file, please refer to [the Guideline](http://static.appstore.picovr.com/docs/JarUnity/index.html)      

## Introduction
This file contains common methods for Android USB communication between USB host and client, you can invoke them directly in your application.

## Sample code
Modify the VID and PID in USBHelper.java to your USB device's VID and PID.
```
//Initialize usbHelper
USBHelper usbHelper = USBHelper.getInstance();
usbHelper.init(this);

//Find the USB device, check the permission and open it.
UsbDevice usbDevice = usbHelper.findAssignUsbDevice();
if (usbDevice != null) {
	if (!usbHelper.checkPermissions(usbDevice)) {
		usbHelper.registerUsbPermission(usbDevice, new USBHelper.USBPermissionCallBack() {
			@Override
			public void success() {
				Log.e(TAG, "success: USB permission grant");
				usbHelper.openUSBDevice(usbDevice);
			}

			@Override
			public void cancel() {
				Log.e(TAG, "cancel: USB permission denied");
			}
		});
	} else {
		usbHelper.openUSBDevice(usbDevice);
		Log.e(TAG, "onCreate: " + "USB has permission");
	}
}

//Then you can call corresponding methods to send and receive data.
//Send
usbHelper.bulkTransfer(data, data.length, 1000);
//Receive
byte[] resByte = usbHelper.readFromUsb();
```
