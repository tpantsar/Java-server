package com.server;

import com.sun.net.httpserver.*;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

public class Handler implements HttpHandler {

    private MessageDatabase database = MessageDatabase.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        System.out.println("Request handled in thread " + Thread.currentThread().getId());

        if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
            try {
                handlePOSTRequest(exchange);
            } catch (NumberFormatException | DateTimeParseException | JSONException | SQLException e) {
                e.printStackTrace();
            }
        } else if (exchange.getRequestMethod().equalsIgnoreCase("GET")) {
            try {
                handleResponseGET(exchange);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            /*
             * Inform user here that only POST and GET functions
             * are supported and send an error code 400 with a message
             * “Not supported" (without the ").
             */
            handleResponse(exchange, "Error 400: Not supported", 400);
        }
    }

    // Handle GET requests here (users use this to get messages)
    private void handleResponseGET(HttpExchange exchange) throws IOException, SQLException {
        OutputStream outputStream = exchange.getResponseBody();
        JSONArray responseMessages = new JSONArray();

        responseMessages = database.getMessages();
        String responseString = responseMessages.toString();
        byte[] bytes = responseString.getBytes("UTF-8");

        exchange.sendResponseHeaders(200, bytes.length);
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

    // Handle POST requests here (users use this for sending messages)
    private void handlePOSTRequest(HttpExchange exchange) throws IOException, NumberFormatException, JSONException, SQLException, DateTimeParseException {
        JSONObject messageJSONobj = null;

        if (exchange.getRequestHeaders().get("Content-Type").get(0).equals("application/json")) {
            String newMessage = new BufferedReader(
                    new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));

            if (newMessage == null || newMessage.length() == 0) {
                handleResponse(exchange, "Invalid data", 400);
            } else {
                try {
                    messageJSONobj = new JSONObject(newMessage);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                    LocalDateTime sent = LocalDateTime.parse(messageJSONobj.getString("sent"), formatter);

                    if (!(messageJSONobj.getString("dangertype").equals("Reindeer")
                            || messageJSONobj.getString("dangertype").equals("Moose")
                            || messageJSONobj.getString("dangertype").equals("Deer")
                            || messageJSONobj.getString("dangertype").equals("Other"))) {
                        handleResponse(exchange, "Invalid data", 400);
                    }

                    if (messageJSONobj.has("areacode") && messageJSONobj.has("phonenumber")) {
                        WarningMessage newWarningMessage = new WarningMessage(
                            messageJSONobj.getString("nickname"),
                            messageJSONobj.getString("dangertype"),
                            messageJSONobj.getDouble("latitude"),
                            messageJSONobj.getDouble("longitude"),
                            sent,
                            messageJSONobj.getString("areacode"),
                            messageJSONobj.getString("phonenumber"));
                        database.setMessage(newWarningMessage);
                    } else {
                        WarningMessage newWarningMessage = new WarningMessage(
                            messageJSONobj.getString("nickname"),
                            messageJSONobj.getString("dangertype"),
                            messageJSONobj.getDouble("latitude"),
                            messageJSONobj.getDouble("longitude"),
                            sent,
                            null,
                            null);
                        database.setMessage(newWarningMessage);
                    }
                    handleResponse(exchange, "Message received", 200);
                    
                } catch (JSONException e) {
                    System.out.println("JSON parse error, faulty user json: " + e.getMessage());
                    handleResponse(exchange, "Invalid data", 400);
                } catch (DateTimeParseException e) {
                    System.out.println("JSON parse error, faulty message json: " + e.getMessage());
                    handleResponse(exchange, "Invalid data", 400);
                }
            }
        } else {
            handleResponse(exchange, "Invalid data", 400);
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
