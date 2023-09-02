package com.swithun.usb_mass_storage_exfat

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.util.Log

class UsbMassStorage(device: UsbDevice, usbInterface: UsbInterface) {



    companion object {

        private const val TAG = "UsbMassStorage"

        /**
         * subclass 6 means that the usb mass storage device implements the SCSI
         * transparent command set
         */
        private const val INTERFACE_SUBCLASS = 6

        /**
         * protocol 80 means the communication happens only via bulk transfers
         */
        private const val INTERFACE_PROTOCOL = 80

        @SuppressLint("ServiceCast")
        fun filterUsbMassStorageFromAllUsbDevices(context: Context) {
            Log.d(TAG, "filterUsbMassStorageFromAllUsbDevices")
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager


            val devices = usbManager.deviceList

            val usbDevices = devices.flatMap { (key, device) ->
                Log.d(TAG, "filterUsbMassStorageFromAllUsbDevices # $key ${device.deviceId}")

                val interfacesSize = device.interfaceCount
                Log.d(TAG, "filterUsbMassStorageFromAllUsbDevices # $key ${device.deviceId} interface size: $interfacesSize")

                (0 until interfacesSize).mapNotNull { interfaceIndex ->
                    val usbInterface = device.getInterface(interfaceIndex)
                    Log.d(
                        TAG,
                        "filterUsbMassStorageFromAllUsbDevices # $key ${device.deviceId} interface $interfaceIndex: ${usbInterface.interfaceClass}, ${usbInterface.interfaceSubclass}, ${usbInterface.interfaceProtocol}"
                    )

                    if (usbInterface.interfaceClass == UsbConstants.USB_CLASS_MASS_STORAGE && usbInterface.interfaceSubclass == INTERFACE_SUBCLASS && usbInterface.interfaceProtocol == INTERFACE_PROTOCOL) {
                        Log.d(
                            TAG,
                            "filterUsbMassStorageFromAllUsbDevices # $key ${device.deviceId} interface $interfaceIndex: ${usbInterface.interfaceClass} is mass storage"
                        )
                        UsbMassStorage(
                            device,
                            usbInterface
                        )
                    } else {
                        null
                    }
                }

            }

            Log.d(TAG, "filterUsbMassStorageFromAllUsbDevices # usbDevices size ${usbDevices.size}")

        }
    }
}