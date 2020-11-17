/*
 * LoginPacketParser.java
 * Author : 박찬형
 * Created Date : 2020-11-12
 */
package util;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;

import java.util.Map;

public class LoginPacketParser extends Parser{
    @Override
    public Target parse(User user, Map<String, String> data) {
        user.setName(data.get("name"));
        user.setCharacterIcon(data.get("characterIcon"));
        log(data);
        return Target.NONE;
    }

    @Override
    public void log(Map<String, String> data) {
        ServerFrame.getInstance().appendLogLine("Received type: " + data.get("type"));
        ServerFrame.getInstance().appendLogLine("Name : " + data.get("name"));
        ServerFrame.getInstance().appendLogLine("CharacterIcon: IMAGE");
    }
}
