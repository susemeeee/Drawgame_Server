/*
 * TypeParser.java
 * Author : 박찬형
 * Created Date : 2020-11-13
 */
package util;

import java.util.HashMap;
import java.util.Map;

public class DataMaker {
    public static Map<String, String> make(String data){
        Map<String, String> result = new HashMap<>();
        String[] array = data.split("\\{\"|\":\"|\", \"|\"}");
        for(int i = 1; i < array.length; i += 2){
            result.put(array[i], array[i + 1]);
        }
        return result;
    }
}
