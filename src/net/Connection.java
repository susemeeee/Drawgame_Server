/*
 * net.Connection.java
 * Author : 박찬형
 * Created Date : 2020-11-09
 */
package net;

import UI.ServerFrame;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

public class Connection{
    private ServerSocketChannel server;
    private Selector selector;
    private Thread serverThread;

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

            //TODO 접속자 추가 동작
            ServerFrame.getInstance().appendLogLine("client connect.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void read(SelectionKey key){
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        try {
            buffer.clear();
            ((SocketChannel)key.channel()).read(buffer);

            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer.array());
            ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
            String data = (String)objectInputStream.readObject();
            ServerFrame.getInstance().appendLogLine(data);

            sendOne(data, (SocketChannel)key.channel()); //test
            //TODO 데이터 바인딩
        } catch (IOException | ClassNotFoundException e) {
            //TODO 연결 끊어짐
            e.printStackTrace();
        }
    }

    private void sendOne(String data, SocketChannel client){
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
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
}
