/*
 * net.Connection.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */
package net;

import UI.ServerFrame;
import datatype.Room;
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
import java.util.concurrent.ThreadLocalRandom;

public class Connection{
    private ServerSocketChannel server;
    private Selector selector;
    private Thread serverThread;
    private List<User> users;
    private ByteBuffer buffer;
    private List<Room> rooms;
    private WordGenerator wordGenerator;

    public Connection(){
        users = Collections.synchronizedList(new CopyOnWriteArrayList<>());
        rooms = Collections.synchronizedList(new CopyOnWriteArrayList<>());
        wordGenerator = new WordGenerator();
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
                    selector.select(100);
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

                for(Room room : rooms){
                    if(room.isGameStarted()){
                        List<Integer> IDList = getIDList(room);
                        if(!room.isRoundInProgress()){
                            if(room.getCurrentRound() >= room.getTotalRound()){
                                Packet packet = getGameResult(room);
                                room.setRoundStartTime(0L);
                                room.setCurrentRound(0);
                                room.setRoundInProgress(false);
                                room.setGameStarted(false);
                                room.setReadyUserCount(1);

                                for(User user : users){
                                    if(user.getRoomNumber() == room.getRoomID()){
                                        sendOne(packet, user.getSocketChannel());
                                    }
                                }
                                ServerFrame.getInstance().appendLogLine(
                                        "Send type: " + PacketType.END_GAME);
                                continue;
                            }

                            room.setCurrentRound(room.getCurrentRound() + 1);
                            room.setTestTakerID(IDList.get(ThreadLocalRandom.current()
                                    .nextInt(0, IDList.size())));
                            room.setCurrentAnswer(wordGenerator.generateRandomWord());
                            Packet packet = new Packet(PacketType.START_ROUND);
                            long curTime = System.currentTimeMillis();
                            packet.addData("round", Integer.toString(room.getCurrentRound()));
                            packet.addData("time", Long.toString(curTime));
                            for(User user : users){
                                if(room.getRoomID() == user.getRoomNumber()){
                                    if(room.getTestTakerID() == user.getRoomUserID()){
                                        packet.addData("testtaker", Boolean.toString(true));
                                        packet.addData("word", room.getCurrentAnswer());
                                    }
                                    else{
                                        packet.addData("testtaker", Boolean.toString(false));
                                        packet.addData("word", "null");
                                    }
                                    sendOne(packet, user.getSocketChannel());
                                    ServerFrame.getInstance().appendLogLine(
                                            "Send type: " + PacketType.START_ROUND);
                                }
                            }
                            room.setRoundStartTime(curTime);
                            room.setRoundInProgress(true);
                        }
                        else {
                            if((!IDList.contains(room.getTestTakerID())) ||
                                    ((System.currentTimeMillis() - room.getRoundStartTime()) >= 60000)){
                                Packet packet = new Packet(PacketType.END_ROUND);
                                room.setTestTakerID(-1);
                                room.setCurrentAnswer(null);
                                for(User user : users){
                                    if(room.getRoomID() == user.getRoomNumber()){
                                        sendOne(packet, user.getSocketChannel());
                                        ServerFrame.getInstance().appendLogLine(
                                                "Send type: " + PacketType.END_ROUND);
                                    }
                                }
                                room.setRoundInProgress(false);
                            }
                        }
                    }
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
        ByteBuffer data = ByteBuffer.allocate(1024);
        buffer = ByteBuffer.allocate(1048576);
        try {
            buffer.clear();
            data.clear();
            SocketChannel client = (SocketChannel)key.channel();
            while(client.read(data) > 0){
                data.flip();
                buffer.put(data);
                data = ByteBuffer.allocate(1024);
            }
            buffer.flip();
            byte[] array = new byte[buffer.limit()];
            buffer.get(array, 0, buffer.limit());
            Map<String, String> receivedPacket = DataMaker.make(new String(array));

            int userIndex = findUser(client);
            if(userIndex != -1 && !receivedPacket.isEmpty()){
                PacketType type = PacketType.valueOf(receivedPacket.get("type"));

                if(type == PacketType.LOGIN){
                    User user = users.get(userIndex);
                    user.setName(receivedPacket.get("name"));
                    user.setCharacterIcon(receivedPacket.get("characterIcon"));

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Name : " + receivedPacket.get("name"));
                    messages.add("CharacterIcon: IMAGE(len: " + receivedPacket.get("characterIcon").length() + ")");
                    log(messages);
                }
                else if(type == PacketType.REQUEST_ROOM){
                    User user = users.get(userIndex);
                    user.setPageNumber(Integer.parseInt(receivedPacket.get("page")));

                    Packet response = responseRoomData(Integer.parseInt(receivedPacket.get("page")));
                    sendOne(response, client);

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Page number: " + receivedPacket.get("page"));
                    messages.add("Send type: " + PacketType.RESPONSE_ROOM);
                    messages.add("Page: " + receivedPacket.get("page"));
                    messages.add("Room count: " + response.getData("totalroom"));
                    log(messages);
                }
                else if(type == PacketType.MAKE_ROOM){
                    User curUser = users.get(userIndex);
                    curUser.setPageNumber(-1);
                    int id = generateRoomID();
                    Room newRoom = new Room(id, receivedPacket.get("roomname"),
                            Integer.parseInt(receivedPacket.get("maxperson")),
                            Integer.parseInt(receivedPacket.get("maxround")));
                    rooms.add(newRoom);
                    users.get(userIndex).setRoomNumber(id);
                    users.get(userIndex).setRoomUserID(0);

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Room name: " + receivedPacket.get("roomname"));
                    messages.add("Max person: " + receivedPacket.get("maxperson"));
                    messages.add("Round: " + receivedPacket.get("maxround"));
                    messages.add("Add Room id " + id);
                    messages.add("Total rooms: " + rooms.size());
                    log(messages);
                    messages = new ArrayList<>();

                    for(User user : users){
                        if(user.getPageNumber() != -1){
                            Packet response = responseRoomData(user.getPageNumber());
                            sendOne(response, user.getSocketChannel());

                            messages.add("Send type: " + PacketType.RESPONSE_ROOM);
                            messages.add("Page: " + user.getPageNumber());
                            messages.add("Room count: " + response.getData("totalroom"));
                        }
                    }
                    log(messages);
                }
                else if(type == PacketType.REQUEST_USER){
                    Packet response = responseUserData(users.get(userIndex).getRoomNumber());
                    response.addData("yourID", Integer.toString(users.get(userIndex).getRoomUserID()));
                    sendOne(response, client);

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Send type: " + PacketType.RESPONSE_USER);
                    messages.add("Room ID: " + users.get(userIndex).getRoomNumber());
                    messages.add("Total users: " + response.getData("totaluser"));
                    messages.add("Current users: " + response.getData("currentuser"));
                    log(messages);
                }
                else if(type == PacketType.JOIN_ROOM){
                    int ID = Integer.parseInt(receivedPacket.get("id"));
                    Packet response = responseJoinRoomResult(ID, users.get(userIndex));
                    if(response.getData("result").equals("ACCEPT")){
                        users.get(userIndex).setPageNumber(-1);
                        users.get(userIndex).setRoomNumber(ID);
                        users.get(userIndex).setRoomUserID(generateRoomUserID(ID, users.get(userIndex)));
                    }
                    sendOne(response, client);

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Room ID: " + receivedPacket.get("id"));
                    messages.add("Send type: " + PacketType.JOIN_ROOM_RESULT);
                    messages.add("Result: " + response.getData("result"));
                    log(messages);

                    if(response.getData("result").equals("ACCEPT")){
                        messages = new ArrayList<>();
                        for(User user : users){
                            if(user.getRoomNumber() == ID){
                                response = responseUserData(ID);
                                response.addData("yourID", Integer.toString(user.getRoomUserID()));
                                sendOne(response, user.getSocketChannel());

                                messages.add("Send type: " + PacketType.RESPONSE_USER);
                                messages.add("Room ID: " + ID);
                                messages.add("Total users: " + response.getData("totaluser"));
                                messages.add("Current users: " + response.getData("currentuser"));
                            }
                        }
                        log(messages);
                    }
                }
                else if(type == PacketType.READY){
                    if(!receivedPacket.get("status").equals("request")){
                        users.get(userIndex).setReady(Boolean.parseBoolean(receivedPacket.get("status")));
                        Room room = findRoom(users.get(userIndex).getRoomNumber());
                        assert room != null;
                        if(users.get(userIndex).isReady()){
                            room.setReadyUserCount(findRoom(users.get(userIndex).getRoomNumber())
                                    .getReadyUserCount() + 1);
                        }
                        else{
                            room.setReadyUserCount(findRoom(users.get(userIndex).getRoomNumber())
                                    .getReadyUserCount() - 1);
                        }
                    }

                    int ID = users.get(userIndex).getRoomNumber();
                    Packet response = responseReadyStatus(ID);
                    for(User user : users){
                        if(user.getRoomNumber() == ID){
                            sendOne(response, user.getSocketChannel());
                        }
                    }

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Ready status: " + receivedPacket.get("status"));
                    messages.add("Send type: " + PacketType.READY);
                    messages.add("Room ID: " + ID);
                    log(messages);
                }
                else if(type == PacketType.CHAT){
                    int id = users.get(userIndex).getRoomNumber();

                    Packet response = new Packet(PacketType.CHAT);
                    response.addData("content", receivedPacket.get("content"));
                    response.addData("sender", users.get(userIndex).getName());

                    for(User user : users){
                        if(user.getRoomNumber() == id){
                            sendOne(response, user.getSocketChannel());
                        }
                    }

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));
                    messages.add("Chat content: " + receivedPacket.get("content"));
                    messages.add("Send type: " + PacketType.CHAT);
                    messages.add("Chat content: " + receivedPacket.get("content"));
                    messages.add("Sender: " + users.get(userIndex).getName());
                    log(messages);

                    Room room = findRoom(users.get(userIndex).getRoomNumber());
                    assert room != null;
                    if(room.isGameStarted() && room.isRoundInProgress() &&
                            room.getCurrentAnswer().equals(receivedPacket.get("content")) &&
                            (users.get(userIndex).getRoomUserID() != room.getTestTakerID())){
                        Packet packet = new Packet(PacketType.END_ROUND);
                        packet.addData("score", users.get(userIndex).getName());
                        users.get(userIndex).setScore(users.get(userIndex).getScore() + 1);
                        room.setTestTakerID(-1);
                        room.setCurrentAnswer(null);
                        for(User user : users){
                            if(room.getRoomID() == user.getRoomNumber()){
                                sendOne(packet, user.getSocketChannel());
                                ServerFrame.getInstance().appendLogLine(
                                        "Send type: " + PacketType.END_ROUND);
                            }
                        }
                        room.setRoundInProgress(false);
                    }
                }
                else if(type == PacketType.START_REQUEST){
                    int ID = users.get(userIndex).getRoomNumber();
                    Room room = findRoom(ID);
                    assert room != null;
                    boolean status = (room.getReadyUserCount() != 1) && (room.getReadyUserCount() == room.getCurrentUser());

                    List<String> messages = new ArrayList<>();
                    messages.add("Received type: " + receivedPacket.get("type"));

                    Packet response;
                    for(User user : users){
                        if((user.getRoomNumber() == ID) && (user.getRoomUserID() == 0)){
                            response = new Packet(PacketType.START_REQUEST);
                            response.addData("status", Boolean.toString(status));
                            sendOne(response, user.getSocketChannel());

                            messages.add("Send type: " + PacketType.START_REQUEST);
                            messages.add("Status: " + response.getData("status"));
                        }
                        if((user.getRoomNumber() == ID) && status){
                            response = new Packet(PacketType.START_GAME);
                            sendOne(response, user.getSocketChannel());

                            messages.add("Send type: " + PacketType.START_GAME);
                        }
                    }
                    if(status){
                        room.setGameStarted(true);
                    }
                    log(messages);
                }
                else if(type == PacketType.DISCONNECT){
                    userDisconnected(users.get(userIndex));

                    ServerFrame.getInstance().appendLogLine("Received type: " + receivedPacket.get("type"));
                }
                else if(type == PacketType.QUIT_ROOM){
                    userQuited(userIndex);

                    ServerFrame.getInstance().appendLogLine("Received type: " + receivedPacket.get("type"));
                }
                else if(type == PacketType.DRAW){
                    ServerFrame.getInstance().appendLogLine("Received type: " + receivedPacket.get("type"));

                    Packet packet = new Packet(PacketType.DRAW);
                    packet.addData("image", receivedPacket.get("image"));

                    List<String> messages = new ArrayList<>();
                    messages.add("Send type: " + PacketType.DRAW);
                    messages.add("Draw: IMAGE(len: " + receivedPacket.get("image").length() + ")");
                    int count = 0;
                    for(User user : users){
                        if((user.getRoomNumber() == users.get(userIndex).getRoomNumber()) &&
                                (user.getRoomUserID() != findRoom(user.getRoomNumber()).getTestTakerID())){
                            sendOne(packet, user.getSocketChannel());
                            count++;
                        }
                    }
                    messages.add("Send user count: " + count);
                    log(messages);
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
            buffer.rewind();
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
            if(server != null){
                server.close();
            }
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    private void userDisconnected(User user){
        ServerFrame.getInstance().appendLogLine(user.getName() + " disconnect");
        if(user.getRoomNumber() != -1){
            userQuited(findUser(user.getSocketChannel()));
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

    private void userQuited(int userIndex){
        User user = users.get(userIndex);
        Room room = findRoom(user.getRoomNumber());
        int ID = user.getRoomNumber();
        if(user.getRoomUserID() == 0 && (room.getCurrentUser() != 1)){
            int findTarget = 1;
            boolean isFind = false;
            while(!isFind){
                for(User u : users){
                    if((u.getRoomNumber() == room.getRoomID()) && (u.getRoomUserID() == findTarget)){
                        u.setRoomUserID(0);
                        isFind = true;
                        break;
                    }
                }
                findTarget++;
            }
        }
        if(user.isReady()){
            room.setReadyUserCount(room.getReadyUserCount() - 1);
        }
        room.setCurrentUser(room.getCurrentUser() - 1);

        user.setReady(false);
        user.setRoomNumber(-1);
        user.setRoomUserID(-1);
        user.setPageNumber(1);

        Packet response;
        if(room.getCurrentUser() != 0){
            response = responseUserData(ID);
            for(User u : users){
                if(u.getRoomNumber() == ID){
                    response.addData("yourID", Integer.toString(u.getRoomUserID()));
                    sendOne(response, u.getSocketChannel());

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
        else{
            rooms.remove(room);

            for(User u : users){
                if(u.getPageNumber() != -1 && !u.equals(user)){
                    response = responseRoomData(u.getPageNumber());
                    sendOne(response, u.getSocketChannel());

                    ServerFrame.getInstance().appendLogLine("Send type: " + PacketType.RESPONSE_ROOM);
                    ServerFrame.getInstance().appendLogLine("Page: " + u.getPageNumber());
                    ServerFrame.getInstance().appendLogLine("Room count: " +
                            response.getData("totalroom"));
                }
            }
        }
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

    public List<Integer> getIDList(Room room){
        List<Integer> result = new ArrayList<>();
        for(User user : users){
            if(user.getRoomNumber() == room.getRoomID()){
                result.add(user.getRoomUserID());
            }
        }
        return result;
    }

    private Packet getGameResult(Room room){
        Packet packet = new Packet(PacketType.END_GAME);
        List<String> names = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();

        for(User user : users){
            if(room.getRoomID() == user.getRoomNumber()){
                names.add(user.getName());
                scores.add(user.getScore());

                user.setReady(false);
                user.setScore(0);
            }
        }

        int[] ranks = new int[names.size()];
        for(int i = 0; i < ranks.length; i++){
            ranks[i] = 0;
        }

        for(int i = 0; i < ranks.length; i++){
            for(int j = 0; j < scores.size(); j++){
                if(scores.get(i) < scores.get(j)){
                    ranks[i]++;
                }
            }
        }

        String[] resultNames = new String[names.size()];
        int[] resultScores = new int[names.size()];

        for(int i = 0; i < ranks.length; i++){
            resultScores[ranks[i]] = scores.get(i);
            resultNames[ranks[i]] = names.get(i);
        }

        for(int i = 0; i < resultNames.length; i++){
            packet.addData("rank" + i + "_name", resultNames[i]);
            packet.addData("rank" + i + "_score", Integer.toString(resultScores[i]));
        }

        return packet;
    }

    private void log(List<String> messages){
        for(String s : messages){
            ServerFrame.getInstance().appendLogLine(s);
        }
    }
}
