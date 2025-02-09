package Server;

import Shared.User;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class Game implements Runnable {

    private final List<User> players;
    private boolean isFinished;
    private final Database database;
    private final ReentrantLock databaseLock;
    private final ReentrantLock waitingQueueLock;

    private int mode;
    public Game(List<User> players, Database database, List<User> waitingQueue, ReentrantLock databaseLock, ReentrantLock waitingQueueLock, int mode) {
        this.players = players;
        this.database = database;
        this.isFinished = false;
        this.databaseLock = databaseLock;
        this.waitingQueueLock = waitingQueueLock;
        this.mode = mode;
    }

    private void createDecks(List<String> cardDeck) {
        List<String> suits = List.of("P", "E", "O", "C");
        while (cardDeck.size() < 52) {
            for (String suit : suits) {
                for (int j = 1; j <= 13; j++) {
                    cardDeck.add(j + suit);
                }
            }
        }
        Collections.shuffle(cardDeck);
    }

    @Override
    public void run() {
        try {
            System.out.println("Starting game with " + this.players.size() + " players");
            String winner = this.playGame();
            System.out.println("Game finished. Winner: " + winner);
        } catch (Exception exception) {
            System.out.println("Exception during the game! Connection closing: " + exception.getMessage());
        }
    }

    private String playGame() {
        if (this.players.size() < 2) {
            return "Not enough players";
        }

        List<String> cardDeck = new ArrayList<>();
        createDecks(cardDeck);

        String winner = "";
        int winnerScore = 0;
        int[] cardsChosenPoints = new int[this.players.size()];

        for (int round = 0; round < 4; round++) {
            for (int i = 0; i < this.players.size(); i++) {
                cardsChosenPoints[i] += this.takeCard(cardDeck);
            }
        }

        for (int i = 0; i < this.players.size(); i++) {
            User player = this.players.get(i);
            if(mode == 0) {
                player.updateRank(cardsChosenPoints[i]);

                databaseLock.lock();

                try {
                    this.database.updateRank(player, cardsChosenPoints[i]);
                } finally {
                    databaseLock.unlock();
                }
            }
            if (cardsChosenPoints[i] > winnerScore) {
                winner = player.getUsername() + " won with " + cardsChosenPoints[i] + " points.";
                winnerScore = cardsChosenPoints[i];
            }
        }

        return winner;
    }

    private int takeCard(List<String> cardDeck) {
        if (cardDeck.isEmpty()) {
            throw new IllegalStateException("Card deck is empty");
        }

        Random generator = new Random();
        int randomIndex = generator.nextInt(cardDeck.size());
        String card = cardDeck.remove(randomIndex);
        int cardValue = Integer.parseInt(card.substring(0, card.length() - 1));

        return switch (cardValue) {
            case 11 -> 21;
            case 12 -> 22;
            case 13 -> 23;
            case 14 -> 25;
            default -> cardValue;
        };
    }

    public List<User> getPlayers() {
        return players;
    }
    public boolean containsPlayer(User user) {
        return players.contains(user);
    }
    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished(boolean finished) {
        isFinished = finished;
    }

}
