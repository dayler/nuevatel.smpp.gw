/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nuevatel.mc.tpdu.TpDcs;
import com.nuevatel.mc.tpdu.TpUd;

/**
 * <p>The TpUdFactoryTest class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * 
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class TpUdFactoryTest {
    
    private byte[] msg;
    
    private byte[] udh;
    
    private byte[] gsm7EncData;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // No op
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // No op
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        msg = new byte[] 
                {
                    0x06, 0x05, 0x04, 0x15, 0x79, 0x00, 0x00, 0x52, 0x45, 0x47, 0x2d, 0x52, 0x45, 0x53, 0x50, 0x3f, 0x76, 0x3d, 0x33, 0x3b,
                    0x72, 0x3d, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x66, 0x3b, 0x6e, 0x3d, 0x2b, 0x35, 0x39, 0x31, 0x37, 0x30, 0x30,
                    0x30, 0x30, 0x30, 0x30, 0x31, 0x3b, 0x73, 0x3d, 0x4f, 0x4d, 0x54, 0x45, 0x53, 0x54, 0x31, 0x32, 0x33, 0x34, 0x41, 0x42,
                    0x43, 0x44, 0x41, 0x42, 0x43, 0x44, 0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, 0x38, 0x39, 0x41, 0x42, 0x43, 0x44,
                    0x41, 0x42, 0x43, 0x44, 0x41, 0x42, 0x43, 0x44, 0x39, 0x38, 0x37, 0x36, 0x35, 0x34, 0x33, 0x32, 0x31, 0x31, 0x32, 0x33,
                    0x78, 0x31, 0x34, 0x31, 0x35, 0x31, 0x37
                };
        udh = new byte[] {0x06, 0x05, 0x04, 0x15, 0x79, 0x00, 0x00};
        gsm7EncData = new byte[]
                {
                    -46, -30, -79, 37, 45, 78, -95, 63, 123, 111, -74, -109, -9, -52, 102, -77, -39, 108, 54, -101, 119, -18, -34, -86, -106,
                    -117, -35, 96, 48, 24, 12, 6, -117, -19, -26, -67, 103, -109, 90, -100, 82, 99, -78, 25, 45, 40, 28, 18, -125, -62, 33,
                    17, 22, -109, -51, 104, 53, -37, 13, -105, 11, 10, -121, -60, -96, 112, 72, 12, 10, -121, -60, 28, -18, 102, -85, -47, 102,
                    -78, 88, 76, 54, -61, -57, 104, -79, 90, -20, 6
                };
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        msg = null;
        udh = null;
        gsm7EncData = null;
    }

    /**
     * Test method for {@link com.nuevatel.mc.smpp.gw.util.TpUdFactory#getTpUd(com.nuevatel.mc.tpdu.TpDcs, com.nuevatel.mc.smpp.gw.util.EsmClass, byte[])}.
     */
    @Test
    public void getTpUdWithCS_GSM7AndHdhi() {
        EsmClass esmClass = new EsmClass((byte)64);
        TpUd tpUd = TpUdFactory.getTpUd(new TpDcs(TpDcs.CS_GSM7), esmClass, msg);
        assertNotNull("Null tpud", tpUd);
        assertNotEquals("Null getTpUd", tpUd.getTpUd());
    }

    /**
     * Test method for {@link com.nuevatel.mc.smpp.gw.util.TpUdFactory#fixTpUd(byte, com.nuevatel.mc.smpp.gw.util.EsmClass, byte[])}.
     */
    @Test
    public void fixTpUdWithCS_GSM7AndUdhi() {
        EsmClass esmClass = new EsmClass((byte)64);
        byte[] fixedTpUd = TpUdFactory.fixTpUd(TpDcs.CS_GSM7, esmClass, msg);
        assertNotNull("Null fixedTpUd", fixedTpUd);
        // assert headers
        for (int i = 0; i > udh.length; i++) {
            assertEquals("Failed to assert header byte. index=" + i, udh[i], fixedTpUd[i]);
        }
        // assert body
        for (int i = 0; i < gsm7EncData.length; i++) {
            assertEquals("Failed to assert encoded gsm7 data. index=" + i, gsm7EncData[i], fixedTpUd[ i + udh.length]);
        }
    }
    
    @Test
    public void getTpUdWithCS_UCS2AndHdhi() {
        EsmClass esmClass = new EsmClass((byte)64);
        TpUd tpUd = TpUdFactory.getTpUd(new TpDcs(TpDcs.CS_UCS2), esmClass, msg);
        assertNotNull("Null tpud", tpUd);
        assertNotEquals("Null getTpUd", tpUd.getTpUd());
    }
    
    @Test
    public void fixTpUdWithCS_UCS2AndUdhi() {
        EsmClass esmClass = new EsmClass((byte)64);
        byte[] fixedTpUd = TpUdFactory.fixTpUd(TpDcs.CS_UCS2, esmClass, msg);
        // assert tpdu data
        assertNotNull("Null fixedTpdu", fixedTpUd);
        assertEquals("Length does not match", msg.length, fixedTpUd.length);
        for (int i = 0; i < msg.length; i++) {
            assertEquals("Failed on match data. index=" + i , msg[i], fixedTpUd[i]);
        }
    }
    
    @Test
    public void getTpUdWithCS_GSM7() {
        EsmClass esmClass = new EsmClass((byte)0);
        TpUd tpUd = TpUdFactory.getTpUd(new TpDcs(TpDcs.CS_GSM7), esmClass, msg);
        assertNotNull("Null tpud", tpUd);
        assertNotEquals("Null getTpUd", tpUd.getTpUd());
    }
    
    @Test
    public void fixTpUdWithCS_GSM7() {
        EsmClass esmClass = new EsmClass((byte)0);
        byte[] fixedTpUd = TpUdFactory.fixTpUd(TpDcs.CS_GSM7, esmClass, msg);
        // assert tpdu data
        assertNotNull("Null fixedTpdu", fixedTpUd);
        byte[] encodedData = TpUdFactory.getSmsGsm7(msg);
        assertEquals("Length does not match", encodedData.length, fixedTpUd.length);
        for (int i = 0; i < encodedData.length; i++) {
            assertEquals("Failed on match data. index=" + i , encodedData[i], fixedTpUd[i]);
        }
    }
}
