package Server;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import Shared.User;

public class ServerGUI extends JFrame {
    private JTextArea activeConnectionsTextArea;
    private JTextArea waitingQueueTextArea;
    private JTextArea activeGamesTextArea;
    private JTextArea serverLogTextArea;
    private Timer updateTimer;
    private GameServer gameServer;

    public ServerGUI(GameServer gameServer) {
        this.gameServer = gameServer;
        setTitle("Server Status");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new GridLayout(1, 2));

        // Left side panel for real-time information
        JPanel realTimeInfoPanel = new JPanel(new GridLayout(3, 1));

        activeConnectionsTextArea = new JTextArea();
        activeConnectionsTextArea.setEditable(false);
        JPanel activeConnectionsPanel = new JPanel(new BorderLayout());
        activeConnectionsPanel.setBorder(BorderFactory.createTitledBorder("Active Connections"));
        activeConnectionsPanel.add(new JScrollPane(activeConnectionsTextArea), BorderLayout.CENTER);

        waitingQueueTextArea = new JTextArea();
        waitingQueueTextArea.setEditable(false);
        JPanel waitingQueuePanel = new JPanel(new BorderLayout());
        waitingQueuePanel.setBorder(BorderFactory.createTitledBorder("Waiting Queue"));
        waitingQueuePanel.add(new JScrollPane(waitingQueueTextArea), BorderLayout.CENTER);

        activeGamesTextArea = new JTextArea();
        activeGamesTextArea.setEditable(false);
        JPanel activeGamesPanel = new JPanel(new BorderLayout());
        activeGamesPanel.setBorder(BorderFactory.createTitledBorder("Active Games"));
        activeGamesPanel.add(new JScrollPane(activeGamesTextArea), BorderLayout.CENTER);

        realTimeInfoPanel.add(activeConnectionsPanel);
        realTimeInfoPanel.add(waitingQueuePanel);
        realTimeInfoPanel.add(activeGamesPanel);

        // Right side panel for server log
        serverLogTextArea = new JTextArea();
        serverLogTextArea.setEditable(false);
        JPanel serverLogPanel = new JPanel(new BorderLayout());
        serverLogPanel.setBorder(BorderFactory.createTitledBorder("Server Log"));
        serverLogPanel.add(new JScrollPane(serverLogTextArea), BorderLayout.CENTER);

        mainPanel.add(realTimeInfoPanel);
        mainPanel.add(serverLogPanel);

        add(mainPanel);

        // Timer to update the GUI every 2 seconds
        updateTimer = new Timer(2000, e -> updateServerStatus());
        updateTimer.start();
    }

    private void updateServerStatus() {
        updateActiveConnections();
        updateWaitingQueue();
        updateActiveGames();
        updateServerLog();
    }

    private void updateActiveConnections() {
        StringBuilder activeConnections = new StringBuilder();
        ConcurrentHashMap<String, ActiveConnection> connections = gameServer.getActiveConnections();
        for (String username : connections.keySet()) {
            activeConnections.append("Username: ").append(username).append("\n");
        }
        activeConnectionsTextArea.setText(activeConnections.toString());
    }

    private void updateWaitingQueue() {
        StringBuilder waitingQueue = new StringBuilder();
        ReentrantLock queueLock = gameServer.getWaitingQueueLock();
        List<User> queue = gameServer.getWaitingQueue();
        queueLock.lock();
        try {
            for (User user : queue) {
                waitingQueue.append("Username: ").append(user.getUsername()).append("\n");
            }
        } finally {
            queueLock.unlock();
        }
        waitingQueueTextArea.setText(waitingQueue.toString());
    }

    private void updateActiveGames() {
        StringBuilder activeGames = new StringBuilder();
        List<Game> games = gameServer.getActiveGames();
        int i = 0;
        for (Game game : games) {
            activeGames.append("Game ID: ").append(i).append("\n");
            for (User player : game.getPlayers()) {
                activeGames.append(" - ").append(player.getUsername()).append("\n");
            }
            i++;
        }
        activeGamesTextArea.setText(activeGames.toString());
    }

    private void updateServerLog() {
        StringBuilder serverLog = new StringBuilder();
        List<String> logs = gameServer.getServerLogs();
        for (String log : logs) {
            serverLog.append(log).append("\n");
        }
        serverLogTextArea.setText(serverLog.toString());
    }
}
