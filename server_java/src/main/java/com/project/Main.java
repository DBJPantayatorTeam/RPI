package com.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

// Tutorials: http://tootallnate.github.io/Java-WebSocket/

/*
    WebSockets server, example of messages:

    From client to server:
        - List of clients       { "type": "list" }
        - Private message       { "type": "private", "value": "Hello 002", "destination": "002" }
        - Broadcast message     { "type": "broadcast", "value": "Hello everyone" }

    From server to client:
        - Welcome message       { "type": "private", "from": "server", "value": "Welcome to the chat server" }
        - Client Id             { "type": "id", "from": "server", "value": "002" }
        - List of clients       { "type": "list", "from": "server", "list": ["001", "002", "003"] }
        - Private message       { "type": "private", "from": "001", "value": "Hello 002" }
        - Broadcast message     { "type": "broadcast", "from": "001", "value": "Hello everyone" }
        - Client connected      { "type": "connected", "from": "server", "id": "001" }
        - Client disconnected   { "type": "disconnected", "from": "server", "id": "001" }
 */

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        // Obtenemos el flujo de salida del proceso

        int port = 8888;
        String localIp = getLocalIPAddress();
        System.out.println("Local server IP: " + localIp);

        //String comando = "cd ~/dev/rpi-rgb-led-matrix && examples-api-use/text-example -x 5 -y 18 -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse";

        String comando = "cd ~/dev/rpi-rgb-led-matrix/utils && ./text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse \""+localIp+"\"";
        try {
            // Crear un objeto ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", comando);

            // Iniciar el proceso
            Process proceso = processBuilder.start();

            // Obtener el flujo de entrada y salida del proceso
            InputStream inputStream = proceso.getInputStream();
            OutputStream outputStream = proceso.getOutputStream();

            // Crear lectores y escritores para la entrada y salida
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);

            

        } catch (Exception e) {
            e.printStackTrace();
        }
        

        java.lang.System.setProperty("jdk.tls.client.protocols", "TLSv1,TLSv1.1,TLSv1.2");
        ChatServer server = new ChatServer(port);
        server.runServerBucle();
    }

    public static String getLocalIPAddress() throws SocketException, UnknownHostException {
        String localIp = "";
        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
        while (networkInterfaces.hasMoreElements()) {
            NetworkInterface ni = networkInterfaces.nextElement();
            Enumeration<InetAddress> inetAddresses = ni.getInetAddresses();
            while (inetAddresses.hasMoreElements()) {
                InetAddress ia = inetAddresses.nextElement();
                if (!ia.isLinkLocalAddress() && !ia.isLoopbackAddress() && ia.isSiteLocalAddress()) {
                    System.out.println(ni.getDisplayName() + ": " + ia.getHostAddress());
                    localIp = ia.getHostAddress();
                    // Si hi ha múltiples direccions IP, es queda amb la última
                }
            }
        }

        // Si no troba cap direcció IP torna la loopback
        if (localIp.compareToIgnoreCase("") == 0) {
            localIp = InetAddress.getLocalHost().getHostAddress();
        }
        return localIp;
    }


}