package com.example.oauth;

import com.example.oauth.controller.AuthorizeController;
import com.example.oauth.controller.CallbackController;
import com.example.oauth.controller.HomeController;
import com.example.oauth.controller.ResourceController;
import com.example.oauth.controller.TokenController;
import com.example.oauth.service.AuthorizationCodeService;
import com.example.oauth.service.ClientRegistryService;
import com.example.oauth.service.TokenService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class OAuthDemoServer {
    public static final int PORT = 8080;

    public static void main(String[] args) throws IOException {
        ClientRegistryService clientRegistryService = new ClientRegistryService();
        AuthorizationCodeService authorizationCodeService = new AuthorizationCodeService();
        TokenService tokenService = new TokenService();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.createContext("/", new HomeController());
        server.createContext("/authorize", new AuthorizeController(clientRegistryService, authorizationCodeService));
        server.createContext("/token", new TokenController(clientRegistryService, authorizationCodeService, tokenService));
        server.createContext("/resource", new ResourceController(tokenService));
        server.createContext("/callback", new CallbackController());
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("OAuth Demo server started at http://localhost:" + PORT);
        System.out.println("Open http://localhost:" + PORT + " to view the demo instructions.");
    }
}
