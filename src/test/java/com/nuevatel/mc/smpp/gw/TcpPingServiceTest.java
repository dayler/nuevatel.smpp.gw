/**
 * 
 */
package com.nuevatel.mc.smpp.gw;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.UnknownHostException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.smpp.Connection;

import com.nuevatel.mc.smpp.gw.domain.Config;

/**
 * <p>The TcpPingServiceTest class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Test TcpPingService.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class TcpPingServiceTest {
    
    private static Config cfg;
    
    private TcpPingService srv = null;
    
    @Mock
    private Connection conn = null;
    
    private boolean failed = false;
    
    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        cfg = mock(Config.class);
        when(cfg.isPingTaskEnable()).thenReturn(true);
        when(cfg.getPingTaskTime()).thenReturn(1);
        when(cfg.getPingTaskTimeout()).thenReturn(500);
        when(cfg.getPingTaskProbes()).thenReturn(1);
        AllocatorService.setConfig(cfg);
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AllocatorService.setConfig(null);
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
        if (srv != null) {
            srv.shutdown();
        }
        conn = null;
    }

    /**
     * Test method for {@link com.nuevatel.mc.smpp.gw.TcpPingService#execute()}.
     * @throws UnknownHostException 
     * @throws InterruptedException 
     */
    @Test
    public void executeWithUnknownAddr() throws UnknownHostException, InterruptedException {
        when(conn.getAddress()).thenReturn("10.10.10.10");
        srv = new TcpPingService(conn, () -> onConnectionLost());
        srv.execute();
        Thread.sleep(2000L);
        assertTrue("Delegate is not executed", failed);
        // verify
        verify(conn).getAddress();
    }
    
    /**
     * Test method for {@link com.nuevatel.mc.smpp.gw.TcpPingService#execute()}.
     * @throws UnknownHostException 
     * @throws InterruptedException 
     */
    @Test
    public void execute() throws UnknownHostException, InterruptedException {
        when(conn.getAddress()).thenReturn("127.0.0.1");
        srv = new TcpPingService(conn, () -> onConnectionLost());
        srv.execute();
        Thread.sleep(1000L);
        assertFalse("Delegate is not executed", failed);
        // verify
        verify(conn).getAddress();
    }

    private void onConnectionLost() {
        failed = true;
    }
}
