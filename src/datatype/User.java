/*
 * datatype.User.java
 * Author : 박찬형
 * Created Date : 2020-11-10
 */
package datatype;


import javax.swing.*;
import java.nio.channels.SocketChannel;

public class User {
    private SocketChannel socketChannel;
    private String name;
    private ImageIcon characterIcon;
    private int pageNumber;
    private boolean isRequestedRoomData;

    public User(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
        pageNumber = -1;
        isRequestedRoomData = false;
    }

    public void login(){
        pageNumber = 1;
    }

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public void setSocketChannel(SocketChannel socketChannel) {
        this.socketChannel = socketChannel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ImageIcon getCharacterIcon() {
        return characterIcon;
    }

    public void setCharacterIcon(ImageIcon characterIcon) {
        this.characterIcon = characterIcon;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public boolean isRequestedRoomData() {
        return isRequestedRoomData;
    }

    public void setRequestedRoomData(boolean requestedRoomData) {
        isRequestedRoomData = requestedRoomData;
    }
}
