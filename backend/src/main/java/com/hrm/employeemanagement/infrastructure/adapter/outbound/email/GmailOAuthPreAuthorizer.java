package com.hrm.employeemanagement.infrastructure.adapter.outbound.email;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.GmailScopes;

/**
 * Utility tool to perform one-time OAuth 2.0 pre-authorization on a machine with a browser.
 * Generates and stores the refresh token into the tokens directory for headless backend use.
 */
public class GmailOAuthPreAuthorizer {
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static void main(String[] args) throws Exception {
        String credentialsPath = args.length > 0 ? args[0] : "backend/credentials/credentials.json";
        String tokensDir = args.length > 1 ? args[1] : "backend/tokens";
        int port = args.length > 2 ? Integer.parseInt(args[2]) : 8889;

        System.out.println("=== Gmail OAuth 2.0 Pre-Authorization Tool ===");
        System.out.println("Credentials file: " + credentialsPath);
        System.out.println("Tokens directory: " + tokensDir);
        System.out.println("Port: " + port);

        File credFile = new File(credentialsPath);
        if (!credFile.exists()) {
            File backendPrefixed = new File("backend", credentialsPath);
            if (backendPrefixed.exists()) {
                credFile = backendPrefixed;
            } else if (credentialsPath.startsWith("backend/") || credentialsPath.startsWith("backend\\")) {
                File stripped = new File(credentialsPath.substring(8));
                if (stripped.exists()) {
                    credFile = stripped;
                }
            }
        }

        if (!credFile.exists()) {
            System.err.println("Error: Credentials file not found at " + credFile.getAbsolutePath());
            System.exit(1);
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        GoogleClientSecrets clientSecrets;
        try (InputStream in = new FileInputStream(credFile)) {
            clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in, StandardCharsets.UTF_8));
        }

        File tokenDir = new File(tokensDir);
        if (!tokenDir.exists()) {
            tokenDir.mkdirs();
        }

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport,
                JSON_FACTORY,
                clientSecrets,
                Collections.singletonList(GmailScopes.GMAIL_SEND))
                .setDataStoreFactory(new FileDataStoreFactory(tokenDir))
                .setAccessType("offline")
                .build();

        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(port).build();
        System.out.println("Opening browser for OAuth authorization. Please sign in and grant permissions...");
        Credential credential = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");

        System.out.println("Successfully pre-authorized!");
        System.out.println("Refresh token present: " + (credential.getRefreshToken() != null));
        System.out.println("Token saved to: " + tokenDir.getAbsolutePath());
    }
}
