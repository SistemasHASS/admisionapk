package com.example.huellaserver;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class HttpServerHuella extends NanoHTTPD {

    private final HuellaHandler handler;

    public HttpServerHuella(int port, HuellaHandler handler) throws IOException {
        super(port);
        this.handler = handler;
    }

    @Override
    public Response serve(IHTTPSession session) {
        if ("/huella".equals(session.getUri())) {
            byte[] imagen = handler.getHuellaPNG();
            if (imagen != null && imagen.length > 0) {
                Response response = newFixedLengthResponse(Response.Status.OK, "image/png", new java.io.ByteArrayInputStream(imagen), imagen.length);
                response.addHeader("Content-Type", "image/png");
                return response;
            } else {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "No hay huella capturada");
            }
        }
        return newFixedLengthResponse("HuellaServer activo");
    }

}
