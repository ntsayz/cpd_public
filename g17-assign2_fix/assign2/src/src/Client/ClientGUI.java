package Client;

import Shared.User;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Map;

public class ClientGUI extends JFrame {
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextArea messageArea;
    Map<String, Integer> rankings;
    private JTextArea rankingTextArea;
    private User currentUser;

    public ClientGUI() {
        setTitle("Login or Register");
        setSize(500, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        add(mainPanel);
        showLoginScreen();
    }

    void showLoginScreen() {
        JPanel loginPanel = new JPanel(new BorderLayout());

        JPanel formPanel = new JPanel(new GridLayout(3, 2));
        JLabel usernameLabel = new JLabel("Username:");
        JLabel passwordLabel = new JLabel("Password:");

        usernameField = new JTextField();
        passwordField = new JPasswordField();

        JButton loginButton = new JButton("Login");
        JButton registerButton = new JButton("Register");

        formPanel.add(usernameLabel);
        formPanel.add(usernameField);
        formPanel.add(passwordLabel);
        formPanel.add(passwordField);
        formPanel.add(loginButton);
        formPanel.add(registerButton);

        loginButton.addActionListener(e -> handleLogin());
        registerButton.addActionListener(e -> handleRegister());

        messageArea = new JTextArea(2, 20);
        messageArea.setEditable(false);

        loginPanel.add(formPanel, BorderLayout.CENTER);
        loginPanel.add(messageArea, BorderLayout.SOUTH);

        mainPanel.add(loginPanel, "loginPanel");
        cardLayout.show(mainPanel, "loginPanel");
    }

    public void showGameOptions(User user, Map<String, Integer> rankings, boolean r) {
        this.currentUser = user;

        if(rankings != null){
            this.rankings = rankings;
        }

        JPanel gameOptionsPanel = new JPanel(new BorderLayout());
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton simpleGameButton;
        JButton rankedGameButton;
        JButton logoutButton;
        if (r){
            simpleGameButton = new JButton("Rejoin game");
            rankedGameButton = new JButton("Play ranked game");
            logoutButton = new JButton("Logout");
        }else {
            simpleGameButton = new JButton("Play simple game");
            rankedGameButton = new JButton("Play ranked game");
            logoutButton = new JButton("Logout");
        }


        simpleGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        rankedGameButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        logoutButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        simpleGameButton.addActionListener(e -> startSimpleGame());
        rankedGameButton.addActionListener(e -> startRankedGame());
        logoutButton.addActionListener(e -> handleLogout());

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(simpleGameButton);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(rankedGameButton);
        centerPanel.add(Box.createVerticalStrut(10));
        centerPanel.add(logoutButton);
        centerPanel.add(Box.createVerticalGlue());

        gameOptionsPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel rankingPanel = new JPanel();
        rankingPanel.setLayout(new BorderLayout());
        rankingPanel.setBorder(BorderFactory.createTitledBorder("User Rankings"));

        rankingTextArea = new JTextArea();
        rankingTextArea.setEditable(false);
        StringBuilder rankingsText = new StringBuilder();
        this.rankings.forEach((username, rank) -> rankingsText.append(username).append(": ").append(rank).append("\n"));
        rankingTextArea.setText(rankingsText.toString().trim());
        rankingPanel.add(new JScrollPane(rankingTextArea), BorderLayout.CENTER);

        gameOptionsPanel.add(rankingPanel, BorderLayout.EAST);

        // Add message area at the bottom of the game options panel
        messageArea = new JTextArea(2, 20);
        messageArea.setEditable(false);
        gameOptionsPanel.add(messageArea, BorderLayout.SOUTH);

        mainPanel.add(gameOptionsPanel, "gameOptionsPanel");
        cardLayout.show(mainPanel, "gameOptionsPanel");
    }

    public void showWaitingScreen() {
        JPanel waitingPanel = new JPanel(new BorderLayout());
        JLabel waitingLabel = new JLabel("Waiting for a game to start...");
        waitingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        waitingPanel.add(waitingLabel, BorderLayout.CENTER);

        mainPanel.add(waitingPanel, "waitingPanel");
        cardLayout.show(mainPanel, "waitingPanel");

        // Start sending ping requests
        new Timer(1000, e -> sendPingRequest()).start();
    }

    public void showGameScreen() {
        JPanel gamePanel = new JPanel(new BorderLayout());
        JLabel gameLabel = new JLabel("You are in the game!");
        gameLabel.setHorizontalAlignment(SwingConstants.CENTER);
        JButton gameButton = new JButton("Game Action");
        // simpleGameButton.addActionListener(e -> startSimpleGame());

        gamePanel.add(gameLabel, BorderLayout.CENTER);
        gamePanel.add(gameButton, BorderLayout.SOUTH);

        gameButton.addActionListener(e -> sendEndGame());

        mainPanel.add(gamePanel, "gamePanel");
        cardLayout.show(mainPanel, "gamePanel");
    }

    private void sendPingRequest() {
        try {
            ClientConnect.sendRequest("PING - STATUS=WAITING GAME TOKEN=" + ClientConnect.sessionToken, currentUser.getUsername());
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void sendEndGame(){
        try {
            ClientConnect.sendRequest("END_GAME - STATUS=END TOKEN=" + ClientConnect.sessionToken, currentUser.getUsername());
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            ClientConnect.sendRequest("LOGIN - USERNAME=" + username + " PASSWORD=" + password, username);
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void handleRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());

        try {
            ClientConnect.sendRequest("REGISTER - USERNAME=" + username + " PASSWORD=" + password, username);
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void handleLogout() {
        try {
            ClientConnect.sendRequest("LOGOUT - TOKEN=" + ClientConnect.sessionToken, currentUser.getUsername());
            showLoginScreen();
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void startSimpleGame() {
        int mode = 1;
        try {
            ClientConnect.sendRequest("PLAY_GAME - TOKEN=" + ClientConnect.sessionToken + " MODE=" + mode, currentUser.getUsername());
            displayMessage("Adding to the game queue...");
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    private void startRankedGame() {
        int mode = 0;
        try {
            ClientConnect.sendRequest("PLAY_GAME - TOKEN=" + ClientConnect.sessionToken + " MODE=" + mode, currentUser.getUsername());
            displayMessage("Adding to the game queue...");
        } catch (IOException e) {
            displayMessage("Error: " + e.getMessage());
        }
    }

    public void displayMessage(String message) {
        messageArea.setText(message);
    }
}
