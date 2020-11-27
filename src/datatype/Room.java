/*
 * Room.java
 * Author : 박찬형
 * Created Date : 2020-11-14
 */
package datatype;

public class Room {
    private int roomID;
    private String name;
    private int maxUser;
    private int currentUser;
    private int totalRound;
    private volatile int currentRound;
    private volatile boolean gameStarted;
    private int readyUserCount;
    private volatile int testTakerID;
    private volatile String currentAnswer;
    private volatile boolean isRoundInProgress;
    private long roundStartTime;

    public Room(int ID, String name, int maxUser, int totalRound){
        this.roomID = ID;
        this.name = name;
        this.maxUser = maxUser;
        this.currentUser = 1;
        this.totalRound = totalRound;
        this.currentRound = 0;
        this.gameStarted = false;
        readyUserCount = 1;
        testTakerID = -1;
        currentAnswer = null;
        isRoundInProgress = false;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
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

    public int getReadyUserCount() {
        return readyUserCount;
    }

    public void setReadyUserCount(int readyUserCount) {
        this.readyUserCount = readyUserCount;
    }

    public int getTestTakerID() {
        return testTakerID;
    }

    public void setTestTakerID(int testTakerID) {
        this.testTakerID = testTakerID;
    }

    public String getCurrentAnswer() {
        return currentAnswer;
    }

    public void setCurrentAnswer(String currentAnswer) {
        this.currentAnswer = currentAnswer;
    }

    public boolean isRoundInProgress() {
        return isRoundInProgress;
    }

    public void setRoundInProgress(boolean roundInProgress) {
        isRoundInProgress = roundInProgress;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }
}
