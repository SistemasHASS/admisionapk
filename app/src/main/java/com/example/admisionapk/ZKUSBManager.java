package com.example.admisionapk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Random;

public class ZKUSBManager {

    private int vid = 0x1b55; // Vendor ID
    private int pid = 0;      // Product ID

    private Context mContext;
    private static final String TAG = "ZKUSBManager";

    // Permiso USB dinámico
    private static final String SOURCE_STRING = "0123456789-_abcdefghigklmnopqrstuvwxyzABCDEFGHIGKLMNOPQRSTUVWXYZ";
    private static final int DEFAULT_LENGTH = 16;
    private String ACTION_USB_PERMISSION;
    private boolean mbRegisterFilter = false;
    private ZKUSBManagerListener zknirusbManagerListener;

    private final BroadcastReceiver usbMgrReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null && device.getVendorId() == vid && device.getProductId() == pid) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Log.d(TAG, "Permiso USB concedido");
                        zknirusbManagerListener.onCheckPermission(0);
                    } else {
                        Log.w(TAG, "Permiso USB denegado");
                        zknirusbManagerListener.onCheckPermission(-2);
                    }
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, "USB conectado: VID=" + device.getVendorId() + " PID=" + device.getProductId());
                    zknirusbManagerListener.onUSBArrived(device);
                }
            }
            else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null) {
                    Log.d(TAG, "USB desconectado");
                    zknirusbManagerListener.onUSBRemoved(device);
                }
            }
        }
    };

    private String createRandomString(String source, int length) {
        if (source == null || source.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        Random random = new Random();
        for (int index = 0; index < length; index++) {
            result.append(source.charAt(random.nextInt(source.length())));
        }
        return result.toString();
    }

    public boolean registerUSBPermissionReceiver() {
        if (mContext == null || mbRegisterFilter) {
            return false;
        }

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            mContext.registerReceiver(usbMgrReceiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            mContext.registerReceiver(usbMgrReceiver, filter);
        }

        mbRegisterFilter = true;
        return true;
    }

    public void unRegisterUSBPermissionReceiver() {
        if (mContext == null || !mbRegisterFilter) {
            return;
        }
        mContext.unregisterReceiver(usbMgrReceiver);
        mbRegisterFilter = false;
    }

    public UsbDevice getConnectedDevice() {
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() != 0 && device.getProductId() != 0) {
                return device;
            }
        }
        return null;
    }

    public ZKUSBManager(@NonNull Context context, @NonNull ZKUSBManagerListener listener) {
        if (context == null || listener == null) {
            throw new NullPointerException("context or listener is null");
        }
        zknirusbManagerListener = listener;
        ACTION_USB_PERMISSION = createRandomString(SOURCE_STRING, DEFAULT_LENGTH);
        mContext = context;
    }

    public void initUSBPermission(int vid, int pid) {
        UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        UsbDevice usbDevice = null;

        for (UsbDevice device : usbManager.getDeviceList().values()) {
            if (device.getVendorId() == vid && device.getProductId() == pid) {
                usbDevice = device;
                break;
            }
        }

        if (usbDevice == null) {
            Log.w(TAG, "No se encontró el dispositivo USB");
            zknirusbManagerListener.onCheckPermission(-1);
            return;
        }

        this.vid = vid;
        this.pid = pid;

        if (!usbManager.hasPermission(usbDevice)) {
            Intent intent = new Intent(this.ACTION_USB_PERMISSION);

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                flags |= PendingIntent.FLAG_MUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getBroadcast(mContext, 0, intent, flags);
            usbManager.requestPermission(usbDevice, pendingIntent);

            Log.d(TAG, "Solicitando permiso para USB...");
        } else {
            Log.d(TAG, "Ya se tiene permiso para USB");
            zknirusbManagerListener.onCheckPermission(0);
        }
    }
}
