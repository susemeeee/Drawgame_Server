/*
 * JoinRoomPacketParser.java
 * Author : 박찬형
 * Created Date : 2020-11-17
 */
package util;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;

import java.util.Map;

public class JoinRoomPacketParser extends Parser{
    @Override
    public Target parse(User user, Map<String, String> data) {
        log(data);
        return Target.SEND_TO_SENDER;
    }

    @Override
    public void log(Map<String, String> data) {
        ServerFrame.getInstance().appendLogLine("Received type: " + data.get("type"));
        ServerFrame.getInstance().appendLogLine("Room ID: " + data.get("id"));
    }
}
