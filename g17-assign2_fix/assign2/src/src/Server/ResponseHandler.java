package Server;

import Shared.User;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Objects;

public interface ResponseHandler {
    void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket)throws IOException;
    static String extractParam(String response, String paramName) {
        // Build the key to look for in the response
        String key = paramName.toUpperCase() + "=";  // Ensure paramName is in uppercase
        int start = response.indexOf(key);
        if (start == -1) return null;  // Parameter not found in the response

        start += key.length();  // Move the start index past the key
        int end = response.indexOf(" ", start);  // Find the next space after the parameter value starts
        if (end == -1) end = response.length();  // If no space is found, assume the parameter goes till the end of the string

        return response.substring(start, end).trim();  // Extract and return the parameter value
    }
}

class LoginHandler implements ResponseHandler {
    @Override
    public void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket) throws IOException {
        if (user != null) {
            if (server.addActiveConnection(user, clientSocket, out)) {
                String token = server.getTokenManager().createToken(user.getUsername(), out);
                String rankings = server.getRankings();
                boolean isInGame = server.isGameStartedForUser(user);
                String rejoin = isInGame ? " REJOIN=true" : "";
                out.println("LOGIN - STATUS=OK MESSAGE=Authentication successful. TOKEN=" + token + " RANKINGS=" + rankings + rejoin);
            } else {
                out.println("LOGIN - STATUS=FAIL MESSAGE=User is already logged in from another device.");
            }
        } else {
            out.println("LOGIN - STATUS=FAIL MESSAGE=Authentication failed.");
        }
    }
}



class RegisterHandler implements ResponseHandler {
    @Override
    public void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket) throws IOException {
        Map<String, String> params = server.parseParams(line.split(" "));
        String username = params.get("USERNAME");
        String password = params.get("PASSWORD");

        if (username == null || password == null) {
            out.println("REGISTER - STATUS=FAIL MESSAGE=Username or password missing.");
            return;
        }

        if (server.getDatabase().getUser(username) != null) {
            out.println("REGISTER - STATUS=FAIL MESSAGE=Username already exists.");
            return;
        }

        if (password.length() <= 8) {
            out.println("REGISTER - STATUS=FAIL MESSAGE=Password must be longer than 8 characters.");
            return;
        }

        User newUser = new User(username, password, null, 0);
        server.getDatabase().addUser(newUser);
        String token = server.getTokenManager().createToken(username, out);
        server.addActiveConnection(newUser, clientSocket, out);

        out.println("REGISTER - STATUS=OK TOKEN=" + token);
    }
}



class LogoutHandler implements ResponseHandler {
    @Override
    public void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket) {
        String token = ResponseHandler.extractParam(line, "TOKEN");

        if (token != null && server.getTokenManager().validateToken(token)) {
            server.getTokenManager().expireToken(token);
            if (user != null) {
                server.removeActiveConnection(user);
            }
            out.println("LOGOUT - STATUS=OK MESSAGE=Logged out successfully.");
            assert user != null;
            server.logMessage("User " + user.getUsername() + " logged out.");

        } else {
            out.println("LOGOUT - STATUS=EXPIRED MESSAGE=Invalid or expired token.");
        }
    }
}


class PlayGameHandler implements ResponseHandler {
    @Override
    public void handleRequest(String content, PrintWriter out, GameServer server, User user, Socket clientSocket) {
        if (user != null) {
            // Check if the user is already in an active game
            if (server.isGameStartedForUser(user)) {
                out.println("PING - STATUS=PLAY MESSAGE=Game started");
            } else {
                // Proceed as usual if the user is not in an active game
                int mode = Integer.parseInt(Objects.requireNonNull(ResponseHandler.extractParam(content, "MODE")));
                System.out.println(mode);
                server.addQueueList(user, mode);
                out.println("PLAY_GAME - STATUS=WAIT MESSAGE=Added to the game queue");
            }
        } else {
            out.println("PLAY_GAME - STATUS=FAILED MESSAGE=User not authenticated");
        }
    }
}



class PingHandler implements ResponseHandler {
    @Override
    public void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket) throws IOException {
        if (user != null) {
            Game game = server.getGameForUser(user);
            if (game != null) {
                if (game.isFinished()) {
                    server.removeActiveGame(user);
                    out.println("END_GAME - STATUS=OK MESSAGE=Game finished");
                    server.logMessage(user.getUsername() + " was removed from a game for insufficient players");
                    server.logMessage("A game was terminated");
                } else {
                    out.println("PING - STATUS=PLAY MESSAGE=Game started");
                }
            } else {
                out.println("PING - STATUS=WAIT MESSAGE=Still waiting");
            }
        } else {
            out.println("PING - STATUS=FAILED MESSAGE=User not authenticated");
        }
    }
}


class EndGameHandler implements ResponseHandler {
    @Override
    public void handleRequest(String line, PrintWriter out, GameServer server, User user, Socket clientSocket) throws IOException {
        if (user != null) {
            Game game = server.getGameForUser(user);
            if (game != null) {
                game.setFinished(true);
                server.logMessage(user.getUsername() + " exited a game");
                out.println("END_GAME - STATUS=OK MESSAGE=Game finished");
            } else {
                out.println("END_GAME - STATUS=OK MESSAGE=Game finished");
            }
        } else {
            out.println("END_GAME - STATUS=FAILED MESSAGE=User not authenticated");
        }
    }
}





