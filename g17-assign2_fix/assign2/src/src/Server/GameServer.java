package Server;

import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import javax.net.ssl.*;
import Shared.User;
import org.json.*;
import javax.swing.*;

public class GameServer {
    private static final int PLAYERS_PER_GAME = 2;
    private final List<Socket> connectedPlayers = new ArrayList<>();
    private Map<String, User> registeredUsers;
    private final TokenManager tokenManager;
    private SSLServerSocket serverSocket;
    private final List<User> waitingQueue = new ArrayList<>();
    private final ReentrantLock waitingQueueLock = new ReentrantLock();
    private final ReentrantLock databaseLock = new ReentrantLock();
    private final Map<String, ResponseHandler> handlers = new HashMap<>();
    private final Map<String, PrintWriter> clientOutputs = new HashMap<>();
    private final Database database;
    private final ConcurrentHashMap<String, ActiveConnection> activeConnections = new ConcurrentHashMap<>();
    private final ExecutorService threadPoolGame = Executors.newFixedThreadPool(10);
    private ServerGUI serverStatusGUI;
    private final List<String> serverLogs = Collections.synchronizedList(new ArrayList<>());
    private final List<Game> activeGames = Collections.synchronizedList(new ArrayList<>());

    public GameServer() throws IOException {
        this.database = new Database();
        this.registeredUsers = database.loadRegisteredUsers();
        this.tokenManager = new TokenManager(this);
        handlers.put("LOGIN", new LoginHandler());
        handlers.put("LOGOUT", new LogoutHandler());
        handlers.put("REGISTER", new RegisterHandler());
        handlers.put("PLAY_GAME", new PlayGameHandler());
        handlers.put("PING", new PingHandler());  // Add
        handlers.put("END_GAME", new EndGameHandler());

        SwingUtilities.invokeLater(() -> {
            serverStatusGUI = new ServerGUI(this);
            serverStatusGUI.setVisible(true);
        });
    }

    public void sendLogout(String token) {
        PrintWriter out = clientOutputs.get(token);
        if (out != null) {
            out.println("LOGOUT - STATUS=EXPIRED MESSAGE=Session expired. Please login again.");
            clientOutputs.remove(token);
        }
    }

    public synchronized boolean addActiveConnection(User user, Socket socket, PrintWriter out) {
        if (activeConnections.containsKey(user.getUsername())) {
            return false;
        }
        ActiveConnection connection = new ActiveConnection(user, socket, out);
        activeConnections.put(user.getUsername(), connection);
        return true;
    }

    public synchronized void removeActiveConnection(User user) {
        ActiveConnection connection = activeConnections.remove(user.getUsername());
        if (connection != null) {
            try {
                connection.getSocket().close();
                System.out.println("Closed connection for user: " + user.getUsername());
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
            removeFromWaitingQueue(user);
        }
    }

    public void addQueueList(User user, int mode) {
        waitingQueueLock.lock();
        try {
            for (User player : waitingQueue) {
                if (player.equals(user)) {
                    System.out.println("Found a duplicate in the waiting queue.\n");
                    return;
                }
            }
            waitingQueue.add(user);
            System.out.println(user.getUsername() + " is in the waiting queue.\n");
            logMessage("User " + user.getUsername() + " added to the waiting queue.");
            if (waitingQueue.size() >= PLAYERS_PER_GAME && mode == 1) {
                startGameSession();
            }
            else if (waitingQueue.size() >= PLAYERS_PER_GAME && mode==0) {
                startRankSession();
            }
        } finally {
            waitingQueueLock.unlock();
        }
    }

    private void startGameSession() {
        List<User> gamePlayers = new ArrayList<>();
        for (int i = 0; i < PLAYERS_PER_GAME; i++) {
            gamePlayers.add(waitingQueue.remove(0));
        }
        Game game = new Game(gamePlayers, database, waitingQueue, databaseLock, waitingQueueLock, 1);
        threadPoolGame.execute(game);
        activeGames.add(game);
        logMessage("Started a new game with users: " + gamePlayers);
    }

    private void startRankSession() {
        List<User> gamePlayers = new ArrayList<>();

        this.waitingQueueLock.lock();
        if (this.waitingQueue.size() >= PLAYERS_PER_GAME) {
            this.sortClients();

            for (int i = 0 ; i < this.waitingQueue.size() - PLAYERS_PER_GAME + 1; i++) {

                User first = this.waitingQueue.get(i + PLAYERS_PER_GAME - 1);
                User second = this.waitingQueue.get(i);

                if (first.getRank() - second.getRank() > 200) {
                    continue;
                }

                for (int j = 0; j < PLAYERS_PER_GAME; j++) {
                    gamePlayers.add(this.waitingQueue.remove(i));
                }

                Game game = new Game(gamePlayers, database, waitingQueue, databaseLock, waitingQueueLock, 0);
                threadPoolGame.execute(game);
                activeGames.add(game);
                logMessage("Started a new game with users: " + gamePlayers);
                this.waitingQueueLock.unlock();
                return;
            }
        }

        this.waitingQueueLock.unlock();
    }

    public static void main(String[] args) throws IOException {
        GameServer server = new GameServer();
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        try {
            server.start(port);
        } catch (IOException e) {
            System.out.println("Server failed to start: " + e.getMessage());
        } finally {
            server.stop();
        }
    }

    public void start(int port) throws IOException {
        try {
            // Load server key store
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(new FileInputStream("./src/Server/server.keystore"), "cpd2024".toCharArray());

            // Create and initialize the SSLContext
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keyStore, "cpd2024".toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);

            // Create SSLServerSocket
            SSLServerSocketFactory ssf = sslContext.getServerSocketFactory();
            serverSocket = (SSLServerSocket) ssf.createServerSocket(port);
            System.out.println("Server started on port " + port + ". Waiting for connections...");

            while (!serverSocket.isClosed()) {
                SSLSocket clientSocket = (SSLSocket) serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            System.out.println("Failed to start SSL server: " + e.getMessage());
        }
    }

    public void stop() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing server socket: " + e.getMessage());
            }
        }
        threadPoolGame.shutdown();
        tokenManager.shutdown();
    }

    private void handleClient(SSLSocket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                processRequest(line, out, clientSocket);
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            activeConnections.values().removeIf(connection -> connection.getSocket().equals(clientSocket));
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void processRequest(String line, PrintWriter out, Socket clientSocket) throws IOException {
        String[] parts = line.split(" ", 2);
        String command = parts[0];
        String content = parts.length > 1 ? parts[1] : "";
        Map<String, String> params = parseParams(content.split(" "));

        User user = null;
        if ("LOGIN".equals(command)) {
            if (params.containsKey("USERNAME") && params.containsKey("PASSWORD")) {
                user = authenticate(params.get("USERNAME"), params.get("PASSWORD"));
                if (user != null) {
                    logMessage("User " + user.getUsername() + " logged in.");
                }
            }
        } else {
            if (params.containsKey("TOKEN")) {
                String token = params.get("TOKEN");
                if (tokenManager.validateToken(token)) {
                    user = tokenManager.findUserByToken(token);
                }
            }
        }

        ResponseHandler handler = handlers.get(command);
        if (handler != null) {
            handler.handleRequest(content, out, this, user, clientSocket);
        } else {
            out.println(command + " - STATUS=EXPIRED MESSAGE=Invalid request or credentials");
        }

        saveServerState();
        printServerState();
    }

    private void sortClients() {
        this.waitingQueueLock.lock();
        this.waitingQueue.sort(Comparator.comparingLong(User::getRank));
        this.waitingQueueLock.unlock();
    }

    Map<String, String> parseParams(String[] parts) {
        Map<String, String> params = new HashMap<>();
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    private User authenticate(String username, String password) {
        User user = registeredUsers.get(username);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }

    public void registerClientOutput(String token, PrintWriter out) {
        clientOutputs.put(token, out);
    }

    public void unregisterClientOutput(String token) {
        clientOutputs.remove(token);
    }

    public Database getDatabase() {
        return database;
    }

    public ConcurrentHashMap<String, ActiveConnection> getActiveConnections() {
        return activeConnections;
    }

    public List<User> getWaitingQueue() {
        return waitingQueue;
    }

    public ReentrantLock getWaitingQueueLock() {
        return waitingQueueLock;
    }

    public List<Game> getActiveGames() {
        return activeGames;
    }

    public List<String> getServerLogs() {
        return serverLogs;
    }

    public void logMessage(String message) {
        String logEntry = "[" + new Date() + "] " + message;
        serverLogs.add(logEntry);
        System.out.println(logEntry);
    }

    private void printServerState() {
        System.out.println("---- Server State ----");
        System.out.println("Active Connections:");
        activeConnections.forEach((username, connection) -> {
            System.out.println("Username: " + username + ", IP: " + connection.getSocket().getInetAddress() + ", Port: " + connection.getSocket().getPort());
        });

        System.out.println("Token Expiry Times:");
        tokenManager.getTokenExpiry().forEach((token, expiryTime) -> {
            User user = tokenManager.findUserByToken(token);
            if (user != null) {
                System.out.println("Username: " + user.getUsername() + ", Token: " + token + ", Expires in: " + (expiryTime - System.currentTimeMillis()) / 1000 + " seconds");
                if( ((expiryTime - System.currentTimeMillis()) / 1000) < 0){
                    removeActiveConnection(user);
                }
            }
        });

        System.out.println("----------------------");
    }

    private void saveServerState() {
        JSONObject serverState = new JSONObject();
        JSONArray activeConnectionsArray = new JSONArray();
        activeConnections.forEach((username, connection) -> {
            JSONObject connectionJson = new JSONObject();
            connectionJson.put("username", username);
            connectionJson.put("ip", connection.getSocket().getInetAddress().toString());
            connectionJson.put("port", connection.getSocket().getPort());
            activeConnectionsArray.put(connectionJson);
        });
        serverState.put("activeConnections", activeConnectionsArray);

        JSONArray tokenExpiryArray = new JSONArray();
        tokenManager.getTokenExpiry().forEach((token, expiryTime) -> {
            User user = tokenManager.findUserByToken(token);
            if (user != null) {
                JSONObject tokenJson = new JSONObject();
                tokenJson.put("username", user.getUsername());
                tokenJson.put("token", token);
                tokenJson.put("expiry", expiryTime);
                tokenExpiryArray.put(tokenJson);
            }
        });
        serverState.put("tokenExpiryTimes", tokenExpiryArray);
        serverState.put("timestamp", System.currentTimeMillis());

        database.saveGameState(serverState);
    }

    public String getRankings() {
        StringBuilder rankings = new StringBuilder();
        registeredUsers.values().stream()
                .sorted(Comparator.comparingInt(User::getRank).reversed())
                .forEach(user -> rankings.append(user.getUsername()).append(" ").append(user.getRank()).append(","));
        if (rankings.length() > 0) {
            rankings.setLength(rankings.length() - 1); // Remove the trailing comma
        }
        return rankings.toString();
    }

    public boolean isGameStartedForUser(User user) {
        for (Game game : activeGames) {
            if (game.containsPlayer(user)) {
                return true;
            }
        }
        return false;
    }

    public void removeActiveGame(User user){
        Iterator<Game> iterator = activeGames.iterator();
        while (iterator.hasNext()) {
            Game game = iterator.next();
            if (game.containsPlayer(user)) {
                iterator.remove();
                return;
            }
        }
    }

    public Game getGameForUser(User user) {
        for (Game game : activeGames) {
            if (game.containsPlayer(user)) {
                return game;
            }
        }
        return null;
    }



    public void removeFromWaitingQueue(User user) {
        waitingQueueLock.lock();
        try {
            waitingQueue.removeIf(u -> u.getUsername().equals(user.getUsername()));
        } finally {
            waitingQueueLock.unlock();
        }
    }

}
