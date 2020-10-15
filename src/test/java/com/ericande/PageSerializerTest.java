package com.ericande;

import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class PageSerializerTest {
    private static final String EXAMPLE_PATH = "src/main/resources/TutorSchedulePage.ser";

    @Test
    public void reloadSavedPage() throws Exception {
        File myFile = new File(EXAMPLE_PATH);
        Object myReadObject = new ObjectInputStream(new FileInputStream(EXAMPLE_PATH)).readObject();
        HtmlPage myPage = (HtmlPage) myReadObject;
        for ( DomNode myElement : myPage.getElementById("selectable").getChildElements()) {
            System.out.println(myElement.getTextContent() + "\t" + myElement);
        }
    }

    @Test
    public void parseSavedPage() throws Exception {
        Object myO = new ObjectInputStream(new FileInputStream(EXAMPLE_PATH)).readObject();
        HtmlPage myPage = (HtmlPage) myO;
        System.out.println(ScheduleParser.extractHours(myPage));
    }
}