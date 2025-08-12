package com.example.huellaserver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {

    private TextView tv;
    private ZKUSBManager zkusbManager;
    private int usb_vid = 0x1b55; // VID ZKTeco
    private int usb_pid = 0x0124; // PID del modelo (ZK9500 puede ser 0x0120 o 0x0124)

    private HuellaHandler huellaHandler;
    private HttpServerHuella server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // UI para mostrar mensajes
        tv = new TextView(this);
        tv.setTextSize(20);
        tv.setTextColor(Color.BLACK);
        tv.setGravity(Gravity.CENTER);
        tv.setBackgroundColor(Color.WHITE);
        setContentView(tv);
        tv.setText("Iniciando...");

        // Inicializar ZKUSBManager
        zkusbManager = new ZKUSBManager(getApplicationContext(), new ZKUSBManagerListener() {
            @Override
            public void onCheckPermission(int result) {
                if (result == 0) {
                    tv.setText("Lector detectado. Iniciando servidor...");
                    iniciarServidor();
                } else if (result == -1) {
                    tv.setText("Lector no detectado");
                } else if (result == -2) {
                    tv.setText("Permiso denegado para acceder al lector");
                }
            }

            @Override
            public void onUSBArrived(UsbDevice device) {
                tv.setText("Lector conectado. Solicitando permiso...");
                tryGetUSBPermission();
            }

            @Override
            public void onUSBRemoved(UsbDevice device) {
                tv.setText("Lector desconectado");
                detenerServidor();
            }
        });

        // Registrar para recibir eventos de USB
        zkusbManager.registerUSBPermissionReceiver();

        new Handler().postDelayed(() -> {
            tryGetUSBPermission();
        }, 500);
    }

    private boolean enumSensor() {
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        for (UsbDevice device : usbManager.getDeviceList().values()) {
            int device_vid = device.getVendorId();
            int device_pid = device.getProductId();
            if (device_vid == 0x1b55 && (device_pid == 0x0120 || device_pid == 0x0124)) {
                usb_pid = device_pid; // guardar el PID real
                return true;
            }
        }
        return false;
    }

    private void tryGetUSBPermission() {
        if (!enumSensor()) {
            tv.setText("Lector no encontrado");
            return;
        }
        zkusbManager.initUSBPermission(usb_vid, usb_pid);
    }


    private void iniciarServidor() {
        try {
            // Inicializar el manejador de huella
            huellaHandler = new HuellaHandler(getApplicationContext());

            // Crear e iniciar el servidor HTTP
            server = new HttpServerHuella(8099, huellaHandler);
            server.start();

            tv.setText("Servidor HTTP iniciado en puerto 8099\n" +
                    "Esperando lecturas de huella...");
        } catch (Exception e) {
            tv.setText("Error iniciando servidor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void detenerServidor() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        zkusbManager.unRegisterUSBPermissionReceiver();
        detenerServidor();
    }
}
