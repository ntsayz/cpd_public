package Server;

import Shared.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TokenManager {
    private Map<String, String> userTokens = new HashMap<>();
    private Map<String, Long> tokenExpiry = new HashMap<>();
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private GameServer server;

    public TokenManager(GameServer server) {
        this.server = server;
        scheduler.scheduleAtFixedRate(this::expireOldTokens, 1, 1, TimeUnit.MINUTES);
    }

    public String createToken(String username, PrintWriter out) throws IOException {
        String token = UUID.randomUUID().toString();
        userTokens.put(username, token);
        tokenExpiry.put(token, getCurrentTimePlusMinutes(30));

        // Load current users, update token, and save back
        Map<String, User> users = server.getDatabase().loadRegisteredUsers();
        User user = users.get(username);
        if (user != null) {
            user.setToken(token);
            server.getDatabase().saveUsers(users);
        }

        server.registerClientOutput(token, out);
        return token;
    }

    private long getCurrentTimePlusMinutes(int minutes) {
        return System.currentTimeMillis() + minutesToMillis(minutes);
    }

    private long minutesToMillis(int minutes) {
        return TimeUnit.MINUTES.toMillis(minutes);
    }

    private void expireOldTokens() {
        long currentTime = System.currentTimeMillis();
        List<String> expiredTokens = new ArrayList<>();
        tokenExpiry.entrySet().removeIf(entry -> {
            if (entry.getValue() < currentTime) {
                expiredTokens.add(entry.getKey());
                return true;
            }
            return false;
        });
        expiredTokens.forEach(token -> {
            userTokens.values().remove(token);
            User user = findUserByToken(token);
            if (user != null) {
                System.out.println("Token expired for " + user.getUsername());
                user.setToken(null);
                server.removeActiveConnection(user); // Remove active connection and close socket
                try {
                    server.getDatabase().saveUsers(server.getDatabase().loadRegisteredUsers());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            server.sendLogout(token);
        });
    }

    public boolean validateToken(String token) {
        Long expiry = tokenExpiry.get(token);
        if (expiry != null && expiry > System.currentTimeMillis()) {
            return true;
        } else {
            expireToken(token);
            return false;
        }
    }

    public void expireToken(String token) {
        userTokens.values().remove(token);
        tokenExpiry.remove(token);
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public User findUserByToken(String token) {
        for (Map.Entry<String, String> entry : userTokens.entrySet()) {
            if (entry.getValue().equals(token)) {
                try {
                    return server.getDatabase().getUser(entry.getKey());
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    public Map<String, Long> getTokenExpiry() {
        return tokenExpiry;
    }
}
