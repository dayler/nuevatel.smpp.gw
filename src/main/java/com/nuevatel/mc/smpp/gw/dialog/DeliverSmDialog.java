/**
 * 
 */
package com.nuevatel.mc.smpp.gw.dialog;

import org.smpp.Data;
import org.smpp.ServerPDUEvent;
import org.smpp.pdu.DeliverSM;

import com.nuevatel.mc.smpp.gw.AllocatorService;
import com.nuevatel.mc.smpp.gw.SmppGwProcessor;
import com.nuevatel.mc.smpp.gw.event.DefaultResponseEvent;
import com.nuevatel.mc.smpp.gw.event.GenericNAckEvent;

/**
 * @author Ariel Salazar
 *
 */
public class DeliverSmDialog extends Dialog {
    
    private DeliverSM deliverPdu;
    
    private SmppGwProcessor gwProcessor;
    
    public DeliverSmDialog(long messageId, int processorId, DeliverSM deliverPdu) {
        super(messageId, processorId);
        gwProcessor = AllocatorService.getSmppGwProcessor(processorId);
        this.deliverPdu = deliverPdu;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init() {
        try {
            state = DialogState.init;
            // do received ok response to remote smsc
            DefaultResponseEvent respEv = new DefaultResponseEvent(deliverPdu);
            gwProcessor.offerSmppEvent(respEv);
            // TODO send to local smsc
            // check if it is ready to invalidate
            if ((deliverPdu.getRegisteredDelivery() & Data.SM_SMSC_RECEIPT_MASK) == Data.SM_SMSC_RECEIPT_NOT_REQUESTED) {
                state = DialogState.close;
                // Not request registered delivery
                invalidate();
            }
        } catch (Throwable ex) {
            // dispatch no ok
            gwProcessor.offerSmppEvent(new GenericNAckEvent(deliverPdu.getSequenceNumber(), Data.ESME_RSYSERR));
            // finalize dialog
            invalidate();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleSmppEvent(ServerPDUEvent ev) {
        // TODO Auto-generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DialogType getType() {
        return DialogType.deliverSm;
    }

    @Override
    public void execute() {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void invalidate() {
        if (!DialogState.close.equals(state)) {
            // No in close estate means an error occurred in the work flow.
            gwProcessor.offerSmppEvent(new GenericNAckEvent(deliverPdu.getSequenceNumber(), Data.ESME_RSYSERR));
        }
        super.invalidate();
    }
}
