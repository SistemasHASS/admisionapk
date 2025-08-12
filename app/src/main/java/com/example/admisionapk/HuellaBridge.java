package com.example.admisionapk;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.webkit.JavascriptInterface;

public class HuellaBridge {
    private Context context;
    private HuellaHandler huellaHandler;
    private ZKUSBManager usbManager;
    private int vid = 0x1b55;
    private int pid = 0x0124;

    public HuellaBridge(Context ctx) {
        this.context = ctx;

        usbManager = new ZKUSBManager(ctx, new ZKUSBManagerListener() {
            @Override
            public void onCheckPermission(int result) {
                if (result == 0) {
                    // Crear y abrir el lector en cuanto se conceda el permiso
                    huellaHandler = new HuellaHandler(ctx, vid, pid);
                }
            }

            @Override
            public void onUSBArrived(UsbDevice device) {
                vid = device.getVendorId();
                pid = device.getProductId();
                usbManager.initUSBPermission(vid, pid);
            }

            @Override
            public void onUSBRemoved(UsbDevice device) {
                // manejar desconexión si es necesario
            }
        });

        usbManager.registerUSBPermissionReceiver();

        // Si ya está conectado, pedir permisos inmediatamente
        UsbDevice device = usbManager.getConnectedDevice();
        if (device != null) {
            vid = device.getVendorId();
            pid = device.getProductId();
            usbManager.initUSBPermission(vid, pid);
        }
    }

    @JavascriptInterface
    public String capturarHuella() {
        // No pedimos permisos aquí, solo devolvemos la última captura
        if (huellaHandler != null) {
            return huellaHandler.getHuellaBase64();
        }
        return null;
    }

    @JavascriptInterface
    public void limpiarHuella() {
        if (huellaHandler != null) {
            huellaHandler.clearHuella();
        }
    }

}
