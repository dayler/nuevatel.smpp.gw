/**
 * 
 */
package com.nuevatel.mc.smpp.gw.util;

import org.smpp.Data;

import com.nuevatel.mc.tpdu.Tpdu;

/**
 * @author Ariel Salazar
 *
 */
public final class TpStatusResolver {
    
    public static byte resolveTpStatus(int smppCmdId) {
        return TpStatus.fromSmppCommandId(smppCmdId).getTpStatus();
    }
    
    public static int resolveSmppCommandStatus(byte tpStatus) {
        // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
        // public static final byte TP_ST_CONGESTION = 0x0;                // Congestion
        // public static final byte TP_ST_SME_BUSY = 0x1;                  // SME busy
        // public static final byte TP_ST_NO_REPONSE_FROM_SME = 0x2;       // No response from SME
        // public static final byte TP_ST_SERVICE_REJECTED = 0x3;          // Service rejected
        // public static final byte TP_ST_QOS_NOT_AVAILABLE = 0x4;         // Quality of service not available
        // public static final byte TP_ST_ERROR_IN_SME = 0x5;              // Error in SME
        // TP_ST_PERMANENT_ERROR
        // public static final byte TP_ST_REMOTE_PROCEDURE_ERROR = 0x0;    // Remote procedure error
        // public static final byte TP_ST_INCOMPATIBLE_DESTINATION = 0x1;  // Incompatible destination
        // public static final byte TP_ST_CONNECTION_REJECTED_BY_SME = 0x2;// Connection rejected by SME
        // public static final byte TP_ST_NOT_OBTAINABLE = 0x3;            // Not obtainable
        // public static final byte TP_ST_NO_IW_AVAILABLE = 0x5;           // No interworking available
        // public static final byte TP_ST_SM_VP_EXPIRED = 0x6;             // SM Validity Period Expired
        // public static final byte TP_ST_SM_DELETED_BY_ORIG_SME = 0x7;    // SM Deleted by originating SME
        // public static final byte TP_ST_SM_DELETED_BY_SC_ADM = 0x8;      // SM Deleted by SC Administration
        // public static final byte TP_ST_SM_DOES_NOT_EXIST = 0x9;         // SM does not exist
        return SmppCommandStatus.fromTpStatus(tpStatus).getStatus();
    }
    
    private enum TpStatus {
        // Short message received by the SME
        TP_ST_SM_RECEIVED_BY_SME(0x0) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_ROK == smppCmdId;
            }
        },
        // Short message forwarded by the SC to the SME but the SC is unable to confirm delivery
        TP_ST_SM_FW_WO_CONFIRMATION(0x1),
        // Short message replaced by the SC
        TP_ST_SM_REPLACED_BY_SC(0x2) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_ROK == smppCmdId;
            }
        },
        // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
        // Congestion
        TP_ST_CONGESTION(0x0) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_RMSGQFUL == smppCmdId;
            }
        },
        // SME busy
        TP_ST_SME_BUSY(0x1),
        // No response from SME
        TP_ST_NO_REPONSE_FROM_SME(0x2),
        // Service rejected
        TP_ST_SERVICE_REJECTED(0x3) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_RX_T_APPN == smppCmdId;
            }
        },
        // Quality of service not available
        TP_ST_QOS_NOT_AVAILABLE(0x4),
        // Error in SME
        TP_ST_ERROR_IN_SME(0x5),
        // TP_ST_PERMANENT_ERROR
        // Remote procedure error
        TP_ST_REMOTE_PROCEDURE_ERROR(0x0) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_RX_P_APPN == smppCmdId;
            }
        },
        // Incompatible destination
        TP_ST_INCOMPATIBLE_DESTINATION(0x1),
        // Connection rejected by SME
        TP_ST_CONNECTION_REJECTED_BY_SME(0x2) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_RX_R_APPN == smppCmdId;
            }
        },
        // Not obtainable
        TP_ST_NOT_OBTAINABLE(0x3),
        // No interworking available
        TP_ST_NO_IW_AVAILABLE(0x5),
        // SM Validity Period Expired
        TP_ST_SM_VP_EXPIRED(0x6) {
            @Override
            protected boolean matchWith(int smppCmdId) {
                return Data.ESME_RINVEXPIRY == smppCmdId;
            }
        },
        // SM Deleted by originating SME
        TP_ST_SM_DELETED_BY_ORIG_SME(0x7),
        // SM Deleted by SC Administration
        TP_ST_SM_DELETED_BY_SC_ADM(0x8),
        // SM does not exist
        TP_ST_SM_DOES_NOT_EXIST(0x9),
        // Values specific to each SC
        TP_ST_SPECIFIC_TO_SC(0x10),
        ;
        
        private byte tpStatus;
        
        private TpStatus(int tpStatus) {
            this.tpStatus = (byte)tpStatus;
        }
        
        public byte getTpStatus() {
            return tpStatus;
        }
        
        protected boolean matchWith(int smppCmdId) {
            return false;
        }
        
        public static TpStatus fromSmppCommandId(int smppCmdId) {
            for (TpStatus ts : values()) {
                if (ts.matchWith(smppCmdId)) {
                    return ts;
                }
            }
            return TP_ST_REMOTE_PROCEDURE_ERROR;
        }
    }
    
    private enum SmppCommandStatus {
        ESME_ROK (Data.ESME_ROK, "No Error") {
            @Override
            protected boolean matchWith(byte tpStatus) {
                return Tpdu.TP_ST_SM_RECEIVED_BY_SME == tpStatus || Tpdu.TP_ST_SM_REPLACED_BY_SC == tpStatus;
            }
        },
        ESME_RINVMSGLEN(Data.ESME_RINVMSGLEN, "Message Length is invalid"),
        ESME_RINVCMDLEN(Data.ESME_RINVCMDLEN, "Command Length is invalid"),
        ESME_RINVCMDID(Data.ESME_RINVCMDID, "Invalid Command ID"),
        ESME_RINVBNDSTS(Data.ESME_RINVBNDSTS, "Incorrect BIND Status for given com- mand"),
        ESME_RALYBND(Data.ESME_RALYBND, "ESME Already in Bound State"),
        ESME_RINVPRTFLG(Data.ESME_RINVPRTFLG, "Invalid Priority Flag"),
        ESME_RINVREGDLVFLG(Data.ESME_RINVREGDLVFLG, "Invalid Registered Delivery Flag"),
        ESME_RSYSERR(Data.ESME_RSYSERR, "System Error"),
        // Reserved 0x00000009, Reserved
        ESME_RINVSRCADR(Data.ESME_RINVSRCADR, "Invalid Source Address"),
        ESME_RINVDSTADR(Data.ESME_RINVDSTADR, "Invalid Dest Addr"),
        ESME_RINVMSGID(Data.ESME_RINVMSGID, "Message ID is invalid") {
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte permanentError = (byte)(Tpdu.TP_ST_PERMANENT_ERROR & tpStatus);
                return Tpdu.TP_ST_SM_DELETED_BY_ORIG_SME == permanentError
                       || Tpdu.TP_ST_SM_DELETED_BY_SC_ADM == permanentError
                       || Tpdu.TP_ST_SM_DOES_NOT_EXIST == permanentError
                       || Tpdu.TP_ST_SPECIFIC_TO_SC == permanentError;
            }
        },
        ESME_RBINDFAIL(Data.ESME_RBINDFAIL, "Bind Failed"),
        ESME_RINVPASWD(Data.ESME_RINVPASWD, "Invalid Password"),
        ESME_RINVSYSID(Data.ESME_RINVSYSID, "Invalid System ID"),
        // Reserved 0x00000010 Reserved
        ESME_RCANCELFAIL(Data.ESME_RCANCELFAIL, "Cancel SM Failed"),
        // Reserved 0x00000012 Reserved
        ESME_RREPLACEFAIL(Data.ESME_RREPLACEFAIL, "Replace SM Failed"),
        // 
        ESME_RMSGQFUL(Data.ESME_RMSGQFUL, "Message Queue Full") {
            // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
            // public static final byte TP_ST_CONGESTION = 0x0;                // Congestion
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte temporaryError0 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_0 & tpStatus);
                byte temporaryError1 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_1 & tpStatus);
                return temporaryError0 == Tpdu.TP_ST_CONGESTION
                       || temporaryError1 == Tpdu.TP_ST_CONGESTION;
            }
        },
        ESME_RINVSERTYP(Data.ESME_RINVSERTYP, "Invalid Service Type"),
        // Reserved 0x00000016- 0x00000032 Reserved
        ESME_RINVNUMDESTS(Data.ESME_RINVNUMDESTS, "Invalid number of destinations"),
        ESME_RINVDLNAME(Data.ESME_RINVDLNAME, "Invalid Distribution List name"),
        // Reserved 0x00000035- 0x0000003F Reserved
        ESME_RINVDESTFLAG(Data.ESME_RINVDESTFLAG, "Destination flag is invalid (submit_multi)"),
        // Reserved 0x00000041 Reserved
        ESME_RINVSUBREP(Data.ESME_RINVSUBREP, "Invalid ‘submit with replace’ request (i.e. submit_sm with replace_if_present_flag set)"),
        ESME_RINVESMCLASS(Data.ESME_RINVESMCLASS, "Invalid esm_class field data"),
        ESME_RCNTSUBDL(Data.ESME_RCNTSUBDL, "Cannot Submit to Distribution List"),
        ESME_RSUBMITFAIL(Data.ESME_RSUBMITFAIL, "submit_sm or submit_multi failed"),
        // Reserved 0x00000046- 0x00000047 Reserved
        ESME_RINVSRCTON(Data.ESME_RINVSRCTON, "Invalid Source address TON"),
        ESME_RINVSRCNPI(Data.ESME_RINVSRCNPI, "Invalid Source address NPI"),
        ESME_RINVDSTTON(Data.ESME_RINVDSTTON, "Invalid Destination address TON"),
        ESME_RINVDSTNPI(Data.ESME_RINVDSTNPI, "Invalid Destination address NPI"),
        // Reserved 0x00000052 Reserved
        ESME_RINVSYSTYP(Data.ESME_RINVSYSTYP, "Invalid system_type field"),
        ESME_RINVREPFLAG(Data.ESME_RINVREPFLAG, "Invalid replace_if_present flag"),
        ESME_RINVNUMMSGS(Data.ESME_RINVNUMMSGS, "Invalid number of messages"),
        // Reserved 0x00000056- 0x00000057 Reserved
        ESME_RTHROTTLED(Data.ESME_RTHROTTLED, "Throttling error (ESME has exceeded allowed message limits)"),
        // Reserved 0x00000059- 0x00000060 Reserved
        ESME_RINVSCHED(Data.ESME_RINVSCHED, "Invalid Scheduled Delivery Time"),
        ESME_RINVEXPIRY(Data.ESME_RINVEXPIRY, "Invalid message validity period (Expiry time)") {
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte permanentError = (byte)(Tpdu.TP_ST_PERMANENT_ERROR & tpStatus);
                return Tpdu.TP_ST_SM_VP_EXPIRED == permanentError;
            }
        },
        ESME_RINVDFTMSGID(Data.ESME_RINVDFTMSGID, "Predefined Message Invalid or Not Found"),
        ESME_RX_T_APPN(Data.ESME_RX_T_APPN, "ESME Receiver Temporary App Error Code") {
            // TP_ST_TEMPORARY_ERROR_0, TP_ST_TEMPORARY_ERROR_1
            // public static final byte TP_ST_SME_BUSY = 0x1;                  // SME busy
            // public static final byte TP_ST_NO_REPONSE_FROM_SME = 0x2;       // No response from SME
            // public static final byte TP_ST_SERVICE_REJECTED = 0x3;          // Service rejected
            // public static final byte TP_ST_QOS_NOT_AVAILABLE = 0x4;         // Quality of service not available
            // public static final byte TP_ST_ERROR_IN_SME = 0x5;              // Error in SME
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte temporaryError0 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_0 & tpStatus);
                byte temporaryError1 = (byte)(Tpdu.TP_ST_TEMPORARY_ERROR_1 & tpStatus);
                return temporaryError0 == Tpdu.TP_ST_SME_BUSY
                       || temporaryError1 == Tpdu.TP_ST_SME_BUSY
                       || temporaryError0 == Tpdu.TP_ST_NO_REPONSE_FROM_SME
                       || temporaryError1 == Tpdu.TP_ST_NO_REPONSE_FROM_SME
                       || temporaryError0 == Tpdu.TP_ST_SERVICE_REJECTED
                       || temporaryError1 == Tpdu.TP_ST_SERVICE_REJECTED
                       || temporaryError0 == Tpdu.TP_ST_ERROR_IN_SME
                       || temporaryError1 == Tpdu.TP_ST_ERROR_IN_SME;
                
            }
        },
        ESME_RX_P_APPN(Data.ESME_RX_P_APPN, "ESME Receiver Permanent App Error Code") {
            // TP_ST_PERMANENT_ERROR
            // public static final byte TP_ST_REMOTE_PROCEDURE_ERROR = 0x0;    // Remote procedure error
            // public static final byte TP_ST_INCOMPATIBLE_DESTINATION = 0x1;  // Incompatible destination
            // public static final byte TP_ST_NOT_OBTAINABLE = 0x3;            // Not obtainable
            // public static final byte TP_ST_NO_IW_AVAILABLE = 0x5;           // No interworking available
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte permanentError = (byte)(Tpdu.TP_ST_PERMANENT_ERROR & tpStatus);
                return Tpdu.TP_ST_REMOTE_PROCEDURE_ERROR == permanentError
                       || Tpdu.TP_ST_INCOMPATIBLE_DESTINATION == permanentError
                       || Tpdu.TP_ST_NOT_OBTAINABLE == permanentError
                       || Tpdu.TP_ST_NO_IW_AVAILABLE == permanentError;
            }
        },
        ESME_RX_R_APPN(Data.ESME_RX_R_APPN, "ESME Receiver Reject Message Error Code") {
            @Override
            protected boolean matchWith(byte tpStatus) {
                byte permanentError = (byte)(Tpdu.TP_ST_PERMANENT_ERROR & tpStatus);
                return Tpdu.TP_ST_CONNECTION_REJECTED_BY_SME == permanentError;
            }
        },
        ESME_RQUERYFAIL(Data.ESME_RQUERYFAIL, "query_sm request failed"),
        // Reserved 0x00000068 - 0x000000BF Reserved
        ESME_RINVOPTPARSTREAM(Data.ESME_RINVOPTPARSTREAM, "Error in the optional part of the PDU Body."),
        ESME_ROPTPARNOTALLWD(Data.ESME_ROPTPARNOTALLWD, "Optional Parameter not allowed"),
        ESME_RINVPARLEN(Data.ESME_RINVPARLEN, "Invalid Parameter Length."),
        ESME_RMISSINGOPTPARAM(Data.ESME_RMISSINGOPTPARAM, "Expected Optional Parameter missing"),
        ESME_RINVOPTPARAMVAL(Data.ESME_RINVOPTPARAMVAL, "Invalid Optional Parameter Value"),
        // Reserved 0x000000C5 - 0x000000FD Reserved
        ESME_RDELIVERYFAILURE(Data.ESME_RDELIVERYFAILURE, "Delivery Failure (used for data_sm_resp)"),
        ESME_RUNKNOWNERR(Data.ESME_RUNKNOWNERR, "Unknown Error"),
        // Reserved for SMPP extension 0x00000100- 0x000003FF Reserved for SMPP extension
        // Reserved for SMSC vendor specific errors 0x00000400- 0x000004FF Reserved for SMSC vendor specific errors
        // Reserved 0x00000500- 0xFFFFFFFF Reserved
        ;
        
        private int cmdStatus;
        private String description;
        
        private SmppCommandStatus(int cmdStatus, String description) {
            this.cmdStatus = cmdStatus;
            this.description = description;
        }
        
        public int getStatus() {
            return cmdStatus;
        }
        
        @SuppressWarnings("unused")
        public String getDescription() {
            return description;
        }
        
        protected boolean matchWith(byte tpStatus) {
            return false;
        }
        
        public static SmppCommandStatus fromTpStatus(byte tpStatus) {
            for (SmppCommandStatus cs : values()) {
                if (cs.matchWith(tpStatus)) {
                    return cs;
                }
            }
            // Unknown
            return ESME_RUNKNOWNERR;
        }
    }
}
