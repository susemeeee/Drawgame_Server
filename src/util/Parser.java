/*
 * Parser.java
 * Author : 박찬형
 * Created Date : 2020-11-12
 */
package util;

import datatype.Target;
import datatype.User;

import java.util.Map;

public abstract class Parser {
    public abstract Target parse(User user, Map<String, String> data);
    public abstract void log(Map<String, String> data);
}
