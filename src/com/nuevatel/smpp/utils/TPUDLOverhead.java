/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp.utils;

/**
 *
 * @author luis
 */
public class TPUDLOverhead {
    private int overhead;

    public TPUDLOverhead(int overhead){
        this.overhead=overhead;
    }

    /**
     * @return the overhead
     */
    public int getOverhead() {
        return overhead;
    }

    /**
     * @param overhead the overhead to set
     */
    public void setOverhead(int overhead) {
        this.overhead = overhead;
    }

}
