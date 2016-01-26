package com.nuevatel.mc.smpp.gw;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test of <code>SmppDateUtil</code>
 * 
 * @author Ariel Salazar
 *
 *Example 1: 
The time the message was sent: 2011-06-15 03:16:52:433 (BST) 
Absolute validity period: 110615022648000+ 

Example 2: 
The time message was sent: 2011-06-15 03:19:13:169 (BST) 
Absolute validity period: 110615022911000+ 

09 07 08 09 57 18 00 0+"
 *
 */
public class SmppDateUtilTest {
    
    /**
     * Would be interpreted as a relative period of 2 years, 6 months, 10 days, 23 hours, 34 minutes and 29 seconds from the current SMSC time.
     * 
     *  2018-07-07T16:57:17.797-04:00[America/La_Paz]
     *  2018-06-15T15:57:18.258-04:00[America/La_Paz]
     *  
     *  2015-04-16T19...
     */
    private static final String TEST_STR_RELATIVE_TIME = "020610233429000R";
    
    /**
     * The time message was sent: 2011-06-15 03:19:13:169 (BST)
     */
    private static final String TEST_STR_ABSOLUTE_TIME = "110615022648000+";
    
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        // No op
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        // No op
    }

    @Before
    public void setUp() throws Exception {
        // No op
    }

    @After
    public void tearDown() throws Exception {
        // No op
    }

    @Test
    public void parseDateTimeRelative() {
        Period period = Period.of(2, 6, 10);
        Duration duration = Duration.ofHours(23).plusMinutes(34).plusSeconds(29);
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime abstime = now.plus(period).plus(duration);
        ZonedDateTime testAbsTime = SmppDateUtil.parseDateTime(now, TEST_STR_RELATIVE_TIME);
        assertNotNull("ZonedDateTime is null", testAbsTime);
        assertTrue("not same time", abstime.isEqual(testAbsTime));
    }
    
    @Test
    public void parseDateTimeAbsolute() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime abstime = ZonedDateTime.of(2011, 6, 15, 02, 26, 48, 0, ZoneId.systemDefault());
        ZonedDateTime testAbsTime = SmppDateUtil.parseDateTime(now, TEST_STR_ABSOLUTE_TIME);
        assertNotNull("ZonedDateTime is null", testAbsTime);
        assertTrue("not same time", abstime.isEqual(testAbsTime));
    }
    
    @Test
    public void toSmppDatetimeAbsolute() {
        ZonedDateTime abstime = ZonedDateTime.of(2011, 6, 15, 02, 26, 48, 0, ZoneId.systemDefault());
        String formattedDate = SmppDateUtil.toSmppDatetime(abstime);
        assertNotNull("null formatted date", formattedDate);
        assertEquals("not same time", TEST_STR_ABSOLUTE_TIME, formattedDate);
    }
}
