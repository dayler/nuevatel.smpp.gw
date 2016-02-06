
package com.nuevatel.mc.smpp.gw.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import com.nuevatel.common.util.Parameters;
import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;

/**
 * <p>The SimpleTpUd class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Factory to create TpDu from SMPP PDU.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class TpUdFactory {
    
    private static final byte[] BITMASK = {0x7f, 0x3f, 0x1f, 0xf, 0x7, 0x3, 0x1, 0x0};
    
    private TpUdFactory() {
        // No op
    }
    
    /**
     * Get TpUd from SMPP PDU data.
     * @param tpDcs
     * @param esmClass
     * @param smData
     * @return
     */
    public static TpUd getTpUd(TpDcs tpDcs, EsmClass esmClass, byte[] smData) {
        Parameters.checkNull(tpDcs, "tpDcs");
        Parameters.checkNull(esmClass, "esmClass");
        Parameters.checkNull(smData, "smData");
        
        byte[] data = fixTpUd(tpDcs.getCharSet(), esmClass, smData);
        return new TpUd(// tpUdhi
                        esmClass.getUdhi(),
                        // tpDcs
                        tpDcs,
                        // tpUdl
                        (byte) smData.length,
                        // tpUd
                        data);
    }
    
    /**
     * Transform to septets <code>data</code> if charset is GSM7.
     * @param charSet
     * @param data
     * @return
     */
    protected static byte[] fixTpUd(byte charSet, EsmClass esmClass, byte[] smData) {
        if (TpDcs.CS_GSM7 != charSet) {
            return smData;
        }
        
        if (esmClass.getUdhi()) {
            short udhl = (short) (smData[0] & 0xff);
            // TODO java.lang.NegativeArraySizeException
            byte[] tmpSmData = new byte[smData.length - (udhl + 1)];
             System.arraycopy(smData, udhl + 1, tmpSmData, 0, tmpSmData.length);
            tmpSmData = getSmsGsm7(tmpSmData); // reduce to septets
            // Concatenate response array
            byte[] fixedTpdu = new byte[udhl + 1 + tmpSmData.length];
            // copy headers
            System.arraycopy(smData, 0, fixedTpdu, 0, udhl + 1);
            // copy encoded data
            System.arraycopy(tmpSmData, 0, fixedTpdu, udhl + 1, tmpSmData.length);
            return fixedTpdu;
        }
        else {
            return getSmsGsm7(smData);
        }
    }
    
    /**
     * Transform to septets <code>data</code>.
     * @param data
     * @return
     */
    protected static byte[] getSmsGsm7(byte[] data) {
        if (data.length > 160) throw new IllegalArgumentException("length must not exceed 160 characters");
        ByteBuffer gsm7 = ByteBuffer.wrap(data);
        byte bitPos = 0;
        short index = 0;
        List<Byte> smsGsm7List = new ArrayList<>();
        while (index < gsm7.remaining()) {
            byte tmpByte = (byte) ((gsm7.get(index) >> bitPos) & BITMASK[bitPos]);
            if (index + 1 < gsm7.remaining()) {
                smsGsm7List.add((byte) (tmpByte | ((gsm7.get(index + 1) << (7 - bitPos)) & ~BITMASK[bitPos] & 0xff)));
            }
            else {
                smsGsm7List.add(tmpByte);
            }
            if (bitPos < 6) {
                ++bitPos;
            }
            else {
                bitPos = 0;
                ++index;
            }
            ++index;
        }
        byte[] smsGsm7 = new byte[smsGsm7List.size()];
        for (index = 0; index < smsGsm7.length; index++) smsGsm7[index] = smsGsm7List.get(index);
        return smsGsm7;
    }
}
