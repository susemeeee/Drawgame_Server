/*
 * ChatPacketParser.java
 * Author : 박찬형
 * Created Date : 2020-11-17
 */
package util;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;

import java.util.Map;

public class ChatPacketParser extends Parser {
    @Override
    public Target parse(User user, Map<String, String> data) {
        log(data);
        return Target.ROOM_USER;
    }

    @Override
    public void log(Map<String, String> data) {
        ServerFrame.getInstance().appendLogLine("Received type: " + data.get("type"));
        ServerFrame.getInstance().appendLogLine("Chat content: " + data.get("content"));
    }
}
