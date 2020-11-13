/*
 * LoginPacketParser.java
 * Author : 박찬형
 * Created Date : 2020-11-12
 */
package util;

import UI.ServerFrame;
import datatype.Target;
import datatype.User;

import javax.swing.*;
import java.util.Base64;
import java.util.Map;

public class LoginPacketParser extends Parser{
    @Override
    public Target parse(User user, Map<String, String> data) {
        user.setName(data.get("name"));
        byte[] imageBytes = Base64.getDecoder().decode(data.get("characterIcon"));
        user.setCharacterIcon(new ImageIcon(imageBytes));
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
