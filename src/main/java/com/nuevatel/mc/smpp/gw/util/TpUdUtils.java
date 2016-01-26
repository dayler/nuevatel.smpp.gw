/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.nuevatel.mc.tpdu.TpDcs;

/**
 * @author Ariel Salazar
 *
 */
public final class TpUdUtils {
    
    private static final byte[] BITMASK = {0x7f, 0x3f, 0x1f, 0xf, 0x7, 0x3, 0x1, 0x0};
    
    private TpUdUtils() {
        // no op
    }
    
    public static byte[] fixTpUd(byte charSet, byte[] data) {
        return TpDcs.CS_GSM7 == charSet ? getSmsGsm7(data) : data;
    }
    
    public static byte[] getSmsGsm7(byte[] data) {
        if (data.length > 160) {
            throw new IllegalArgumentException("length must not exceed 160 characters");
        }
        ByteBuffer gsm7 = ByteBuffer.wrap(data);
        byte bitPos = 0;
        short index = 0;
        List<Byte> smsGsm7List = new ArrayList<>();
        while (index < gsm7.remaining()) {
            byte tmpByte = (byte) ((gsm7.get(index) >> bitPos) & BITMASK[bitPos]);
            if (index + 1 < gsm7.remaining()) {
                smsGsm7List.add((byte) (tmpByte | ((gsm7.get(index + 1) << (7 - bitPos)) & ~BITMASK[bitPos] & 0xff)));
            } else {
                smsGsm7List.add(tmpByte);
            }
            if (bitPos < 6) {
                ++bitPos;
            } else {
                bitPos = 0;
                ++index;
            }
            ++index;
        }
        byte[] smsGsm7 = new byte[smsGsm7List.size()];
        for (index = 0; index < smsGsm7.length; index++) {
            smsGsm7[index] = smsGsm7List.get(index);
        }
        return smsGsm7;
    }
}
