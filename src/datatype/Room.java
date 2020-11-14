/*
 * Room.java
 * Author : 박찬형
 * Created Date : 2020-11-14
 */
package datatype;

public class Room {
    private String name;
    private int maxUser;
    private int currentUser;
    private int totalRound;
    private int currentRound;
    private boolean gameStarted;

    public Room(String name, int maxUser, int totalRound){
        this.name = name;
        this.maxUser = maxUser;
        this.currentUser = 1;
        this.totalRound = totalRound;
        this.currentRound = 0;
        this.gameStarted = false;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxUser() {
        return maxUser;
    }

    public void setMaxUser(int maxUser) {
        this.maxUser = maxUser;
    }

    public int getCurrentUser() {
        return currentUser;
    }

    public void setCurrentUser(int currentUser) {
        this.currentUser = currentUser;
    }

    public int getTotalRound() {
        return totalRound;
    }

    public void setTotalRound(int totalRound) {
        this.totalRound = totalRound;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted = gameStarted;
    }
}
