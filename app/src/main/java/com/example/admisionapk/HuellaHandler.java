package com.example.admisionapk;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.usb.UsbDevice;
import android.util.Base64;
import android.util.Log;

import com.zkteco.android.biometric.core.device.ParameterHelper;
import com.zkteco.android.biometric.core.device.TransportType;
import com.zkteco.android.biometric.core.utils.ToolUtils;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintCaptureListener;
import com.zkteco.android.biometric.module.fingerprintreader.FingerprintSensor;
import com.zkteco.android.biometric.module.fingerprintreader.FingprintFactory;
import com.zkteco.android.biometric.module.fingerprintreader.exception.FingerprintException;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class HuellaHandler implements ZKUSBManagerListener {
    private int vid;
    private int pid;
    private FingerprintSensor fingerprintSensor;
    private byte[] ultimaImagenPNG;
    private Context context;
    private ZKUSBManager usbManager;

    public HuellaHandler(Context ctx, int vid, int pid) {
        this.context = ctx;
        this.vid = vid;
        this.pid = pid;

        usbManager = new ZKUSBManager(ctx, this);
        usbManager.registerUSBPermissionReceiver();

        UsbDevice device = usbManager.getConnectedDevice();
        if (device != null) {
            this.vid = device.getVendorId();
            this.pid = device.getProductId();
            usbManager.initUSBPermission(this.vid, this.pid); // pedir siempre
        }
    }

    @Override
    public void onCheckPermission(int result) {
        if (result == 0) {
            abrirLector();
        } else {
            Log.e("HuellaServer", "Error permiso USB: " + result);
        }
    }

    @Override
    public void onUSBArrived(UsbDevice device) {
        this.vid = device.getVendorId();
        this.pid = device.getProductId();
        usbManager.initUSBPermission(vid, pid);
    }

    @Override
    public void onUSBRemoved(UsbDevice device) {
        Log.i("HuellaServer", "Lector desconectado");
    }

    private void abrirLector() {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put(ParameterHelper.PARAM_KEY_VID, vid);
            params.put(ParameterHelper.PARAM_KEY_PID, pid);

            fingerprintSensor = FingprintFactory.createFingerprintSensor(context, TransportType.USB, params);
            fingerprintSensor.open(0);
            fingerprintSensor.setFingerprintCaptureListener(0, listener);
            fingerprintSensor.startCapture(0);

            Log.i("HuellaServer", "Lector iniciado y escuchando huellas...");
        } catch (FingerprintException e) {
            Log.e("HuellaServer", "Error abriendo lector", e);
        }
    }

    public byte[] getHuellaPNG() {
        return ultimaImagenPNG;
    }

    private final FingerprintCaptureListener listener = new FingerprintCaptureListener() {
        @Override
        public void captureOK(byte[] fpImage) {
            try {
                Bitmap bmp = ToolUtils.renderCroppedGreyScaleBitmap(
                        fpImage,
                        fingerprintSensor.getImageWidth(),
                        fingerprintSensor.getImageHeight()
                );
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
                ultimaImagenPNG = baos.toByteArray();
                Log.i("HuellaServer", "Huella capturada (" + ultimaImagenPNG.length + " bytes)");
            } catch (Exception e) {
                Log.e("HuellaServer", "Error procesando huella", e);
            }
        }

        @Override
        public void captureError(FingerprintException e) {
            Log.e("HuellaServer", "Error captura: " + e.getMessage());
        }

        @Override public void extractOK(byte[] bytes) {}
        @Override public void extractError(int i) {}
    };

    public String getHuellaBase64() {
        if (ultimaImagenPNG != null) {
            return "data:image/png;base64," + Base64.encodeToString(ultimaImagenPNG, Base64.NO_WRAP);
        }
        return null;
    }

    public void clearHuella() {
        this.ultimaImagenPNG = null;
    }
}
