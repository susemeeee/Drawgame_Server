/*
 * PacketType.java
 * Author : 박찬형
 * Created Date : 2020-11-12
 */
package datatype.packet;

import java.io.Serializable;

public enum PacketType implements Serializable {
    LOGIN, REQUEST_ROOM, RESPONSE_ROOM, MAKE_ROOM
}