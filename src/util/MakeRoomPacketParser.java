/*
 * MakeRoomPacketParser.java
 * Author : 박찬형
 * Created Date : 2020-11-14
 */
package util;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;

import java.util.Map;

public class MakeRoomPacketParser extends Parser {
    @Override
    public Target parse(User user, Map<String, String> data) {
        user.setPageNumber(-1);
        log(data);
        return Target.WAITING_USER;
    }

    @Override
    public void log(Map<String, String> data) {
        ServerFrame.getInstance().appendLogLine("Received type: " + data.get("type"));
        ServerFrame.getInstance().appendLogLine("Room name: " + data.get("roomname"));
        ServerFrame.getInstance().appendLogLine("Max person: " + data.get("maxperson"));
        ServerFrame.getInstance().appendLogLine("Round: " + data.get("maxround"));
    }
}
