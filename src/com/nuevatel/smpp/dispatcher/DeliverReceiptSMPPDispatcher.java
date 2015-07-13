/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.dispatcher;

import com.logica.smpp.pdu.DeliverSM;
import com.logica.smpp.pdu.WrongLengthOfStringException;
import com.nuevatel.mc.appconn.Name;
import com.nuevatel.smpp.cache.CacheHandler;
import com.nuevatel.smpp.session.Session;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

/**
 *
 * @author luis
 */
public class DeliverReceiptSMPPDispatcher implements Runnable{
    /*private variables*/
    private String messageId;
    private String referenceId;
    private Name fromName;
    private Name toName;
    private Integer smppSessionId;
    private Session session;
    private DateFormat dateFormat=new SimpleDateFormat("yyMMddHHmm");


    /*constants*/
    private static final byte DR_ESME_CLASS = (byte)0x04;
    private static final byte DR_DATA_CODING = (byte)0x00;
    private static final String STAT_DELIVRD = "DELIVRD";
    private static final String STAT_UNDELIV = "UNDELIV";
    private static final String ERR_OK = "000";
    private static final String ERR_NOK = "001";

    private static final Logger logger = Logger.getLogger(DeliverReceiptSMPPDispatcher.class.getName());

    public DeliverReceiptSMPPDispatcher(String messageId, String referenceId, Name fromName, Name toName, Integer smmppSessionId){
        this.messageId=messageId;
        this.referenceId=referenceId;
        this.fromName=fromName;
        this.toName=toName;
        this.smppSessionId=smmppSessionId;
        session = CacheHandler.getCacheHandler().getSessionCache().get(smppSessionId);
    }

    public void run() {
        try{
            DeliverSM deliverSM = new DeliverSM();

            //set the esm_class
            deliverSM.setEsmClass(DR_ESME_CLASS);

            //the data_coding
            deliverSM.setDataCoding(DR_DATA_CODING);

            //set the source address
            byte srcTon=(byte)((toName.getType() & 0x70)>>4);
            byte srcNpi=(byte)(toName.getType() & 0x0f);
            String srcAddress=toName.getName();
            deliverSM.setSourceAddr(srcTon, srcNpi, srcAddress);

            //set the destination address
            byte dstTon=(byte)((fromName.getType() & 0x70)>>4);
            byte dstNpi=(byte)(fromName.getType() & 0x0f);
            String dstAddress=fromName.getName();
            deliverSM.setDestAddr(dstTon, dstNpi, dstAddress);

            //set the short message
            Date doneDate = new Date();
            StringBuilder builder = new StringBuilder();
            builder.append("id:");
            builder.append(referenceId);
            builder.append(" sub:001 dlvrd:001");
            builder.append(" submit date:");
            builder.append(dateFormat.format(doneDate));
            builder.append(" done date:");
            builder.append(dateFormat.format(doneDate));
            builder.append(" stat:");
            builder.append(STAT_DELIVRD);
            builder.append(" err:");
            builder.append(ERR_OK);
            deliverSM.setShortMessage(builder.toString());
            deliverSM.assignSequenceNumber();
            CacheHandler.getCacheHandler().getPendingResponsesMap().put(deliverSM.getSequenceNumber(), messageId);
            session.send(deliverSM);
            
        } catch (WrongLengthOfStringException ex) {
            logger.warning("statusReport: "+referenceId+" "+ex.getMessage());
        }
        
    }

}
