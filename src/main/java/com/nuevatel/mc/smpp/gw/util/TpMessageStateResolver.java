/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import org.smpp.Data;

import com.nuevatel.mc.tpdu.Tpdu;

/**
 * <p>The TpMessageStateResolver class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * 
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public final class TpMessageStateResolver {
    
    private TpMessageStateResolver() {
        // No op.
    }
    
    /* TP_ST Status */
    //    public static final byte TP_ST_SM_RECEIVED_BY_SME = 0x0;        // Short message received by the SME
    //    public static final byte TP_ST_SM_FW_WO_CONFIRMATION = 0x1;     // Short message forwarded by the SC to the SME but the SC is unable to confirm delivery
    //    public static final byte TP_ST_SM_REPLACED_BY_SC = 0x2;         // Short message replaced by the SC
    //
    //    public static final byte TP_ST_TEMPORARY_ERROR_0 = 0x20;        // Temporary error, SC still trying to transfer SM mask
    //    public static final byte TP_ST_PERMANENT_ERROR = 0x40;          // Permanent error, SC is not making any more transfer attempts mask
    //    public static final byte TP_ST_TEMPORARY_ERROR_1 = 0x60;        // Temporary error, SC is not making any more transfer attempts mask
    //    // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
    //    public static final byte TP_ST_CONGESTION = 0x0;                // Congestion
    //    public static final byte TP_ST_SME_BUSY = 0x1;                  // SME busy
    //    public static final byte TP_ST_NO_REPONSE_FROM_SME = 0x2;       // No response from SME
    //    public static final byte TP_ST_SERVICE_REJECTED = 0x3;          // Service rejected
    //    public static final byte TP_ST_QOS_NOT_AVAILABLE = 0x4;         // Quality of service not available
    //    public static final byte TP_ST_ERROR_IN_SME = 0x5;              // Error in SME
    //    // TP_ST_PERMANENT_ERROR
    //    public static final byte TP_ST_REMOTE_PROCEDURE_ERROR = 0x0;    // Remote procedure error
    //    public static final byte TP_ST_INCOMPATIBLE_DESTINATION = 0x1;  // Incompatible destination
    //    public static final byte TP_ST_CONNECTION_REJECTED_BY_SME = 0x2;// Connection rejected by SME
    //    public static final byte TP_ST_NOT_OBTAINABLE = 0x3;            // Not obtainable
    //    public static final byte TP_ST_NO_IW_AVAILABLE = 0x5;           // No interworking available
    //    public static final byte TP_ST_SM_VP_EXPIRED = 0x6;             // SM Validity Period Expired
    //    public static final byte TP_ST_SM_DELETED_BY_ORIG_SME = 0x7;    // SM Deleted by originating SME
    //    public static final byte TP_ST_SM_DELETED_BY_SC_ADM = 0x8;      // SM Deleted by SC Administration
    //    public static final byte TP_ST_SM_DOES_NOT_EXIST = 0x9;         // SM does not exist
    //    public static final byte TP_ST_SPECIFIC_TO_SC = 0x10;           // Values specific to each SC
    
    public static byte getMsgState(byte tpSt) {
        switch (tpSt) {
        case Tpdu.TP_ST_SM_RECEIVED_BY_SME:
            return Data.SM_STATE_DELIVERED;
        case Tpdu.TP_ST_SM_FW_WO_CONFIRMATION:
            return Data.SM_STATE_UNDELIVERABLE;
        default:
            break;
        }
        
        
        return 0;
    }
}
