package com.example.oauth.util;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class HttpResponseWriter {
    private HttpResponseWriter() {
    }

    public static void redirect(HttpExchange exchange, String location) throws IOException {
        Headers headers = exchange.getResponseHeaders();
        headers.set("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    public static void writeJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        write(exchange, statusCode, "application/json; charset=UTF-8", json);
    }

    public static void writeHtml(HttpExchange exchange, int statusCode, String html) throws IOException {
        write(exchange, statusCode, "text/html; charset=UTF-8", html);
    }

    private static void write(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(bytes);
        }
    }
}
