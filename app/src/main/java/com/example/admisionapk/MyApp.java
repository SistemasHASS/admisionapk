package com.example.admisionapk;

import android.app.Application;
import android.util.Log;

public class MyApp extends Application {

    private static HuellaHandler huellaHandler;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.i("HuellaServer", "Iniciando aplicación...");

        huellaHandler = new HuellaHandler(getApplicationContext());

        try {
            HttpServerHuella server = new HttpServerHuella(8080, huellaHandler);
            server.start();
            Log.i("HuellaServer", "Servidor HTTP iniciado en puerto 8080");
        } catch (Exception e) {
            Log.e("HuellaServer", "Error iniciando servidor HTTP", e);
        }
    }

    public static HuellaHandler getHuellaHandler() {
        return huellaHandler;
    }
}
