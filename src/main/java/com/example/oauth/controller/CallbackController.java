package com.example.oauth.controller;

import com.example.oauth.util.FormParser;
import com.example.oauth.util.HttpResponseWriter;
import com.example.oauth.view.CallbackPageView;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

public class CallbackController implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        Map<String, String> query = FormParser.parse(uri.getRawQuery());
        String html = CallbackPageView.render(
                query.getOrDefault("code", ""),
                query.getOrDefault("state", "")
        );
        HttpResponseWriter.writeHtml(exchange, 200, html);
    }
}
