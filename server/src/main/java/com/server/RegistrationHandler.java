package com.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RegistrationHandler implements HttpHandler {
    private UserAuthenticatorDB userAuthenticator;

    public RegistrationHandler(UserAuthenticatorDB userAuthenticator) {
        this.userAuthenticator = userAuthenticator;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            // Respond to HTTP GET requests with error 400 and response body "Not supported"
            if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
                handleResponse(exchange, "Not supported", 400);

            } else if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                if (exchange.getRequestHeaders().get("Content-Type").get(0).equals("application/json")) {
                    String newUser = new BufferedReader(
                            new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                            .lines().collect(Collectors.joining("\n"));
                    JSONObject obj = null;

                    if (newUser == null || newUser.length() == 0) {
                        handleResponse(exchange, "Invalid registration data", 400);
                    } else {
                        try {
                            obj = new JSONObject(newUser);
                        } catch (JSONException e) {
                            System.out.println("JSON parse error, faulty user json: " + e.getMessage());
                        }

                        if (obj.getString("username").equals("")
                                || obj.getString("password").equals("")
                                || obj.getString("email").equals("")) {
                            handleResponse(exchange, "Username or password incorrect", 413);
                        } else {
                            System.out.println("Registering user " + obj.getString("username") + ":" + obj.getString("password"));

                            // Check if the user registration is successful
                            if (userAuthenticator.addUser(obj.getString("username"), obj.getString("password"), obj.getString("email"))) {
                                handleResponse(exchange, "User already registered", 200);
                            } else {
                                handleResponse(exchange, "User already exists", 405);
                            }
                        }
                    }
                } else {
                    handleResponse(exchange, "No content type in request", 411);
                }
            } else {
                handleResponse(exchange, "Content-Type is not application/json", 407);
            }

        } catch (Exception e) {
            System.out.println(e.getStackTrace());
            handleResponse(exchange, "Internal server error", 500);
        }
    }

    /*
     * Used to handle responses to the client with error codes and descriptive
     * messages
     */
    private void handleResponse(HttpExchange exchange, String responseString, Integer responseCode) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();

        exchange.sendResponseHeaders(responseCode, responseString.length());
        outputStream.write(responseString.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
