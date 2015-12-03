/**
 * 
 */
package com.nuevatel.mc.smpp.gw.exception;

/**
 * Used to prevent instantiation of new CachedObject in CacheLoader
 * 
 * @author Ariel Salazar
 *
 */
public class NoDialogCachedObject extends Exception {

    private static final long serialVersionUID = 20151201L;

    public NoDialogCachedObject() {
        super("No Dialog cached object");
    }
}
