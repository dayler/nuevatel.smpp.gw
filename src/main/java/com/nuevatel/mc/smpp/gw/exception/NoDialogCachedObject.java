
package com.nuevatel.mc.smpp.gw.exception;

/**
 * 
 * <p>The NoDialogCachedObject class.</p>
 * <p>Nuevatel PCS de Bolivia S.A. (c) 2016</p>
 * 
 * Used to prevent instantiation of new CachedObject in CacheLoader.
 * 
 * @author Ariel Salazar
 * @version 1.0
 * @since 1.8
 */
public class NoDialogCachedObject extends Exception {

    private static final long serialVersionUID = 20151201L;

    /**
     * NoDialogCachedObject constructor.
     */
    public NoDialogCachedObject() {
        super("No Dialog cached object");
    }
}
