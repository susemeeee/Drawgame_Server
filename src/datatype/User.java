/*
 * datatype.User.java
 * Author : 박찬형
 * Created Date : 2020-11-10
 */
package datatype;

import java.nio.channels.SocketChannel;

public class User {
    private SocketChannel socketChannel;
    private String name;
    private String characterIcon;
    private int pageNumber;
    private int roomNumber;
    private int roomUserID;
    private boolean isReady;
    private int score;

    public User(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
        pageNumber = -1;
        roomNumber = -1;
        roomUserID = -1;
        isReady = false;
        score = 0;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCharacterIcon() {
        return characterIcon;
    }

    public void setCharacterIcon(String characterIcon) {
        this.characterIcon = characterIcon;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getRoomNumber() {
        return roomNumber;
    }

    public void setRoomNumber(int roomNumber) {
        this.roomNumber = roomNumber;
    }

    public int getRoomUserID() {
        return roomUserID;
    }

    public void setRoomUserID(int roomUserID) {
        this.roomUserID = roomUserID;
    }

    public boolean isReady() {
        return isReady;
    }

    public void setReady(boolean ready) {
        isReady = ready;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
