package com.server;

import com.sun.net.httpserver.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.Executors;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManagerFactory;

public class Server {

    private Server() {
    }

    private static SSLContext serverSSLContext(String file, String pass) throws Exception {
        char[] passphrase = pass.toCharArray();
        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(file), passphrase);

        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        kmf.init(ks, passphrase);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(ks);

        SSLContext ssl = SSLContext.getInstance("TLS");
        ssl.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

        return ssl;
    }

    public static void main(String[] args) throws Exception {
        try {
            // Create the HTTPS server to port 8001 with default logger
            HttpsServer server = HttpsServer.create(new InetSocketAddress(8001), 0);

            // Create a SSLContext for HTTPS server, using the self signed certificate
            SSLContext sslContext = serverSSLContext(args[0], args[1]);

            server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    // InetSocketAddress remote = params.getClientAddress();
                    SSLContext c = getSSLContext();
                    SSLParameters sslparams = c.getDefaultSSLParameters();
                    params.setSSLParameters(sslparams);
                }
            });

            // Create an instance of the UserAuthenticator
            UserAuthenticatorDB authenticator = new UserAuthenticatorDB("warning");

            // Create context that defines path for the resource, in this case a "warning"
            final HttpContext warningContext = server.createContext("/warning", new Handler());

            server.createContext("/registration", new RegistrationHandler(authenticator));

            warningContext.setAuthenticator(authenticator);

            // Creates a default executor
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

        } catch (FileNotFoundException e) {
            System.out.println("Certificate not found!");
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}