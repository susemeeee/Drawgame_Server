/*
 * net.Connection.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */
package net;

import UI.ServerFrame;
import datatype.Room;
import datatype.Target;
import datatype.User;
import datatype.packet.Packet;
import datatype.packet.PacketType;
import util.*;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Connection{
    private ServerSocketChannel server;
    private Selector selector;
    private Thread serverThread;
    private List<User> users;
    private EnumMap<PacketType, Parser> parserMap;
    private ByteBuffer buffer;
    private List<Room> rooms;

    public Connection(){
        users = Collections.synchronizedList(new CopyOnWriteArrayList<>());
        rooms = Collections.synchronizedList(new ArrayList<>());

        parserMap = new EnumMap(PacketType.class);
        parserMap.put(PacketType.LOGIN, new LoginPacketParser());
        parserMap.put(PacketType.REQUEST_ROOM, new RequestRoomPacketParser());
        parserMap.put(PacketType.MAKE_ROOM, new MakeRoomPacketParser());
        parserMap.put(PacketType.REQUEST_USER, new RequestUserPacketParser());
        parserMap.put(PacketType.JOIN_ROOM, new JoinRoomPacketParser());
        parserMap.put(PacketType.READY, new ReadyPacketParser());
        parserMap.put(PacketType.CHAT, new ChatPacketParser());
        parserMap.put(PacketType.START_REQUEST, new StartPacketParser());
        parserMap.put(PacketType.DISCONNECT, new DisconnectPacketParser());
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

                if(target == Target.SEND_TO_SENDER){
                    if(users.get(userIndex).isRequestedRoomData()){
                        Packet response = responseRoomData(Integer.parseInt(receivedPacket.get("page")));
                        users.get(userIndex).setRequestedRoomData(false);
                        sendOne(response, client);

                        ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.RESPONSE_ROOM);
                        ServerFrame.getInstance().appendLogLine("Page: " + receivedPacket.get("page"));
                        ServerFrame.getInstance().appendLogLine("Room count: " + response.getData("totalroom"));
                    }
                    else if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.REQUEST_USER){
                        Packet response = responseUserData(users.get(userIndex).getRoomNumber());
                        response.addData("yourID", Integer.toString(users.get(userIndex).getRoomUserID()));
                        sendOne(response, client);

                        ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.RESPONSE_USER);
                        ServerFrame.getInstance().appendLogLine("Room ID: " +
                                users.get(userIndex).getRoomNumber());
                        ServerFrame.getInstance().appendLogLine("Total users: " +
                                response.getData("totaluser"));
                        ServerFrame.getInstance().appendLogLine("Current users: " +
                                response.getData("currentuser"));
                    }
                    else if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.JOIN_ROOM){
                        int ID = Integer.parseInt(receivedPacket.get("id"));
                        Packet response = responseJoinRoomResult(ID, users.get(userIndex));
                        if(response.getData("result").equals("ACCEPT")){
                            users.get(userIndex).setPageNumber(-1);
                            users.get(userIndex).setRoomNumber(ID);
                            users.get(userIndex).setRoomUserID(generateRoomUserID(ID, users.get(userIndex)));
                        }
                        sendOne(response, client);

                        ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.JOIN_ROOM_RESULT);
                        ServerFrame.getInstance().appendLogLine("Result: " + response.getData("result"));

                        if(response.getData("result").equals("ACCEPT")){
                            for(User user : users){
                                if(user.getRoomNumber() == ID){
                                    response = responseUserData(ID);
                                    response.addData("yourID", Integer.toString(user.getRoomUserID()));
                                    sendOne(response, user.getSocketChannel());

                                    ServerFrame.getInstance().appendLogLine("Send type: " +
                                            PacketType.RESPONSE_USER);
                                    ServerFrame.getInstance().appendLogLine("Room ID: " +
                                            ID);
                                    ServerFrame.getInstance().appendLogLine("Total users: " +
                                            response.getData("totaluser"));
                                    ServerFrame.getInstance().appendLogLine("Current users: " +
                                            response.getData("currentuser"));
                                }
                            }
                        }
                    }
                }
                else if(target == Target.WAITING_USER){
                    if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.MAKE_ROOM){
                        int id = generateRoomID();
                        Room newRoom = new Room(id, receivedPacket.get("roomname"),
                                Integer.parseInt(receivedPacket.get("maxperson")),
                                Integer.parseInt(receivedPacket.get("maxround")));
                        rooms.add(newRoom);

                        users.get(userIndex).setRoomNumber(id);
                        users.get(userIndex).setRoomUserID(0);

                        ServerFrame.getInstance().appendLogLine("Add Room id " + id);
                        ServerFrame.getInstance().appendLogLine("Total rooms: " + rooms.size());

                        for(User user : users){
                            if(user.getPageNumber() != -1){
                                Packet response = responseRoomData(user.getPageNumber());
                                sendOne(response, user.getSocketChannel());

                                ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.RESPONSE_ROOM);
                                ServerFrame.getInstance().appendLogLine("Page: " + user.getPageNumber());
                                ServerFrame.getInstance().appendLogLine("Room count: " +
                                        response.getData("totalroom"));
                            }
                        }
                    }
                }
                else if(target == Target.ROOM_USER){
                    if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.READY){
                        users.get(userIndex).setReady(Boolean.parseBoolean(receivedPacket.get("status")));
                        Room room = findRoom(users.get(userIndex).getRoomNumber());
                        if(users.get(userIndex).isReady()){
                            room.setReadyUserCount(findRoom(users.get(userIndex).getRoomNumber()).getReadyUserCount() + 1);
                        }
                        else{
                            room.setReadyUserCount(findRoom(users.get(userIndex).getRoomNumber()).getReadyUserCount() - 1);
                        }

                        int ID = users.get(userIndex).getRoomNumber();
                        Packet response = responseReadyStatus(ID);
                        for(User user : users){
                            if(user.getRoomNumber() == ID){
                                sendOne(response, user.getSocketChannel());
                            }
                        }
                        ServerFrame.getInstance().appendLogLine("Send type: " +
                                PacketType.READY);
                        ServerFrame.getInstance().appendLogLine("Room ID: " +
                                ID);
                    }
                    else if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.CHAT){
                        int id = users.get(userIndex).getRoomNumber();

                        Packet response = new Packet(PacketType.CHAT);
                        response.addData("content", receivedPacket.get("content"));
                        response.addData("sender", users.get(userIndex).getName());

                        for(User user : users){
                            if(user.getRoomNumber() == id){
                                sendOne(response, user.getSocketChannel());
                            }
                        }

                        ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.CHAT);
                        ServerFrame.getInstance().appendLogLine("Chat content: " + receivedPacket.get("content"));
                        ServerFrame.getInstance().appendLogLine("Sender: " +
                                users.get(userIndex).getName());
                    }
                    else if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.START_REQUEST){
                        int ID = users.get(userIndex).getRoomNumber();
                        Room room = findRoom(ID);
                        boolean status = room.getReadyUserCount() == room.getCurrentUser();
                        Packet response;
                        for(User user : users){
                            if((user.getRoomNumber() == ID) && (user.getRoomUserID() == 0)){
                                response = new Packet(PacketType.START_REQUEST);
                                response.addData("status", Boolean.toString(status));
                                sendOne(response, user.getSocketChannel());
                                ServerFrame.getInstance().appendLogLine("Send type: " +
                                        PacketType.START_REQUEST);
                                ServerFrame.getInstance().appendLogLine("Status: " +
                                        response.getData("status"));
                            }
                            if((user.getRoomNumber() == ID) && status){
                                //TODO 여기서부터 게임 시작 또다른 패킷
                            }
                        }
                    }
                }
                else{//Target.NONE
                    if(PacketType.valueOf(receivedPacket.get("type")) == PacketType.DISCONNECT){
                        userDisconnected(users.get(userIndex));
                    }
                }
            }
        } catch (IOException e) {
            stopServer();
        }
    }

    private void sendOne(Packet data, SocketChannel client){
        try {
            Charset charset = StandardCharsets.UTF_8;
            buffer = charset.encode(data.toString());
            client.write(buffer);
        } catch (IOException e) {
            userDisconnected(users.get(findUser(client)));
        }
    }

    public void stopServer(){
        Packet packet = new Packet(PacketType.DISCONNECT);
        for(User user : users){
            sendOne(packet, user.getSocketChannel());
            userDisconnected(user);
        }
        try {
            server.close();
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void userDisconnected(User user){
        ServerFrame.getInstance().appendLogLine(user.getName() + " disconnect");
        if(user.getRoomNumber() != -1){
            //TODO 방 나가기 처리
        }
        try {
            user.getSocketChannel().close();
            users.remove(user);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int findUser(SocketChannel socketChannel){
        for(User user : users){
            if(user.getSocketChannel().equals(socketChannel)){
                return users.indexOf(user);
            }
        }
        return -1;
    }

    private Packet responseRoomData(int page){
        Packet packet = new Packet(PacketType.RESPONSE_ROOM);
        List<Room> result = new ArrayList<>();
        int count = 0;
        for(int i = 0; i < rooms.size(); i++){
            if((count >= page * 10 - 10) && (count < page * 10) && (!rooms.get(count).isGameStarted())){
                result.add(rooms.get(i));
                count++;
            }
            else if((count < page * 10 - 10) && !rooms.get(count).isGameStarted()){
                count++;
            }
            if(count >= page * 10){
                break;
            }
        }

        int roomIndex = 1;
        int roomCount = 0;
        for(Room r : result){
            packet.addData("room" + roomIndex + "_name", r.getName());
            packet.addData("room" + roomIndex + "_maxuser", Integer.toString(r.getMaxUser()));
            packet.addData("room" + roomIndex + "_currentuser", Integer.toString(r.getCurrentUser()));
            packet.addData("room" + roomIndex + "_id", Integer.toString(r.getRoomID()));
            roomIndex++;
            roomCount++;
        }
        packet.addData("totalroom", Integer.toString(roomCount));

        return packet;
    }

    private Packet responseUserData(int roomID){
        Packet packet = new Packet(PacketType.RESPONSE_USER);
        int count = 0;
        for(User user : users){
            if(user.getRoomNumber() == roomID){
                packet.addData("user" + user.getRoomUserID() + "_name", user.getName());
                packet.addData("user" + user.getRoomUserID() + "_id", Integer.toString(user.getRoomUserID()));
                packet.addData("user" + user.getRoomUserID() + "_characterIcon", user.getCharacterIcon());
                count++;
            }
        }
        Room r = findRoom(roomID);
        packet.addData("totaluser", Integer.toString(r.getMaxUser()));
        packet.addData("currentuser", Integer.toString(count));

        return packet;
    }

    private Packet responseJoinRoomResult(int ID, User client) {
        Room foundRoom = findRoom(ID);
        Packet response = new Packet(PacketType.JOIN_ROOM_RESULT);
        if (foundRoom == null) {
            response.addData("result", "NOT_FOUND");
        } else if (foundRoom.getCurrentUser() >= foundRoom.getMaxUser()) {
            response.addData("result", "MAX_USER");
        } else if (foundRoom.isGameStarted()) {
            response.addData("result", "GAME_STARTED");
        } else {
            response.addData("result", "ACCEPT");
            response.addData("userID", Integer.toString(generateRoomUserID(ID, client)));
            foundRoom.setCurrentUser(foundRoom.getCurrentUser() + 1);
        }

        return response;
    }

    private Packet responseReadyStatus(int ID){
        Packet packet = new Packet(PacketType.READY);
        for(User user : users){
            if(user.getRoomNumber() == ID){
                packet.addData("user" + user.getRoomUserID() + "_readystatus", Boolean.toString(user.isReady()));
            }
        }

        return packet;
    }

    private Room findRoom(int ID){
        for(Room r : rooms){
            if(r.getRoomID() == ID){
                return r;
            }
        }
        return null;
    }

    private int generateRoomID(){
        int id = 0;
        boolean isFind = false;
        while(rooms.size() != 0){
            for(Room r : rooms){
                if(r.getRoomID() == id){
                    isFind = true;
                    break;
                }
            }
            if(!isFind){
                break;
            }
            id++;
            if(id >= rooms.size()){
                break;
            }
        }
        return id;
    }

    private int generateRoomUserID(int roomID, User client){
        int id = 0;
        Room room = findRoom(roomID);
        boolean[] isGenerated = new boolean[room.getMaxUser()];

        for(int i = 0; i < isGenerated.length; i++){
            isGenerated[i] = false;
        }

        for(User user : users){
            if(user.getRoomNumber() == roomID && !user.equals(client)){
                isGenerated[user.getRoomUserID()] = true;
            }
        }

        for(int i = 0; i < isGenerated.length; i++){
            if(!isGenerated[i]){
                return i;
            }
        }
        return -1;
    }
}
