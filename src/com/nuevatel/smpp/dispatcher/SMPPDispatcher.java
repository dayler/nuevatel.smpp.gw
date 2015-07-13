/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.dispatcher;

import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.SubmitSM;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.nuevatel.mc.appconn.ForwardMTSMAdvice;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.tpdu.DCS;
import com.nuevatel.mc.tpdu.SMSDeliver;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.GSMMessage;
import com.nuevatel.smpp.SMPPApplication;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.SMPPServerApplication;
import com.nuevatel.smpp.session.ClientSession;
import com.nuevatel.smpp.session.ServerSession;
import com.nuevatel.smpp.session.Session;
import com.nuevatel.smpp.session.SessionProperties;
import com.nuevatel.smpp.utils.CharsetHelper;
import com.nuevatel.smpp.utils.StringUtils;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class SMPPDispatcher implements Runnable{
    
    private GSMMessage gsmMessage;
    private Session session;
    private int appType;
    private static final Logger logger = Logger.getLogger(SMPPDispatcher.class.getName());

    private static final byte TON = 112;
    private static final byte NPI = 15;
    private final int SESSION_NOT_FOUND_SERVICE_MESSAGE=404;

    public SMPPDispatcher(GSMMessage gsmMessage, int appType){
        this.gsmMessage=gsmMessage;
        this.appType=appType;
        session = CacheHandler.getCacheHandler().getSessionCache().get(gsmMessage.getSmppSessionId());
    }
    public void run() {
        if (session==null){
            //nobody connected
            logger.warning("SMPP SessionId: "+gsmMessage.getSmppSessionId()+" not found. MessageId: "+gsmMessage.getId());
            ForwardMTSMAdvice advice = new ForwardMTSMAdvice(gsmMessage.getId(), MCTypeCollection.REQUEST_FAILED, SESSION_NOT_FOUND_SERVICE_MESSAGE);
            RunnableScheduledFuture expirationResult = CacheHandler.getCacheHandler().getExpirationTasksMap().get(gsmMessage.getId());
            if (expirationResult!=null){
                expirationResult.cancel(false);
                CacheHandler.getCacheHandler().getExpirationTasksMap().remove(gsmMessage.getId());
            }
            if (appType==SMPPApplication.APP_TYPE_CLIENT){
                SMPPClientApplication.getSMPPClientApplication().getMcClient().writeMCAdvice(advice);
            }
            else if (appType==SMPPApplication.APP_TYPE_SERVER){
                SMPPServerApplication.getSMPPServerApplication().getMcClient().writeMCAdvice(advice);
            }
            return;
        }
//        logger.finest(getDebugInfo(gsmMessage));
        SMSDeliver smsDeliver=(SMSDeliver)gsmMessage.getTpdu();


        if (session instanceof ServerSession){
            //create the deliverSM skeleton
            DeliverSM deliverSM=new DeliverSM();

            //set the source address
            byte srcTon=(byte)(smsDeliver.getOA().getTON()>>4);
            byte srcNpi=(byte)(smsDeliver.getOA().getNPI());
            String srcAddress=smsDeliver.getOA().getAddress();
            try {
                deliverSM.setSourceAddr(srcTon, srcNpi, srcAddress);
            } catch (WrongLengthOfStringException ex) {
                logger.warning(gsmMessage.getId()+" "+ex.getMessage());
            }

            //set the destination address
            Name dstName=gsmMessage.getMsisdn();
            byte dstTon=(byte)((dstName.getType() & 0x70)>>4);
            byte dstNpi=(byte)(dstName.getType() & 0x0f);
            String dstAddress=dstName.getName();
            try {
                deliverSM.setDestAddr(dstTon, dstNpi, dstAddress);
            } catch (Exception ex) {
                logger.warning(gsmMessage.getId()+" "+ex.getMessage());
            }

            //set the esm_class
            byte esmClass=0;
            if (smsDeliver.getUDHI()){
                if (smsDeliver.getRP()) esmClass=(byte)192;
                else esmClass=(byte)64;
            }
            else if(smsDeliver.getRP()) esmClass=(byte)128;
            deliverSM.setEsmClass(esmClass);

            //set the protocolID
            deliverSM.setProtocolId(smsDeliver.getPID());

            //set the RegisteredDelivery is unsupported for deliverSM
//            if (smsDeliver.getSRI()){
//                deliverSM.setRegisteredDelivery((byte)1);
//            }

            //set the coding
            byte dcs=smsDeliver.getDCS().getDCS();
            deliverSM.setDataCoding(dcs);

            //set the ud
//            logger.severe("ud: "+gsmMessage.getId());
            byte[] ud = getShortMessageData(smsDeliver.getUD(), smsDeliver.getDCS(), smsDeliver.getUDHI());
//            logger.severe("setShortMessage: "+gsmMessage.getId());
            deliverSM.setShortMessageData(ud);
            if (deliverSM.getShortMessageData().length==0 && CacheHandler.getCacheHandler().getEmptyUDString()!=null){
                try {
                    //empty message, replace with default String
                    deliverSM.setShortMessage(CacheHandler.getCacheHandler().getEmptyUDString());
                } catch (WrongLengthOfStringException ex) {
                    logger.warning("Cannot set emptyUDString "+CacheHandler.getCacheHandler().getEmptyUDString()+" to "+gsmMessage.getId());
                }
            }
//            logger.severe("assignSquenceNumber: "+gsmMessage.getId());
            deliverSM.assignSequenceNumber();
            CacheHandler.getCacheHandler().getPendingResponsesMap().put(deliverSM.getSequenceNumber(), gsmMessage.getId());
//            logger.severe("send: "+gsmMessage.getId());
            session.send(deliverSM);
        }
        else if (session instanceof ClientSession){

            ClientSession clientSession = (ClientSession)session;

            /**
             * Queueing and Message limit logic:
             * 1) if Queue is not empty then the dispatchers already in the queue should have priority. This dispatcher
             * should be sent to the tail of the queue
             * 2) if there is a message limit:
             * 2.1)if the current message count has surpassed such limit then this dispatcher
             * should be sent to the tail of the queue. there it will have priority over new Dispatchers
             * 2.2) if the current message count hasn't surpassed such limit just increase the
             * counter and then execute this dispatcher
             * 3) if there is no message limit (getMessageLimit()==0) then just process message;
             */

            //1)
            if (!clientSession.getDispatcherQueue().isEmpty()){
                clientSession.getDispatcherQueue().add(this);
                return;
            }
            //2)
            if (clientSession.getSessionProperties().getMessageLimit()!=0 ){
                //2.1)
                if (clientSession.getMessageCount()>=clientSession.getSessionProperties().getMessageLimit()){
                    clientSession.getDispatcherQueue().add(this);
                    return;
                }
                //2.2)
                else{
                    clientSession.increaseMessageCount();
                }
            }
            //3)
            SubmitSM submitSM=new SubmitSM();
//            System.out.println(getDebugInfo(gsmMessage));
            //set the source address
            byte srcTon;
            byte srcNpi;
            if (CacheHandler.getCacheHandler().getSmppSourceTon()!=null){
                srcTon=CacheHandler.getCacheHandler().getSmppSourceTon();
            }
            else{
                srcTon=(byte)(smsDeliver.getOA().getTON()>>4);
            }
            if (CacheHandler.getCacheHandler().getSmppSourceNpi()!=null){
                srcNpi=CacheHandler.getCacheHandler().getSmppSourceNpi();
            }
            else {
                srcNpi=(byte)(smsDeliver.getOA().getNPI());
            }
            
            String srcAddress=smsDeliver.getOA().getAddress();
            try {
                submitSM.setSourceAddr(srcTon, srcNpi, srcAddress);
            } catch (WrongLengthOfStringException ex) {
                logger.warning(gsmMessage.getId()+" "+ex.getMessage());
            }

            //set the destination address
            Name dstName=gsmMessage.getMsisdn();
            byte dstTon;
            byte dstNpi;
            if (CacheHandler.getCacheHandler().getSmppDestinationTon()!=null){
                dstTon = CacheHandler.getCacheHandler().getSmppDestinationTon();
            }
            else {
                dstTon=(byte)((dstName.getType() & 0x70)>>4);
            }
            if (CacheHandler.getCacheHandler().getSmppDestinationNpi()!=null){
                dstNpi = CacheHandler.getCacheHandler().getSmppDestinationNpi();
            }
            else {
                dstNpi=(byte)(dstName.getType() & 0x0f);
            }
            String dstAddress=dstName.getName();
            try {
                submitSM.setDestAddr(dstTon, dstNpi, dstAddress);
            } catch (WrongLengthOfStringException ex) {
                logger.warning(gsmMessage.getId()+" "+ex.getMessage());
            }
//            System.out.println(submitSM.getSourceAddr().getTon());
//            System.out.println(submitSM.getSourceAddr().getNpi());
//            System.out.println(submitSM.getDestAddr().getTon());
//            System.out.println(submitSM.getDestAddr().getNpi());
            //set the esm_class
            byte esmClass=0;
            if (smsDeliver.getUDHI()){
                if (smsDeliver.getRP()) esmClass=(byte)192;
                else esmClass=(byte)64;
            }
            else if(smsDeliver.getRP()) esmClass=(byte)128;
            submitSM.setEsmClass(esmClass);

            //set the protocolID
            submitSM.setProtocolId(smsDeliver.getPID());

            //set the RegisteredDelivery
            if (session.getSessionProperties().enableSR() && smsDeliver.getSRI()){
                submitSM.setRegisteredDelivery((byte)1);
            }

            //set the coding
            byte dcs=smsDeliver.getDCS().getDCS();
            submitSM.setDataCoding(dcs);

            //set the ud
            byte[] ud = getShortMessageData(smsDeliver.getUD(), smsDeliver.getDCS(), smsDeliver.getUDHI());
            submitSM.setShortMessageData(ud);
            if (submitSM.getShortMessageData().length==0 && CacheHandler.getCacheHandler().getEmptyUDString()!=null){
                try {
                    //empty message, replace with default String
                    submitSM.setShortMessage(CacheHandler.getCacheHandler().getEmptyUDString());
                } catch (WrongLengthOfStringException ex) {
                    logger.warning("Cannot set emptyUDString "+CacheHandler.getCacheHandler().getEmptyUDString()+" to "+gsmMessage.getId());
                }
            }
            submitSM.assignSequenceNumber();
            if (submitSM.getRegisteredDelivery()==0){
                //only put id in map if not registed_delivery, if registered_delivery=true then the messageId will be in the delivery receipt
//                logger.finest("Saving in pendingResponses:"+submitSM.getSequenceNumber()+" "+gsmMessage.getId());
                CacheHandler.getCacheHandler().getPendingResponsesMap().put(submitSM.getSequenceNumber(), gsmMessage.getId());
            }
//             just for testing
//            try{
//                submitSM.setSourceAddr("103");
//                submitSM.setDestAddr("70100044");
//            } catch (Exception ex){
//                ex.printStackTrace();
//            }
            clientSession.send(submitSM);
        }

        

    }

    private byte[] getShortMessageData(byte[] tpud, DCS dcs, boolean tpUDHI){
        if (tpud==null){
//            logger.severe("tpud null: "+gsmMessage.getId());
            return new byte[0];
        }
        byte[] result;

        if (dcs.getCharSet()==DCS.CS_GSM7){
            //this tpud needs to be unpacked

            //obtain udhl and udh. Note: udhl counts itself
            int udhl;
            byte[] udh;
            if (tpUDHI){
//                logger.severe("tpUDHI: "+gsmMessage.getId());
                udhl=(tpud[0]&255)+1;
//                logger.severe("udhl: "+gsmMessage.getId());
                udh = new byte[udhl];
//                logger.severe("udh: "+gsmMessage.getId());
                try{
//                    logger.severe("arraycopy: "+gsmMessage.getId());
                    System.arraycopy(tpud, 0, udh, 0, udhl);
                }catch (Exception e){
                    System.out.println(StringUtils.getHex(tpud));
                    e.printStackTrace();
                }
//                logger.severe("endUDHI: "+gsmMessage.getId());
            }
            else{
//                logger.severe("udhl=0: "+gsmMessage.getId());
                udhl=0;
                udh=null;
            }
//            logger.severe("decode7bitEncoding: "+gsmMessage.getId());
//            logger.severe("decode7bitEncoding: "+gsmMessage.getId()+" "+StringUtils.getHex(udh)+ " "+StringUtils.getHex(tpud));
            byte[] tmpResult=CharsetHelper.decode7bitEncoding(udh, tpud);
//            logger.severe("decode7bitEncoding: "+gsmMessage.getId()+" "+StringUtils.getHex(tmpResult));
            if (session.getSessionProperties().getEncodingInt()==SessionProperties.ENCODING_LATIN1){
                //this tpdu needs to be transcoded from GSM 03.38 to ISO 8859-1 first
                if (udh!=null){
                    //lets be careful not to transcode the UDH
                    byte[] tmpUnpacked = new byte[tmpResult.length-udhl];
                    System.arraycopy(tmpResult, udhl, tmpUnpacked, 0, tmpUnpacked.length);
//                    logger.severe("transcoded: "+gsmMessage.getId());
                    byte[] tmpTranscoded = CharsetHelper.GSMToISOLatin(tmpUnpacked);
//                    logger.severe("udhl+transcoded: "+gsmMessage.getId());
                    result = new byte[udhl+tmpTranscoded.length];
                    System.arraycopy(udh, 0, result, 0, udhl);
                    System.arraycopy(tmpTranscoded, 0, result, udhl, tmpTranscoded.length);
                }
                else{
//                    logger.severe("GSMToISOLatin: "+gsmMessage.getId());
//                    logger.severe("GSMToISOLatin: "+gsmMessage.getId()+" "+StringUtils.getHex(tmpResult));
                    result=CharsetHelper.GSMToISOLatin(tmpResult);
                }
            }
            else {
                result=tmpResult;
            }
        }
        else {
            //send as it is
            result=tpud;
        }
//        logger.severe("return: "+gsmMessage.getId());
        return result;
    }

    private String getDebugInfo(GSMMessage gsmMessage){
        StringBuilder builder = new StringBuilder("Received GSM message:\n");
        builder.append("id:").append(gsmMessage.getId()).append("\n");
        builder.append("smppSessionId: ").append(gsmMessage.getSmppSessionId()).append("\n");
        builder.append("Msisdn Name: ").append(gsmMessage.getMsisdn().getName()).append("\n");
        builder.append("Msisdn type: ").append(gsmMessage.getMsisdn().getType()).append("\n");
        SMSDeliver smsDeliver = (SMSDeliver)gsmMessage.getTpdu();
        builder.append("TP-MMS:").append(smsDeliver.getMMS()).append("\n");
        builder.append("TP-RP:").append(smsDeliver.getRP()).append("\n");
        builder.append("TP-UDHI:").append(smsDeliver.getUDHI()).append("\n");
        builder.append("TP-SRI:").append(smsDeliver.getSRI()).append("\n");
        builder.append("TP-OA Address:").append(smsDeliver.getOA().getAddress()).append("\n");
        builder.append("TP-OA Ton:").append(smsDeliver.getOA().getTON()).append("\n");
        builder.append("TP-OA NPI:").append(smsDeliver.getOA().getNPI()).append("\n");
        builder.append("TP-PID:").append(smsDeliver.getPID()).append("\n");
        builder.append("TP-DCS:").append(smsDeliver.getDCS().getDCS()).append("\n");
        builder.append("TP-SCTS:").append(smsDeliver.getSCTS()).append("\n");
        builder.append("TP-UDL:").append(smsDeliver.getUDL()).append("\n");
        builder.append("TP-UD:").append(StringUtils.getHex(smsDeliver.getUD())).append("\n");
        return builder.toString();
    }

    public String getId(){
        return gsmMessage.getId();
    }
}
