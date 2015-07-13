/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.cache;

import com.logica.smpp.pdu.Address;


/**
 *
 * @author luis
 */
public class DeliveryReceiptAddressHolder {
    private Address sourceAddress;
    private Address destinationAddress;

    public DeliveryReceiptAddressHolder(Address sourceAddress, Address destinationAddess){
            this.sourceAddress=sourceAddress;
            this.destinationAddress=destinationAddess;
    }

    /**
     * @return the sourceAddress
     */
    public Address getSourceAddress() {
        return sourceAddress;
    }

    /**
     * @return the destinationAddress
     */
    public Address getDestinationAddress() {
        return destinationAddress;
    }
}
