package net.sothatsit.blockstore.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class NumberUtils {
    
    public static byte[] unpackInt(int num) {
        return ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(num).array();
    }
    
    public static int packInt(byte[] array) {
        return ByteBuffer.wrap(array).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }
    
    public static int packLoc(int x, int y, int z) {
        return packInt(new byte[] { (byte) x, (byte) y, (byte) z, (byte) 0 });
    }
    
}
