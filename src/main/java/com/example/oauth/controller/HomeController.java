package com.example.oauth.controller;

import com.example.oauth.util.HttpResponseWriter;
import com.example.oauth.view.HomePageView;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class HomeController implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        HttpResponseWriter.writeHtml(exchange, 200, HomePageView.render());
    }
}
