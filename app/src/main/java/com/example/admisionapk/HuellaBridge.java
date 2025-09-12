package com.example.admisionapk;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.util.Log;
import android.webkit.JavascriptInterface;

public class HuellaBridge {

    private static final String TAG = "HuellaBridge";

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
                    Log.d(TAG, "Permiso USB concedido, inicializando lector...");
                    huellaHandler = new HuellaHandler(context, vid, pid);
                } else if (result == -1) {
                    Log.w(TAG, "No se encontró el dispositivo USB.");
                } else if (result == -2) {
                    Log.w(TAG, "Permiso USB denegado por el usuario.");
                }
            }

            @Override
            public void onUSBArrived(UsbDevice device) {
                vid = device.getVendorId();
                pid = device.getProductId();
                Log.d(TAG, "Dispositivo USB conectado: VID=" + vid + " PID=" + pid);
                usbManager.initUSBPermission(vid, pid);
            }

            @Override
            public void onUSBRemoved(UsbDevice device) {
                Log.d(TAG, "Dispositivo USB desconectado.");
                huellaHandler = null; // liberar referencia
            }
        });

        usbManager.registerUSBPermissionReceiver();

        // Si ya está conectado al iniciar la app, pedir permisos
        UsbDevice device = usbManager.getConnectedDevice();
        if (device != null) {
            vid = device.getVendorId();
            pid = device.getProductId();
            Log.d(TAG, "Dispositivo detectado al iniciar: VID=" + vid + " PID=" + pid);
            usbManager.initUSBPermission(vid, pid);
        }
    }

    @JavascriptInterface
    public String capturarHuella() {
        if (huellaHandler != null) {
            String huella = huellaHandler.getHuellaBase64();
            Log.d(TAG, "Huella capturada: " + (huella != null ? "OK" : "NULA"));
            return huella;
        }
        Log.w(TAG, "No hay lector inicializado, no se puede capturar huella.");
        return null;
    }

    @JavascriptInterface
    public void limpiarHuella() {
        if (huellaHandler != null) {
            huellaHandler.clearHuella();
            Log.d(TAG, "Huella limpiada.");
        }
    }
}
