package com.server;

import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.sun.net.httpserver.BasicAuthenticator;

public class UserAuthenticatorDB extends BasicAuthenticator {

    private MessageDatabase messageDb = null;

    public UserAuthenticatorDB(String realm) {
        super(realm);
        messageDb = MessageDatabase.getInstance();
    }

    @Override
    public boolean checkCredentials(String username, String password) {
        System.out.println("Checking user " + username + ":" + password + "\n");

        boolean isValidUser;
        try {
            isValidUser = messageDb.authenticateUser(username, password);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return isValidUser;
    }

    public boolean addUser(String username, String password, String email) throws JSONException, SQLException {
        boolean result = messageDb.setUser(new JSONObject().put("username", username).put("password", password).put("email", email));
        if (!result) {
            System.out.println("Cannot register user " + '"' + username + '"');
            return false;
        }
        System.out.println("Username " + '"' + username + '"' + " registered");
        return true;
    }
}
