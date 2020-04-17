# USBHelper Description   

## Introduction
This sample code contains common methods for Android USB communication between Pico device and USB slave device. Pico device is acting as USB host to find specified USB slave device with PID & VID. After that, developer can use USB open and data transfer methods.

You need to modify the VID and PID (at line 40 of  USBHelper.java) with your USB device value.

## Sample code
```java
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
