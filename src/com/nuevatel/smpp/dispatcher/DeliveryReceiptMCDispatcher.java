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
import com.logica.smpp.pdu.SubmitSMResp;
import com.nuevatel.mc.appconn.ForwardMTSMAdvice;
import com.nuevatel.mc.appconn.MCTypeCollection;
import com.nuevatel.smpp.FileLogger;
import com.nuevatel.smpp.SMPPClientApplication;
import com.nuevatel.smpp.SMPPServerApplication;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.ClientSession;
import com.nuevatel.smpp.session.ServerSession;
import com.nuevatel.smpp.session.Session;
import java.util.Date;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class DeliveryReceiptMCDispatcher implements Runnable{

    private static final Logger logger = Logger.getLogger(DeliveryReceiptMCDispatcher.class.getName());
    private static final String formatString="%1$tY-%1$tm-%1$td %1$tH:%1$tM:%1$tS\t%2$s\t%3$s\t%4$s\n";
    private PDU pdu;
    private Session session;

    public DeliveryReceiptMCDispatcher(PDU pdu, Session session){
        this.pdu=pdu;
        this.session=session;
    }

    public void run() {
        if (pdu.isResponse()){
           Response response = (Response)pdu;
            if (response.getCommandId()==Data.DELIVER_SM_RESP || response.getCommandId()==Data.SUBMIT_SM_RESP){
                String id = CacheHandler.getCacheHandler().getPendingResponsesMap().get(response.getSequenceNumber());
//                logger.finest("Removing from pendingResponses:"+response.getSequenceNumber()+" "+id);
                if (id != null){
//                    logger.finest("about to cancel expiration for:" + id);
                    ForwardMTSMAdvice advice;
                    //cancel message expiration
                    RunnableScheduledFuture expirationResult = CacheHandler.getCacheHandler().getExpirationTasksMap().get(id);
                    if (expirationResult!=null){
                        expirationResult.cancel(false);
                        CacheHandler.getCacheHandler().getExpirationTasksMap().remove(id);
                        if (response.getCommandStatus()==Data.ESME_ROK){
                            advice = new ForwardMTSMAdvice(id, MCTypeCollection.REQUEST_ACCEPTED, null);
                        }
                        else if(response.getCommandStatus() == Data.ESME_RINVMSGLEN)
                        {
                            advice = new ForwardMTSMAdvice(id, MCTypeCollection.REQUEST_FAILED, response.getCommandStatus());
                        }
                        else{
                            advice = new ForwardMTSMAdvice(id, MCTypeCollection.REQUEST_FAILED, response.getCommandStatus());
                        }
                        if (session instanceof ClientSession){
                            SMPPClientApplication.getSMPPClientApplication().getMcClient().writeMCAdvice(advice);
                        }
                        else if (session instanceof ServerSession){
                            SMPPServerApplication.getSMPPServerApplication().getMcClient().writeMCAdvice(advice);
                        }
                        CacheHandler.getCacheHandler().getPendingResponsesMap().remove(response.getSequenceNumber());
                    }
                    else {
                        logger.finest("DELIVER_SM_RESP for "+id+" after message expired");
                    }
                }
                else {
                    if (pdu.getCommandId()==Data.SUBMIT_SM_RESP){         
                        // this may be an SubmitSMResp to a SubmitSM with SRI=true. Save the id and seq
                        SubmitSMResp submitSMResp = (SubmitSMResp)pdu;
                        CacheHandler.getCacheHandler().getPendingResponsesSRIMap().put(submitSMResp.getMessageId(), submitSMResp.getSequenceNumber());
                    }

                }
            if (CacheHandler.getCacheHandler().isFileLog()){
                String smppMessageId;
                if (pdu.getCommandId()==Data.SUBMIT_SM_RESP) smppMessageId=((SubmitSMResp)pdu).getMessageId();
                else smppMessageId=((DeliverSMResp)pdu).getMessageId();
                FileLogger.getFileLogger().write(formatString, new Date(),id,smppMessageId, pdu.getCommandStatus());
            }
            }
        }
        else if (pdu.isRequest()){
            //this is a delivery receipt
            DeliverSM deliverSM = (DeliverSM)pdu;
            Response deliverSMResponse = deliverSM.getResponse();
            int sdIndex;
            String shortMessage = deliverSM.getShortMessage();
            sdIndex = shortMessage.indexOf("sub:");
            if (sdIndex==-1) sdIndex = shortMessage.indexOf("submit date");
            String tmpId = shortMessage.substring(3,sdIndex-1);
            Integer tmpSeq = CacheHandler.getCacheHandler().getPendingResponsesSRIMap().get(tmpId);
            if (tmpSeq!=null){
                //found sequence number, lookup id
                String id =CacheHandler.getCacheHandler().getPendingResponsesMap().get(tmpSeq);
                if (id!=null){
                    ForwardMTSMAdvice advice;
                    //cancel message expiration
                    RunnableScheduledFuture expirationResult = CacheHandler.getCacheHandler().getExpirationTasksMap().get(id);
                    if (expirationResult!=null){
                        expirationResult.cancel(false);
                        CacheHandler.getCacheHandler().getExpirationTasksMap().remove(id);
                        if (shortMessage.contains("DELIVRD")){
                            advice = new ForwardMTSMAdvice(tmpId, MCTypeCollection.REQUEST_ACCEPTED, null);
                        }
                        else{
                            int errIndex = deliverSM.getShortMessage().indexOf("err:");
                            Integer serviceMessage = Integer.valueOf(shortMessage.substring(errIndex+4, errIndex+7));
                            advice = new ForwardMTSMAdvice(tmpId, MCTypeCollection.REQUEST_FAILED, serviceMessage);
                        }
                        SMPPServerApplication.getSMPPServerApplication().getMcClient().writeMCAdvice(advice);
                        CacheHandler.getCacheHandler().getPendingResponsesMap().remove(tmpSeq);
                        CacheHandler.getCacheHandler().getPendingResponsesSRIMap().remove(tmpId);

                        //now send response to ESME
                        deliverSMResponse.setCommandStatus(Data.ESME_ROK);
                        session.send(deliverSMResponse);
                    }
                    else {
                        logger.finest("DELIVER_SM Receipt for "+tmpId+" after message expired");
                    }
                }
            }
        }
    }
}
