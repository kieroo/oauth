package com.example.oauth.service;

import com.example.oauth.model.Client;

import java.util.Map;

public class ClientRegistryService {
    private final Map<String, Client> clients = Map.of(
            "demo-client", new Client("demo-client", "http://localhost:8080/callback")
    );

    public boolean isValidRedirect(String clientId, String redirectUri) {
        Client client = clients.get(clientId);
        return client != null && client.redirectUri().equals(redirectUri);
    }
}
