/*
 * Packet.java
 * Author : 박찬형
 * Created Date : 2020-11-12
 */
package datatype.packet;

import java.util.HashMap;
import java.util.Map;

public class Packet {
    private final PacketType type;
    private Map<String, String> data;

    public Packet(PacketType type){
        this.type = type;
        data = new HashMap<>();
    }

    public PacketType getType() {
        return type;
    }

    public void addData(String key, String value){
        data.put(key, value);
    }

    public void replaceData(String key, String value){
        if(data.containsKey(key)){
            data.replace(key, value);
        }
    }

    public void removeData(String key){
        data.remove(key);
    }

    public Map<String, String> getAllData(){
        if(!data.containsKey("type")){
            data.put("type", type.toString());
        }
        return data;
    }

    public String getData(String key){
        return data.get(key);
    }

    @Override
    public String toString() {
        data.put("type", type.toString());
        StringBuilder result = new StringBuilder();
        result.append("{");
        for(Map.Entry<String, String> entry : data.entrySet()){
            result.append("\"");
            result.append(entry.getKey());
            result.append("\":\"");
            result.append(entry.getValue());
            result.append("\"");
            result.append(", ");
        }
        result.delete(result.length() - 2, result.length());
        result.append("}");
        return result.toString();
    }
}
