package Server;

import Shared.User;
import org.json.*;
import java.nio.file.*;
import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Database {
    private static final String USER_DB_PATH = "./database/users.json";
    private static final String SERVER_STATE_PATH = "./database/server_state.json";

    public Database() {
        initializeDatabase();
    }

    private void initializeDatabase() {
        createFileIfNotExists(USER_DB_PATH, new JSONObject().put("users", new JSONArray()).toString());
        createFileIfNotExists(SERVER_STATE_PATH, new JSONObject().toString());
    }

    private void createFileIfNotExists(String path, String initialContent) {
        Path filePath = Paths.get(path);
        if (!Files.exists(filePath)) {
            try {
                Files.createDirectories(filePath.getParent());
                Files.write(filePath, Collections.singleton(initialContent));
            } catch (IOException e) {
                System.err.println("Error initializing database file: " + path + "; " + e.getMessage());
            }
        }
    }

    public JSONObject loadUsers() {
        return loadJsonFromFile(USER_DB_PATH);
    }

    public void saveUsers(JSONObject users) {
        saveJsonToFile(users, USER_DB_PATH);
    }

    public JSONObject loadGameState() {
        return loadJsonFromFile(SERVER_STATE_PATH);
    }

    public void saveGameState(JSONObject gameState) {
        saveJsonToFile(gameState, SERVER_STATE_PATH);
    }

    public Map<String, User> loadRegisteredUsers() throws IOException {
        JSONObject usersJson = loadJsonFromFile(USER_DB_PATH);
        Map<String, User> users = new HashMap<>();
        if (usersJson.has("users")) {
            JSONArray userList = usersJson.getJSONArray("users");
            for (int i = 0; i < userList.length(); i++) {
                JSONObject userJson = userList.getJSONObject(i);
                String username = userJson.getString("username");
                String password = userJson.getString("password");
                String token = userJson.optString("token", null);
                Integer rank = userJson.getInt("rank");
                users.put(username, new User(username, password, token, rank));
            }
        }
        return users;
    }

    public void saveUsers(Map<String, User> users) throws IOException {
        JSONArray userList = new JSONArray();
        for (User user : users.values()) {
            JSONObject userJson = new JSONObject();
            userJson.put("username", user.getUsername());
            userJson.put("password", user.getPassword());
            userJson.put("token", user.getToken());
            userJson.put("rank", user.getRank());
            userList.put(userJson);
        }
        JSONObject usersJson = new JSONObject();
        usersJson.put("users", userList);
        saveJsonToFile(usersJson, USER_DB_PATH);
    }

    public void addUser(User user) {
        try {
            Map<String, User> users = loadRegisteredUsers();
            users.put(user.getUsername(), user);
            saveUsers(users);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private JSONObject loadJsonFromFile(String filePath) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filePath)));
            return new JSONObject(content);
        } catch (IOException e) {
            System.err.println("Failed to load from file: " + filePath + "; " + e.getMessage());
            return new JSONObject();
        }
    }

    private void saveJsonToFile(JSONObject json, String filePath) {
        try (FileWriter file = new FileWriter(filePath)) {
            file.write(json.toString(2));
        } catch (IOException e) {
            System.err.println("Failed to save to file: " + filePath + "; " + e.getMessage());
        }
    }

    public User getUser(String username) {
        try {
            Map<String, User> users = loadRegisteredUsers();
            return users.getOrDefault(username, null);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    void updateRank(User player, int cardsChosenPoint) {
        // Load the JSON data from the file
        JSONObject usersJson = loadJsonFromFile(USER_DB_PATH);
        int current_rank = 0;

        // Check if the "users" array exists in the JSON data
        if (usersJson.has("users")) {
            JSONArray userList = usersJson.getJSONArray("users");

            // Loop through the users to find the specific user and update the rank
            for (int i = 0; i < userList.length(); i++) {
                JSONObject userJson = userList.getJSONObject(i);
                if (userJson.get("username").equals(player.getUsername())) {
                    current_rank = userJson.getInt("rank");
                    current_rank += cardsChosenPoint;

                    // Update the user's rank
                    userJson.put("rank", current_rank);

                    // Replace the updated user object back into the array
                    userList.put(i, userJson); // Use put(index, element) to update the existing element
                    break; // Exit the loop once the user is found and updated
                }
            }
        }

        // Save the updated JSON data back to the file
        saveJsonToFile(usersJson, USER_DB_PATH);
    }

}
