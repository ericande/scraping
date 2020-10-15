package com.ericande;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.io.*;

/*
    Contains utility methods for serializing HtmLPage and related classes
 */
class PageSerializer {
    private static final String SERIALIZED = ".ser";
    private static final String PREFIX = "src/main/resources/";
    private static final String PLAIN_TEXT = ".txt";
    private static final String XML = ".xml";
    private String theFName = "untitled";

    void savePage(Serializable aHtmlPage) {
        File myFile = fileName(SERIALIZED);
        try {
            myFile.createNewFile();
            FileOutputStream myFOS = new FileOutputStream(myFile);
            ObjectOutputStream myOutputStream = new ObjectOutputStream(myFOS);
            myOutputStream.writeObject(aHtmlPage);
        } catch (IOException aE) {
            System.out.println("ERROR: Could not serialize file: " + myFile.toString());
            aE.printStackTrace();
        }
    }

    void stringSave(Object aO) {
        File myFile = fileName(PLAIN_TEXT);
        try {
            myFile.createNewFile();
            FileWriter myFileWriter = new FileWriter(myFile);
            myFileWriter.write(aO.toString());
            myFileWriter.flush();
            myFileWriter.close();
        } catch (IOException aE) {
            System.out.println("ERROR: Could not serialize file: " + myFile.toString());
            aE.printStackTrace();
        }
    }

    void xmlSave(HtmlPage aPage) {
        File myFile = fileName(XML);
        try {
            myFile.createNewFile();
            FileWriter myFileWriter = new FileWriter(myFile);
            myFileWriter.write(aPage.asXml());
            myFileWriter.flush();
            myFileWriter.close();
        } catch (IOException aE) {
            System.out.println("ERROR: Could not save xml for file: " + myFile.getName());
            aE.printStackTrace();
        }
    }

    PageSerializer setFname(String aFName) {
        theFName = aFName;
        return this;
    }

    private File fileName(String aSuffix) {
        File myFile = new File(PREFIX + theFName + aSuffix);
        int counter = 1;
        while (myFile.exists()) {
            myFile = new File(PREFIX + theFName + counter + aSuffix);
            counter++;
        }
        return myFile;
    }
}
