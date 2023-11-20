package com.project;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.json.JSONObject;

public class ChatServer extends WebSocketServer {

    static BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

    private static Map<String, String> usuarios = new HashMap<String, String>();
    public Map<String, String> registeredUsers = new HashMap<String, String>();
    public int serverIp;

    public ChatServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onStart() {
        // Quan el servidor s'inicia
        
        String host = getAddress().getAddress().getHostAddress();
        int port = getAddress().getPort();
        System.out.println("WebSockets server running at: ws://" + host + ":" + port);
        System.out.println("Type 'exit' to stop and exit server.");
        setConnectionLostTimeout(0);
        setConnectionLostTimeout(100);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // Quan un client es connecta
        String clientId = getConnectionId(conn);

        System.out.println(conn);
        System.out.println(handshake);

        executeKillCommand();


        // Saludem personalment al nou client
        JSONObject objWlc = new JSONObject("{}");
        objWlc.put("type", "private");
        objWlc.put("from", "server");
        objWlc.put("value", "Welcome to the chat server");
        conn.send(objWlc.toString());

        // Li enviem el seu identificador
        JSONObject objId = new JSONObject("{}");
        objId.put("type", "id");
        objId.put("from", "server");
        objId.put("value", clientId);
        conn.send(objId.toString());

        // Enviem al client la llista amb tots els clients connectats
        send0(conn);

        // Enviem la direcció URI del nou client a tothom
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "connected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la nova connexió
        String host = conn.getRemoteSocketAddress().getAddress().getHostAddress();
        System.out.println("New client (" + clientId + "): " + host);
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        // Quan un client es desconnecta
        String clientId = getConnectionId(conn);
        usuarios.remove(clientId);
        
        executeKillCommand();

        if (usuarios.size() != 0) {
            executeDisplayCommand(getUsers());
        } else {
            try {
                executeDisplayCommand(getLocalIPAddress());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Informem a tothom que el client s'ha desconnectat
        JSONObject objCln = new JSONObject("{}");
        objCln.put("type", "disconnected");
        objCln.put("from", "server");
        objCln.put("id", clientId);
        broadcast(objCln.toString());

        // Mostrem per pantalla (servidor) la desconnexió
        System.out.println("Client ¡+ '" + clientId + "'");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Quan arriba un missatge

        executeKillCommand();

        String clientId = getConnectionId(conn);
        try {
            JSONObject objRequest = new JSONObject(message);
            String type = objRequest.getString("type");
            if (type.equalsIgnoreCase("connection")) {
                String versino = objRequest.getString("version");
                if (versino.equalsIgnoreCase("desktop")) {
                    usuarios.put(clientId, "desktop");
                } else if (versino.equalsIgnoreCase("app")) {
                    usuarios.put(clientId, "app");
                } else {
                    usuarios.put(clientId, "¿?");
                }
                String users = getUsers();
                executeDisplayCommand(users);
            }
            else if (type.equalsIgnoreCase("show")) {
                String value = objRequest.getString("value");
                executeDisplayCommand(value);
            }
            else if (type.equalsIgnoreCase("image")) {
                String value = objRequest.getString("value");
                convertBase64ToImage(value);
                executeDisplayImageCommand();

            }
            else if (type.equalsIgnoreCase("login")) {
                String user = objRequest.getString("user");
                String passwd = objRequest.getString("password");
                Set<String> users = registeredUsers.keySet();
                JSONObject response = new JSONObject("{}");
                response.put("type", "login");

                //Validar usuari
                if (users.contains(user)) {
                    if (registeredUsers.get(user).equals(passwd)) {
                        response.put("value", true);
                    } else {
                        response.put("value", false);
                    }
                } else {
                    response.put("value", false);
                }
                conn.send(response.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        // Quan hi ha un error
        ex.printStackTrace();
    }

    public void runServerBucle() {
        boolean running = true;
        try {
            System.out.println("Starting server");
            start();
            while (running) {
                String line;
                line = in.readLine();
                if (line.equals("exit")) {
                    running = false;
                }
            }
            System.out.println("Stopping server");
            stop(1000);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendList(WebSocket conn) {
        JSONObject objResponse = new JSONObject("{}");
        objResponse.put("type", "list");
        objResponse.put("from", "server");
        objResponse.put("list", getClients());
        conn.send(objResponse.toString());
    }

    public void send0(WebSocket conn) {
        JSONObject objResponse = new JSONObject("{}");
        objResponse.put("type", "conexion");
        conn.send(objResponse.toString());
    }

    public String getConnectionId(WebSocket connection) {
        String name = connection.toString();
        return name.replaceAll("org.java_websocket.WebSocketImpl@", "").substring(0, 3);
    }

    public String[] getClients() {
        int length = getConnections().size();
        String[] clients = new String[length];
        int cnt = 0;

        for (WebSocket ws : getConnections()) {
            clients[cnt] = getConnectionId(ws);
            cnt++;
        }
        return clients;
    }

    public WebSocket getClientById(String clientId) {
        for (WebSocket ws : getConnections()) {
            String wsId = getConnectionId(ws);
            if (clientId.compareTo(wsId) == 0) {
                return ws;
            }
        }
        return null;
    }

    public static String getFirstProcess() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ps aux | grep text");
            Process proceso = processBuilder.start();
            InputStream inputStream = proceso.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            Pattern pattern = Pattern.compile("^\\s*(\\d+).*");
            StringBuilder outputBuilder = new StringBuilder();
            String linea;
            while ((linea = reader.readLine()) != null) {
                outputBuilder.append(linea).append("\n");
                Matcher matcher = pattern.matcher(linea);
                if (matcher.matches()) {
                    String numeroProceso = matcher.group(1);
                    break;
                }
            }
            int resultado = proceso.waitFor();
            return outputBuilder.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return "error";
        }
    }

    public static void executeKillCommand() {
        try {
            //Per al text scrolling
            String killCommand = "killall text-scroller"; 
            ProcessBuilder killProcessBuilder = new ProcessBuilder("bash", "-c", killCommand);
            Process killProceso = killProcessBuilder.start();

            //Per a les imatges
            String killCommand2 = "killall led-image-viewer"; 
            ProcessBuilder killProcessBuilder2 = new ProcessBuilder("bash", "-c", killCommand2);
            Process killProceso2 = killProcessBuilder2.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void convertBase64ToImage(String base64) {
        try {
            //Decodificar la cadena
            byte[] imgBytes = Base64.decodeBase64(base64);
            //Crear objecte ByteArrayInputStream, llegir la imatge i guardarla
            ByteArrayInputStream bis = new ByteArrayInputStream(imgBytes);

            BufferedImage bufferedImage = ImageIO.read(bis);
            
            File outputFile = new File("data/image.jpg");
            if (outputFile.exists()) {
                outputFile.delete();
            }
            ImageIO.write(bufferedImage, "jpg", outputFile);
            
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static void executeDisplayCommand(String text) {
        try {
            //String command = "cd ~/dev/rpi-rgb-led-matrix && examples-api-use/text-example -x 5 -y 18 -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse";
            String command = "cd ~/dev/rpi-rgb-led-matrix/utils && ./text-scroller -f ~/dev/bitmap-fonts/bitmap/cherry/cherry-10-b.bdf --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse \""+text+"\"";

            ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
            Process proceso = processBuilder.start();

            InputStream inputStream = proceso.getInputStream();
            OutputStream outputStream = proceso.getOutputStream();
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            PrintWriter writer = new PrintWriter(outputStream, true);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void executeDisplayImageCommand() {
        try {
            String command = "cd ~/dev/rpi-rgb-led-matrix/utils &&  ./led-image-viewer -C --led-cols=64 --led-rows=64 --led-slowdown-gpio=4 --led-no-hardware-pulse data/image.jpeg";

            ProcessBuilder pB = new ProcessBuilder("bash", "-c", command);
            Process proceso = pB.start();

            int exitCode = proceso.waitFor();
            System.out.println("El proces ha terminat amb el códi de sortida: " + exitCode);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static String getUsers() {
        String text = "";
        int appUsers = 0;
        int desktopUsers = 0;
        for (Map.Entry<String, String> entry : usuarios.entrySet()) {
            if (entry.getValue() == "app") {
                appUsers += 1;
            } else if (entry.getValue() == "desktop") {
                desktopUsers += 1;
            }
        }
        text += "App: " + appUsers;
        text += " Desktop: " + desktopUsers;
        return text;
    }

    public void loadUsersAndPasswords(){
        File data = new File("data/users.txt");
        boolean user = false;
        try {
            Scanner sc = new Scanner(data);
            while (sc.hasNextLine()) {
                String line = sc.nextLine();
                String name = "";
                String passwd = "";
                String[] palabras = line.split(";");

                for (String s : palabras) {
                    if (user) {
                        passwd = "" +s;
                        registeredUsers.put(name, passwd);
                        user = !user;
                    } else {
                        name = ""+ s;
                        user = !user;
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
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