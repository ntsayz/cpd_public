package Server;

import Shared.User;

import java.io.PrintWriter;
import java.net.Socket;

public class ActiveConnection {
    private User user;
    private Socket socket;
    private PrintWriter out;

    public ActiveConnection(User user, Socket socket, PrintWriter out) {
        this.user = user;
        this.socket = socket;
        this.out = out;
    }

    public User getUser() {
        return user;
    }

    public Socket getSocket() {
        return socket;
    }

    public PrintWriter getOut() {
        return out;
    }
}
