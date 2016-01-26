package com.nuevatel.mc.smpp.gw;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.nuevatel.common.exception.OperationException;
import com.nuevatel.mc.smpp.gw.domain.SmppGwSession;

/**
 * 
 * TODO by impl
 * 
 * @author Ariel Salazar
 *
 */
public class SmppGwProcessorTest {
    
    private SmppGwProcessor gwProcessor = null;
    
    @Mock
    private SmppGwSession gwSession;
    
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
        MockitoAnnotations.initMocks(this);
        gwProcessor = new SmppGwProcessor(gwSession) {
            @Override
            public void execute() {
                 // No op
            }
        };
        // Create smpp processors
        // p1
        SmppProcessor p1 = mock(SmppProcessor.class);
        when(p1.isBound()).thenReturn(true);
        doNothing().when(p1).setSmppProcessorId(anyInt());
        gwProcessor.registerSmppProcessor(p1);
        // p2
        SmppProcessor p2 = mock(SmppProcessor.class);
        when(p2.isBound()).thenReturn(true);
        doNothing().when(p2).setSmppProcessorId(anyInt());
        gwProcessor.registerSmppProcessor(p2);
        // p3
        SmppProcessor p3 = mock(SmppProcessor.class);
        when(p3.isBound()).thenReturn(true);
        doNothing().when(p3).setSmppProcessorId(anyInt());
        gwProcessor.registerSmppProcessor(p3);
    }

    @After
    public void tearDown() throws Exception {
        gwProcessor = null;
        gwSession = null;
    }

    @Test
    public void nextSmppProcessorId() {
        Integer id1 = gwProcessor.nextSmppProcessorId();
        assertNotNull("Null id1", id1);
        Integer id2 = gwProcessor.nextSmppProcessorId();
        assertNotNull("Null id2", id2);
        assertNotEquals("Bot are equals(id1,id2)", id1, id2);
        Integer id3 = gwProcessor.nextSmppProcessorId();
        assertNotNull("Null id3", id3);
        assertNotEquals("Bot are equals(id1,id3)", id1, id3);
        assertNotEquals("Bot are equals(id2,id3)", id2, id3);
        Integer id4 = gwProcessor.nextSmppProcessorId();
        assertNotNull("Null id4", id4);
        assertEquals("Not same Id4 = id1", id1, id4);
        System.out.println("id1=" + id1 + " id2=" + id2 + " id3=" + id3 + " id4=" + id4);
    }
    
    @Test
    public void registerSmppProcessor() throws OperationException {
        // p
        SmppProcessor p = mock(SmppProcessor.class);
        when(p.isBound()).thenReturn(true);
        doNothing().when(p).setSmppProcessorId(anyInt());
        gwProcessor.registerSmppProcessor(p);
        //
        assertTrue("Not found processor...", gwProcessor.getSmppProcessorMap().containsValue(p));
        // verify
        verify(p).setSmppProcessorId(anyInt());
    }
    
    @Test
    public void unregisterSmppProcessor() throws OperationException {
        // p
        SmppProcessor p = mock(SmppProcessor.class);
        when(p.isBound()).thenReturn(true);
        doNothing().when(p).setSmppProcessorId(anyInt());
        gwProcessor.registerSmppProcessor(p);
        //
        assertTrue("Not found processor...", gwProcessor.getSmppProcessorMap().containsValue(p));
        // unregister and verify
        gwProcessor.unregisterSmppProcessor(p);
        // verify
        verify(p).setSmppProcessorId(anyInt());
    }
}
