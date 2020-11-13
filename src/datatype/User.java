/*
 * datatype.User.java
 * Author : 박찬형
 * Created Date : 2020-11-10
 */
package datatype;


import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class User {
    private SocketChannel socketChannel;
    private String name;
    private ImageIcon characterIcon;

    public User(SocketChannel socketChannel){
        this.socketChannel = socketChannel;
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
}
