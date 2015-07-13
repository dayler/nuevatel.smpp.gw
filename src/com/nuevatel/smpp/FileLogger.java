/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.nuevatel.smpp;

import com.nuevatel.smpp.cache.CacheHandler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;

/**
 *
 * @author luis
 */
public class FileLogger {

    private static final String PATH="/mc/deliveryReceipts/";
    private static final String nameFormat="delivery_%1$tY%1$tm%1$td";
    private static FileLogger fileLogger=null;

    private String name;
    private File file;
    private Writer writer;
    private int mcNodeId;
    private int dayOfYear;
    private int year;


    private FileLogger(){}
    private FileLogger(int mcNodeId) throws IOException{
        this.mcNodeId=mcNodeId;
        name=String.format(nameFormat+".txt", new Date());
        File pathFile = new File(PATH+String.valueOf(this.mcNodeId));
        pathFile.mkdirs();
        file = new File(PATH+String.valueOf(this.mcNodeId)+"/"+name);
        writer = new BufferedWriter(new FileWriter(file,true));
        year = Calendar.getInstance().get(Calendar.YEAR);
        dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
    }

    public static FileLogger getFileLogger(){
        if (fileLogger==null) try {
            fileLogger = new FileLogger(CacheHandler.getCacheHandler().getMCNodeId());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return fileLogger;
    }

    /**
     * @return the writer
     */
    public synchronized void write(String format, Object... args) {
        try {
            writer.write(String.format(format, args));

            int tmpYear = Calendar.getInstance().get(Calendar.YEAR);
            int tmpDayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
            if (tmpDayOfYear>dayOfYear || tmpYear>year){
                writer.flush();
                writer.close();
                name=String.format(nameFormat+".txt", new Date());
                file = new File(PATH+this.mcNodeId+"/"+name);
                writer = new BufferedWriter(new FileWriter(file,true));
                year = tmpYear;
                dayOfYear = tmpDayOfYear;
            }
            writer.flush();
            
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
