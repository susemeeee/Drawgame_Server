/*
 * net.Connection.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */
package net;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;
import datatype.packet.Packet;
import datatype.packet.PacketType;
import util.DataMaker;
import util.LoginPacketParser;
import util.Parser;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.*;

public class Connection{
    private ServerSocketChannel server;
    private Selector selector;
    private Thread serverThread;
    private List<User> users;
    private EnumMap<PacketType, Parser> parserMap;
    private ByteBuffer buffer;

    private ByteArrayInputStream byteArrayInputStream;
    private ObjectInputStream objectInputStream;
    private ByteArrayOutputStream byteArrayOutputStream;
    private ObjectOutputStream objectOutputStream;

    public Connection(){
        users = Collections.synchronizedList(new ArrayList<>());

        buffer = ByteBuffer.allocate(1048576);
        try {
            byteArrayOutputStream = new ByteArrayOutputStream();
            objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            byteArrayInputStream = null;
            objectInputStream = null;

        } catch (IOException e) {
            e.printStackTrace();
        }

        parserMap = new EnumMap(PacketType.class);
        parserMap.put(PacketType.LOGIN, new LoginPacketParser());
    }

    public void startServer(String address, int port){
        try {
            selector = Selector.open();
            server = ServerSocketChannel.open();
            server.bind(new InetSocketAddress(address, port));
            server.configureBlocking(false);
            server.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ServerFrame.getInstance().appendLogLine("Server start.");
        ServerFrame.getInstance().appendLogLine("Address : " + server.socket().getLocalSocketAddress());

        serverThread = new Thread(() -> {
            while(true){
                try {
                    selector.select();
                    Iterator<?> keys = selector.selectedKeys().iterator();

                    while(keys.hasNext()){
                        SelectionKey key = (SelectionKey)keys.next();
                        keys.remove();

                        if(!key.isValid()){
                            continue;
                        }

                        if(key.isAcceptable()){
                            accept(key);
                            continue;
                        }
                        if(key.isReadable()){
                            read(key);
                            continue;
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        });
        serverThread.start();
    }

    private void accept(SelectionKey key){
        try {
            SocketChannel client = ((ServerSocketChannel)key.channel()).accept();

            if(client == null){
                return;
            }

            client.configureBlocking(false);
            client.register(selector, SelectionKey.OP_READ);

            ServerFrame.getInstance().appendLogLine("client connect.");
            users.add(new User(client));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key){
        ByteBuffer data = ByteBuffer.allocate(1048576);
        buffer = ByteBuffer.allocate(1048576);
        try {
            buffer.clear();
            data.clear();
            SocketChannel client = (SocketChannel)key.channel();
            while(client.read(data) > 0){
                data.flip();
                buffer.put(data);
                data = ByteBuffer.allocate(1048576);
            }
            buffer.flip();
            byte[] array = new byte[buffer.limit()];
            buffer.get(array, 0, buffer.limit());
            Map<String, String> receivedPacket = DataMaker.make(new String(array));

            int userIndex = findUser(client);
            Target target = Target.NONE;
            if(userIndex != -1){
                target = parserMap.get(PacketType.valueOf(receivedPacket.get("type")))
                        .parse(users.get(userIndex), receivedPacket);
            }

            ServerFrame.getInstance().appendImage(users.get(userIndex).getCharacterIcon());

//            if(target == Target.BROADCAST){
//                //TODO 브로드캐스트
//            }
        } catch (IOException e) {
            //TODO 연결 끊어짐
            e.printStackTrace();
            System.exit(1); //temp
        }
    }

    private void sendOne(Packet data, SocketChannel client){
        try {
            objectOutputStream.writeObject(data);
            objectOutputStream.flush();
            client.write(ByteBuffer.wrap(byteArrayOutputStream.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer(){
        //TODO stop버튼 누를 시 서버 중지`
    }

    private int findUser(SocketChannel socketChannel){
        for(User user : users){
            if(user.getSocketChannel().equals(socketChannel)){
                return users.indexOf(user);
            }
        }
        return -1;
    }
}
