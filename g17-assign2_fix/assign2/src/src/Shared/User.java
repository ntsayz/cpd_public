package Shared;

import java.nio.channels.SocketChannel;

public class User {
    private String token;
    private String username;
    private String password;
    private int rank;
    private SocketChannel socket;

    public User(String username, String password, String token, Integer rank) {
        this.username = username;
        this.password = password;
        this.token = token;
        this.rank = rank;
    }

    public String getUsername() {
        return username;
    }
    public String getPassword() {
        return password;
    }
    public int getRank() {
        return this.rank;
    }
    public String getToken() {
        return this.token;
    }
    public SocketChannel getSocket() { return this.socket; }
    public void setSocket (SocketChannel socket) { this.socket = socket; }
    public void updateRank(int rank) {this.rank += rank; }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        User user = (User) obj;
        return username.equals(user.username);
    }

    @Override
    public int hashCode() {
        return username.hashCode();
    }


}