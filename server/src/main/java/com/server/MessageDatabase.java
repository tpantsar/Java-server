package com.server;

import org.apache.commons.codec.digest.Crypt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.io.File;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;

public class MessageDatabase {

    private Connection dbConnection = null;
    private static MessageDatabase dbInstance = null;
    private SecureRandom secureRandom = new SecureRandom();

    // Constructor
    public MessageDatabase() {
        try {
            openDB("Database");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static synchronized MessageDatabase getInstance() {
        if (null == dbInstance) {
            dbInstance = new MessageDatabase();
        }
        return dbInstance;
    }

    /* Store messages to a database table */
    public void setMessage(WarningMessage message) throws SQLException {
        System.out.println("Sending message to database");

        String setMessageString = "insert into messages " + "VALUES('" +
                message.getNickname() + "','" +
                message.getDangertype() + "','" +
                message.getLatitude() + "','" +
                message.getLongitude() + "','" +
                message.dateAsInt() + "','" +
                message.getAreacode() + "','" +
                message.getPhonenumber() + "')";

        Statement createStatement;
        createStatement = dbConnection.createStatement();
        createStatement.executeUpdate(setMessageString);
        createStatement.close();
    }

    /* Read messages from a database table */
    public JSONArray getMessages() throws SQLException {
        System.out.println("Reading messages from database");
        String getMessagesString = "SELECT * FROM messages";

        Statement queryStatement = dbConnection.createStatement();
        ResultSet rs = queryStatement.executeQuery(getMessagesString); // Use executeQuery() in SELECT queries
        JSONArray messageArray = new JSONArray();

        while (rs.next()) {
            WarningMessage message = new WarningMessage(
                    rs.getString("nickname"),
                    rs.getString("dangertype"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    null,
                    rs.getString("areacode"),
                    rs.getString("phonenumber"));

            message.setSent(rs.getLong("sent"));
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            String sentString = message.getSent().format(formatter);
            JSONObject messageObject = new JSONObject();

            if (message.getAreacode().equals("null") && message.getPhonenumber().equals("null")) {
                // Exclude areacode and phonenumber from message
                messageObject.put("nickname", message.getNickname())
                        .put("latitude", message.getLatitude())
                        .put("longitude", message.getLongitude())
                        .put("dangertype", message.getDangertype())
                        .put("sent", sentString);
                messageArray.put(messageObject);
            } else {
                // Include areacode and phonenumber to message
                messageObject.put("nickname", message.getNickname())
                        .put("latitude", message.getLatitude())
                        .put("longitude", message.getLongitude())
                        .put("dangertype", message.getDangertype())
                        .put("sent", sentString)
                        .put("areacode", message.getAreacode())
                        .put("phonenumber", message.getPhonenumber());
                messageArray.put(messageObject);
            }
        }
        return messageArray;
    }

    /* Creates new database tables for users and messages */
    private boolean initializeDatabase() throws SQLException {
        String dbName = "Database";
        String connectionAddress = "jdbc:sqlite:" + dbName;
        dbConnection = DriverManager.getConnection(connectionAddress);

        if (null != dbConnection) {
            String createUserTable = "create table users (username varchar(50) NOT NULL, password varchar(50) NOT NULL, email varchar(50) NOT NULL, primary key(username))";
            String createMessagesTable = "create table messages (nickname varchar(50) NOT NULL, dangertype varchar(50) NOT NULL, latitude real NOT NULL, longitude real NOT NULL, sent integer NOT NULL, areacode varchar(50), phonenumber varchar(50))";

            Statement createStatement = dbConnection.createStatement();
            createStatement.executeUpdate(createUserTable);
            createStatement.executeUpdate(createMessagesTable);
            createStatement.close();
            System.out.println("Database creation OK");
            return true;
        }
        System.out.println("Database creation FAILED");
        return false;
    }

    public void openDB(String dbName) throws SQLException {
        File dbFile = new File("./" + dbName);
        boolean fileExists = dbFile.exists();
        // boolean fileExists = dbFile.isFile() && !dbFile.isDirectory();

        if (fileExists) {
            String connectionAddress = "jdbc:sqlite:" + dbName; // JDBC connection address string
            dbConnection = DriverManager.getConnection(connectionAddress); // Open a connection to the database
            System.out.println("Database opened again");
        } else {
            initializeDatabase(); // If the file did not exist, initialize the database
        }
    }

    // Closes database connection (UNUSED method)
    public void closeDB() throws SQLException {
        if (null != dbConnection) {
            dbConnection.close();
            System.out.println("Closing database connection");
            dbConnection = null;
        }
    }

    public boolean authenticateUser(String givenUserName, String givenPassword) throws SQLException {
        Statement queryStatement = null;
        ResultSet rs;

        String getMessagesString = "SELECT username, password FROM users WHERE username = '" + givenUserName + "'";
        System.out.println("Checking user " + '"' + givenUserName + '"');

        queryStatement = dbConnection.createStatement();
        rs = queryStatement.executeQuery(getMessagesString);

        if (rs.next() == false) {
            System.out.println("Cannot find user " + '"' + givenUserName + '"');
            return false;
        } else {
            // Hashed & salted password from the database
            String hashedPassword = rs.getString("password");
            if (hashedPassword.equals(Crypt.crypt(givenPassword, hashedPassword))) {
                return true; // user authenticated
            }
            return false; // else
        }
    }

    public boolean setUser(JSONObject user) throws SQLException {
        if (checkIfUserExists(user.getString("username"))) {
            return false;
        }

        byte bytes[] = new byte[13];
        secureRandom.nextBytes(bytes);
        String saltBytes = new String(Base64.getEncoder().encode(bytes));
        String salt = "$6$" + saltBytes;

        String password = user.getString("password");
        String hashedPassword = Crypt.crypt(password, salt);

        String setUserString = "INSERT INTO users " + "VALUES('" +
                user.getString("username") + "','" +
                hashedPassword + "','" +
                user.getString("email") + "')";

        Statement createStatement;
        createStatement = dbConnection.createStatement();
        createStatement.executeUpdate(setUserString);
        createStatement.close();
        return true;
    }

    private boolean checkIfUserExists(String givenUserName) throws SQLException {
        Statement queryStatement = null;
        ResultSet rs;

        String checkUser = "SELECT username FROM users WHERE username = '" + givenUserName + "'";
        System.out.println("Checking user " + '"' + givenUserName + '"' + " from database");

        queryStatement = dbConnection.createStatement();
        rs = queryStatement.executeQuery(checkUser);

        if (rs.next()) {
            System.out.println("User " + '"' + givenUserName + '"' + " exists");
            return true;
        }
        return false; // else
    }
}
