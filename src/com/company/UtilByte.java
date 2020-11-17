package com.company;

import java.nio.ByteBuffer;

public class UtilByte {
    public static byte[] intInByte(int i){
        return ByteBuffer.allocate(4).putInt(i).array();
    }
    public static int byteInInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }
}
