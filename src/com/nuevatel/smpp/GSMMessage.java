/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.nuevatel.mc.appconn.Name;
import com.nuevatel.mc.tpdu.TPDU;

/**
 *
 * @author luis
 */
public class GSMMessage {

    private TPDU tpdu;
    private Name msisdn;
    private String id;
    private Integer smppSessionId;


    public GSMMessage(String id, Name msisdn, TPDU tpdu, Integer smppSessionId){
        this.id=id;
        this.msisdn=msisdn;
        this.tpdu=tpdu;
        this.smppSessionId=smppSessionId;
    }

    /**
     * @return the tpdu
     */
    public TPDU getTpdu() {
        return tpdu;
    }

    /**
     * @return the msisdn
     */
    public Name getMsisdn() {
        return msisdn;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @return the smppSessionId
     */
    public Integer getSmppSessionId() {
        return smppSessionId;
    }
}
