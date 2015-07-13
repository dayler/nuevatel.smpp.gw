/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.dispatcher;

import com.logica.smpp.Data;
import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.DeliverSMResp;
import com.logica.smpp.pdu.PDU;
import com.logica.smpp.pdu.Response;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.SubmitSMResp;
import com.logica.smpp.pdu.ValueNotSetException;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.nuevatel.base.appconn.CompositeIE;
import com.nuevatel.base.appconn.Message;
import com.nuevatel.mc.appconn.ForwardMOSMRequest;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.tpdu.Address;
import com.nuevatel.mc.tpdu.DCS;
import com.nuevatel.mc.tpdu.SMSSubmit;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.SMPPServerApplication;
import com.nuevatel.smpp.session.Session;
import com.nuevatel.smpp.session.SessionProperties;
import com.nuevatel.smpp.utils.CharsetHelper;
import com.nuevatel.smpp.utils.StringUtils;
import com.nuevatel.smpp.utils.TPUDLOverhead;
import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author Luis Baldiviezo
 */
public class MCDispatcher implements Runnable {

    /*private variables*/
    private PDU pdu;
    private Session session;
    private static final Logger logger = Logger.getLogger(MCDispatcher.class.getName());

//    private Date createDate;
//    private Date runDate;
//    private Date endDate;

    //the TpUDL field
    private byte tpUDL;

    /*masks*/
    private static final byte MASK_ESM_CLASS=(byte)0xC0;

    public MCDispatcher(PDU pdu, Session session){
        this.pdu=pdu;
        this.session=session;
//        createDate = new Date();

    }

    public void run() {
//        runDate=new Date();
        session.updateLastReceivedMessageTimestamp();
        if (pdu.getCommandId()==Data.SUBMIT_SM){
            //the SMPP SubmitSM
            SubmitSM submitSM = (SubmitSM)pdu;

            //message throttling logic
//            logger.finest("messageLimit: "+ session.getSessionProperties().getMessageLimit()+" messageLimitWindow:"+ session.getSessionProperties().getMessageLimitWindow()+" messageCount:" +session.getMessageCount());
            if (session.getSessionProperties().getMessageLimit()!=0){
                if (session.getMessageCount()>=session.getSessionProperties().getMessageLimit()){
                    Response response = submitSM.getResponse();
                    response.setCommandStatus(Data.ESME_RTHROTTLED);
                    session.send(response);
                    return;
                }
                else{
                    session.increaseMessageCount();
                }
            }
            

            //the GSM TP-Reject-Duplicates parameter
            boolean tpRD=false;

            //The GSM TP-Reply-Path parameter
            boolean tpRP=false;

            //The GSM TP-User-Data-Header-Indicator parameter
            boolean tpUDHI=false;

            switch ((submitSM.getEsmClass() & MASK_ESM_CLASS)>>6){
                case 1: tpUDHI=true; break;
                case 2: tpRP=true; break;
                case 3: tpUDHI=tpRP=true; break;
            }

            //The GSM TP-Status-Report-Request paremeter
            boolean tpSSR=false;
            if (session.getSessionProperties().enableSR() && submitSM.getRegisteredDelivery()==1) tpSSR=true; //only success&failure is supported

            //The GSM TP-Message-Reference paremeter
            byte tpMR;
            try{
                tpMR = (byte)submitSM.getUserMessageReference();
            }
            catch (ValueNotSetException ex){
                tpMR = 0;
            }

            //The GSM TP-Protocol-Identifier parameter
            byte tpPID=submitSM.getProtocolId();

            //The GSM TP-Destination-Address parameter
            Address tpDA = new Address((byte)(submitSM.getDestAddr().getTon()<<4), submitSM.getDestAddr().getNpi(), submitSM.getDestAddr().getAddress());

            //The GSM TP-Data-Coding-Scheme paramter
            DCS tpDCS;
            Byte msMsgWaitFacilities = null;
            Byte destAddrSubunit = null;
            byte indication;
            byte indicationType;
            byte messageClass;
                //first check if the optional headers are present
            try{
                msMsgWaitFacilities = submitSM.getMsMsgWaitFacilities();
                switch (msMsgWaitFacilities >> 7){
                    case 0: indication = DCS.INDICATION_INACTIVE; break;
                    case 1: indication = DCS.INDICATION_ACTIVE; break;
                    default: indication = DCS.INDICATION_INACTIVE; break;
                }
                switch(msMsgWaitFacilities & 0x3){
                    case 0: indicationType = DCS.VOICEMAIL_MW; break;
                    case 1: indicationType = DCS.FAX_MW; break;
                    case 2: indicationType = DCS.ELECTRONIC_MAIL_MW; break;
                    case 3: indicationType = DCS.OTHER_MW; break;
                    default:indicationType = DCS.VOICEMAIL_MW; break;
                }
            }catch (ValueNotSetException ex1){
                indication = DCS.INDICATION_INACTIVE;
                indicationType = DCS.VOICEMAIL_MW;
            }

            try{
                destAddrSubunit = submitSM.getDestAddrSubunit();
                switch (destAddrSubunit){
//                        case 0: messageClass = DCS.CLASS_0; break;
//                        case 1: messageClass = DCS.CLASS_0; break;
                        case 2: messageClass = DCS.CLASS_1; break;
                        case 3: messageClass = DCS.CLASS_2; break;
                        case 4: messageClass = DCS.CLASS_3; break;
                        default: messageClass = DCS.CLASS_0; break;
                    }
            }catch (ValueNotSetException ex2){
                messageClass = DCS.CLASS_0;
            }
            if (msMsgWaitFacilities!=null || destAddrSubunit!=null){
//                if (submitSM.getDestAddr().getAddress().contains("70100044")){
//                    System.out.println("destAddrSubunit: "+(destAddrSubunit&255));
//                }
                byte codingGroup = (byte)(submitSM.getDataCoding()&240);
                byte charset = (byte)(submitSM.getDataCoding()&12);
//                byte codingGroup = DCS.GENERAL_UNCOMPRESSED_NO_CLASS_MEANING;
//                if (messageClass!=DCS.CLASS_0) codingGroup=DCS.GENERAL_UNCOMPRESSED_CLASS_MEANING;
//                if(indication != DCS.INDICATION_INACTIVE) codingGroup = DCS.WAITING_INDICATION_STORE_CS_GSM7;
                tpDCS = new DCS(codingGroup, messageClass, charset, indication, indicationType);
            }
            else{
//                if (submitSM.getDestAddr().getAddress().contains("70100044")){
//                    System.out.println("dataCoding: "+(submitSM.getDataCoding()&255));
//                }
                //no optional headers, just copy de dataCoding into de DCS
                tpDCS = new DCS(submitSM.getDataCoding());
            }

            //The GSM TP-User-Data parameter
            byte[] tpUD=getTpUD(pdu, tpDCS, tpUDHI);
//            if (submitSM.getDestAddr().getAddress().contains("70100044")){
//                System.out.println("DCS:" + (tpDCS.getDCS()&255));
//                System.out.println("Charset:" + (tpDCS.getCharSet()&255));
//                System.out.println("CodingGroup:" + (tpDCS.getCodingGroup()&255));
//                System.out.println("MessageClass:" + (tpDCS.getMessageClass()&255));
//                System.out.println("shortMessageData:" + StringUtils.getHex(submitSM.getShortMessageData()));
//                System.out.println("tpUD:" + StringUtils.getHex(tpUD));
//                System.out.println("tpUDL:" + (tpUDL&255));
//            }

            //The GSM SMSSubmit
            SMSSubmit smsSubmit = new SMSSubmit(tpRD, tpRP, tpUDHI, tpSSR, tpMR, tpDA, tpPID, tpDCS,getTpUDL(),tpUD);
//            Name msisdn = new Name(submitSM.getSourceAddr().getAddress(), (byte)(submitSM.getSourceAddr().getNpi()|submitSM.getSourceAddr().getTon()));
//            if (!submitSM.getSourceAddr().getAddress().matches("[0-9]+")) submitSM.getSourceAddr().setTon(Data.GSM_TON_ALPHANUMERIC);
//            if (!submitSM.getSourceAddr().getAddress().matches("[0-9]+")) submitSM.getSourceAddr().setNpi(Data.GSM_NPI_UNKNOWN);
            Name msisdn = new Name(submitSM.getSourceAddr().getAddress(), (byte)((submitSM.getSourceAddr().getNpi()|128)|submitSM.getSourceAddr().getTon()<<4));
            byte serviceType = SMPPServerApplication.getSMPPServerApplication().getServiceType();
            ForwardMOSMRequest forwardMOSMRequest = new ForwardMOSMRequest(serviceType, msisdn, session.getSessionProperties().getSmppNodeId(), session.getSessionProperties().getSmppSessionId(), smsSubmit);

            Message message = SMPPServerApplication.getSMPPServerApplication().getMcClient().dispatchMCRequest(forwardMOSMRequest);
            CompositeIE actionIE = (CompositeIE)message.getIE(MCTypeCollection.ACTION);
            if (actionIE!=null){
                byte result = actionIE.getValueByte();
                SubmitSMResp submitSMResp = (SubmitSMResp)submitSM.getResponse();
                String messageId=null;
                if ((byte)(result & MCTypeCollection.MESSAGE_ACTION) == MCTypeCollection.ACCEPT){
                    submitSMResp.setCommandStatus(Data.ESME_ROK);
                    CompositeIE idIE = (CompositeIE)message.getIE(MCTypeCollection.MESSAGE_ID);
                    if (idIE!=null){
                        messageId=idIE.getString();
                    }
                }
                else submitSMResp.setCommandStatus(Data.ESME_RSUBMITFAIL);
                try{
                    submitSMResp.setMessageId(messageId);
                }catch (WrongLengthOfStringException ex){
                    logger.severe(ex.getMessage());
                }
                session.send(submitSMResp);
            }
        }

        else if (pdu.getCommandId() == Data.DELIVER_SM){
            //the SMPP SubmitSM
//            session.
            DeliverSM deliverSM = (DeliverSM)pdu;

            //message throttling logic
//            logger.finest("messageLimit: "+ session.getSessionProperties().getMessageLimit()+" messageLimitWindow:"+ session.getSessionProperties().getMessageLimitWindow()+" messageCount:" +session.getMessageCount());
//            if (session.getSessionProperties().getMessageLimit()!=0){
//                if (session.getMessageCount()>=session.getSessionProperties().getMessageLimit()){
//                    Response response = deliverSM.getResponse();
//                    response.setCommandStatus(Data.ESME_RTHROTTLED);
//                    session.send(response);
//                    return;
//                }
//                else{
//                    session.increaseMessageCount();
//                }
//            }


            //the GSM TP-Reject-Duplicates parameter
            boolean tpRD=false;

            //The GSM TP-Reply-Path parameter
            boolean tpRP=false;

            //The GSM TP-User-Data-Header-Indicator parameter
            boolean tpUDHI=false;

            switch ((deliverSM.getEsmClass() & MASK_ESM_CLASS)>>6){
                case 1: tpUDHI=true; break;
                case 2: tpRP=true; break;
                case 3: tpUDHI=tpRP=true; break;
            }

            //The GSM TP-Status-Report-Request paremeter, is  unsupported for DeliverSM
            boolean tpSSR=false;
//            if (deliverSM.getRegisteredDelivery()==1) tpSSR=true; //only success&failure is supported

            //The GSM TP-Message-Reference paremeter
            byte tpMR;
            try{
                tpMR = (byte)deliverSM.getUserMessageReference();
            }
            catch (ValueNotSetException ex){
                tpMR = 0;
            }

            //The GSM TP-Protocol-Identifier parameter
            byte tpPID=deliverSM.getProtocolId();

            //The GSM TP-Destination-Address parameter
            Address tpDA = new Address((byte)(deliverSM.getDestAddr().getTon()<<4), deliverSM.getDestAddr().getNpi(), deliverSM.getDestAddr().getAddress());

            //The GSM TP-Data-Coding-Scheme paramter
            DCS tpDCS = new DCS(deliverSM.getDataCoding());

            //The GSM TP-User-Data parameter
            byte[] tpUD=getTpUD(pdu, tpDCS, tpUDHI);

            //just for testing
//            if (session instanceof ClientSession){
//                tpDA = new Address(Address.TON_NATIONAL, Address.NPI_ISDN, "70710269");
//            }

            //The GSM SMSSubmit
            SMSSubmit smsSubmit = new SMSSubmit(tpRD, tpRP, tpUDHI, tpSSR, tpMR, tpDA, tpPID, tpDCS,getTpUDL(),tpUD);
            Name msisdn = new Name(deliverSM.getSourceAddr().getAddress(), (byte)(deliverSM.getSourceAddr().getNpi()|deliverSM.getSourceAddr().getTon()));
            //just for testing
//            Name msisdn = new Name("103",(byte)0xA1);
            byte serviceType = SMPPClientApplication.getSMPPClientApplication().getServiceType();
            ForwardMOSMRequest forwardMOSMRequest = new ForwardMOSMRequest(serviceType, msisdn, session.getSessionProperties().getSmppNodeId(), session.getSessionProperties().getSmppSessionId(), smsSubmit);

            Message message = SMPPClientApplication.getSMPPClientApplication().getMcClient().dispatchMCRequest(forwardMOSMRequest);
            CompositeIE actionIE = (CompositeIE)message.getIE(MCTypeCollection.ACTION);
            if (actionIE!=null){
                byte result = actionIE.getValueByte();
                DeliverSMResp deliverSMResp = (DeliverSMResp)deliverSM.getResponse();
                String messageId=null;
                if ((byte)(result & MCTypeCollection.MESSAGE_ACTION) == MCTypeCollection.ACCEPT){
                    deliverSMResp.setCommandStatus(Data.ESME_ROK);
                }
                else deliverSMResp.setCommandStatus(Data.ESME_RSUBMITFAIL);

                CompositeIE idIE = (CompositeIE)message.getIE(MCTypeCollection.MESSAGE_ID);
                if (idIE!=null){
                    messageId=idIE.getString();
                }

                try{
                    deliverSMResp.setMessageId(messageId);
                }catch (WrongLengthOfStringException ex){
                    logger.severe(ex.getMessage());
                }
                deliverSM.assignSequenceNumber();
                session.send(deliverSMResp);
            }
//            endDate = new Date();
//            System.out.println("seq:"+deliverSM.getSequenceNumber()+" run-create:"+(runDate.getTime()-createDate.getTime()) + " end-run:"+(endDate.getTime()-runDate.getTime()));
        }
    }
    
    private byte[] getTpUD(PDU pdu, DCS dcs, boolean tpUDHI){
        byte [] shortMessageData=null;
        switch (pdu.getCommandId()){
            case Data.SUBMIT_SM:
                shortMessageData = ((SubmitSM)pdu).getShortMessageData();
                break;
            case Data.DELIVER_SM:
                shortMessageData = ((DeliverSM)pdu).getShortMessageData();
                break;
        }
        if (shortMessageData==null) return new byte[0];
        byte[] result;
        if (dcs.getCharSet()==DCS.CS_GSM7){
            //this pdu needs to be packed to 7-bit GSM

            //obtain udhl and udh. Note udhl includes itself
            int udhl;
            byte[] udh;
            byte[] tpud;
            byte[] tmpTpud;
            if (tpUDHI){
                udhl=(shortMessageData[0]&255)+1;
                udh = new byte[udhl];
                tpud=new byte[shortMessageData.length-udhl];
                System.arraycopy(shortMessageData, 0, udh, 0, udhl);
                System.arraycopy(shortMessageData, udhl, tpud, 0, shortMessageData.length-udhl);

                // also set the tpudl. See 3GPP TS 23.040 Section 9.2.3.16
                setTpUDL((udhl+(udhl/7)) + tpud.length+1); //+1 because tpdu has (ud length + uhd legth -1)
            }
            else{
                udhl=0;
                udh=null;
                tpud=shortMessageData;
                // also set the tpudl. See 3GPP TS 23.040 Section 9.2.3.16
                setTpUDL(tpud.length);
            }

            if (session.getSessionProperties().getEncodingInt()==(SessionProperties.ENCODING_LATIN1)){
                //this pdu needs transcoding from ISO 8859-1 to GSM 03.38 first
                TPUDLOverhead tpUDLOverhead = new TPUDLOverhead(0);
                tmpTpud=CharsetHelper.ISOLatinToGSM(tpud, tpUDLOverhead);
                setTpUDL(getIntTpUDL()+tpUDLOverhead.getOverhead());
            }
            else {
                //assume GSM 03.38
                tmpTpud=tpud;
            }
            result=CharsetHelper.encode7bitUserData(udh, tmpTpud);
        }
        else{
            //this pdu needn't be packed, send as it is
            result=shortMessageData;
            // also set the tpudl. See 3GPP TS 23.040 Section 9.2.3.16
            setTpUDL(shortMessageData.length);
        }
        return result;
    }

    public void setTpUDL(int tpUDL){
        this.tpUDL=(byte)tpUDL;
    }

    public byte getTpUDL(){
        return tpUDL;
    }
    public int getIntTpUDL(){
        return (int)tpUDL;
    }
}