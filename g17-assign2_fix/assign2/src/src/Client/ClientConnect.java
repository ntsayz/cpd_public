package Client;

import Shared.User;
import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.security.KeyStore;
import javax.net.ssl.*;
import java.util.HashMap;
import java.util.Map;

public class ClientConnect {
    enum State {DISCONNECTED, CONNECTED, LOGGED_IN}
    private static State state = State.DISCONNECTED;
    static String sessionToken = null;
    Map<String, Integer> rankings;
    private static User currentUser;
    private static PrintWriter writer;
    private static BufferedReader reader;
    private static SSLSocket socket;
    private static ClientGUI gui;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        connectToServer(port);
    }

    private static void connectToServer(int port) {
        final String SERVER_ADDRESS = "localhost";
        try {
            // Load client trust store
            KeyStore trustStore = KeyStore.getInstance("JKS");
            trustStore.load(new FileInputStream("./src/Client/client.truststore"), "cpd2024".toCharArray());

            // Create and initialize the SSLContext
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            // Create SSLSocket
            SSLSocketFactory ssf = sslContext.getSocketFactory();
            socket = (SSLSocket) ssf.createSocket(SERVER_ADDRESS, port);
            writer = new PrintWriter(socket.getOutputStream(), true);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            state = State.CONNECTED;
            SwingUtilities.invokeLater(() -> {
                gui = new ClientGUI();
                gui.setVisible(true);
            });

        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            state = State.DISCONNECTED;
        }
    }

    public static void sendRequest(String request, String username) throws IOException {
        if (state == State.DISCONNECTED) {
            System.out.println("Not connected to server.");
            return;
        }

        writer.println(request);
        String response = reader.readLine();
        if (response != null) {
            System.out.println("Server: " + response);
            processServerResponse(response, username);
        } else {
            System.out.println("Server connection lost.");
            state = State.DISCONNECTED;
            socket.close();
        }
    }

    private static void processServerResponse(String response, String username) throws IOException {
        String status = extractParam(response, "STATUS");
        String message = extractParam(response, "MESSAGE");
        String token = extractParam(response, "TOKEN");

        message = (message != null) ? message : "No message provided";

        String actionType = response.split(" ")[0];
        ResponseHandler handler = handlers.get(actionType);
        if (handler != null) {
            handler.handle(response, status, message, token, username);
        } else {
            System.out.println("Unknown response type: " + actionType);
        }

        if ("PING".equals(actionType)) {
            if ("PLAY".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showGameScreen());
            }
        } else if ("PLAY_GAME".equals(actionType)) {
            if ("WAIT".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showWaitingScreen());
            }
        }
    }

    public static Map<String, Integer> parseRankings(String rankings) {
        Map<String, Integer> rankingsMap = new HashMap<>();
        String[] entries = rankings.split(",");
        for (String entry : entries) {
            String[] parts = entry.split(" ");
            if (parts.length == 2) {
                String username = parts[0];
                try {
                    int rank = Integer.parseInt(parts[1]);
                    rankingsMap.put(username, rank);
                } catch (NumberFormatException e) {
                    System.err.println("Failed to parse rank for entry: " + entry);
                }
            }
        }
        return rankingsMap;
    }

    interface ResponseHandler {
        void handle(String response, String status, String message, String token, String username) throws IOException;
    }

    static class LoginHandler implements ResponseHandler {
        public void handle(String response, String status, String message, String token, String username ) {
            if ("OK".equals(status)) {
                sessionToken = token;
                state = State.LOGGED_IN;
                currentUser = new User(username, "", token, 0);
                String rankingsStr = extractParam(response, "RANKINGS");
                Map<String, Integer> rankings = parseRankings(rankingsStr);
                boolean rejoin = "true".equals(extractParam(response, "REJOIN"));
                SwingUtilities.invokeLater(() -> gui.showGameOptions(currentUser, rankings,rejoin));
            } else {
                state = State.CONNECTED;
                SwingUtilities.invokeLater(() -> gui.displayMessage("Login failed. " + message));
            }
        }
    }


    static class RegisterHandler implements ResponseHandler {
        public void handle(String response, String status, String message, String token, String username) {
            if ("OK".equals(status)) {
                sessionToken = token;
                state = State.LOGGED_IN;
                currentUser = new User(username, "", token, 0);
                SwingUtilities.invokeLater(() -> gui.showGameOptions(currentUser, new HashMap<>(),false));
            } else {
                state = State.CONNECTED;
                SwingUtilities.invokeLater(() -> gui.displayMessage("Registration failed. " + message));
            }
        }
    }

    static class LogoutHandler implements ResponseHandler {
        public void handle(String response, String status, String message, String token, String username) {
            if ("OK".equals(status)) {
                state = State.CONNECTED;
                sessionToken = null;
                currentUser = null;
                SwingUtilities.invokeLater(() -> gui.showLoginScreen());
            } else if ("EXPIRED".equals(status)) {
                sessionToken = null;
                state = State.CONNECTED;
                currentUser = null;
                SwingUtilities.invokeLater(() -> gui.showLoginScreen());
            } else {
                sessionToken = null;
                state = State.CONNECTED;
                currentUser = null;
                SwingUtilities.invokeLater(() -> gui.showLoginScreen());
            }
        }
    }

    static class PlayGameHandler implements ResponseHandler {
        public void handle(String response, String status, String message, String token, String username) {
            if ("WAIT".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showWaitingScreen());
            } else if ("PLAY".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showGameScreen());
            }
        }
    }

    static class PingHandler implements ResponseHandler {
        public void handle(String response, String status, String message, String token, String username) {
            if ("PLAY".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showGameScreen());
            }
        }
    }

    static class EndGameHandler implements ResponseHandler {
        @Override
        public void handle(String response, String status, String message, String token, String username) {
            if ("OK".equals(status)) {
                SwingUtilities.invokeLater(() -> gui.showGameOptions(currentUser, new HashMap<>(), false));
                gui.displayMessage("Game finished. " + message);
            } else {
                gui.displayMessage("Failed to end game. " + message);
            }
        }
    }


    private static final Map<String, ResponseHandler> handlers = new HashMap<>();
    static {
        handlers.put("LOGIN", new LoginHandler());
        handlers.put("LOGOUT", new LogoutHandler());
        handlers.put("REGISTER", new RegisterHandler());
        handlers.put("PLAY_GAME", new PlayGameHandler());
        handlers.put("PING", new PingHandler());
        handlers.put("END_GAME", new EndGameHandler());
    }

    private static String extractParam(String response, String paramName) {
        String key = paramName.toUpperCase() + "=";
        int start = response.indexOf(key);
        if (start == -1) return null;

        start += key.length();

        if ("MESSAGE".equals(paramName.toUpperCase()) || "RANKINGS".equals(paramName.toUpperCase())) {
            int end = findNextParamIndex(response, start);
            return response.substring(start, end).trim();
        } else {
            int end = response.indexOf(" ", start);
            if (end == -1) end = response.length();
            return response.substring(start, end).trim();
        }
    }

    private static int findNextParamIndex(String response, int start) {
        for (int i = start; i < response.length(); i++) {
            if (Character.isUpperCase(response.charAt(i))) {
                int equalsIndex = response.indexOf('=', i);
                if (equalsIndex != -1 && equalsIndex == i + 1) {
                    return i - 1;
                }
            }
        }
        return response.length();
    }
}
