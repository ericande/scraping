package com.ericande;

import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

class ScheduleParser {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy");
    private static final String SCHEDULED = "Scheduled!";
    private static final String HOURS = "cellHours";

    static List<LocalDateTime> extractHours(HtmlPage aHtmlPage) {
        String myWeek = aHtmlPage.getElementById("divSelectedWeek").getTextContent();
        System.out.println(myWeek);
        LocalDateTime myStartOfWeek = LocalDate.parse(myWeek.split(" ")[2], DATE_FORMAT).atStartOfDay();
        //System.out.println(myStartOfWeek);

        Iterator<DomElement> mySelectables = aHtmlPage.getElementById("selectable").getChildElements().iterator();
        List<LocalDateTime> myScheduledHours = new ArrayList<>();

        while (mySelectables.hasNext() && !mySelectables.next().getTextContent().equals("12AM")) {
            //Skip header
        }

        int myHourOffset = 0;
        int myDayOffset = 0;
        while (mySelectables.hasNext()) {
            DomElement myNode = mySelectables.next();
            if (myNode.getTextContent().equals(SCHEDULED)) {
                myScheduledHours.add(myStartOfWeek.plusHours(myHourOffset).plusDays(myDayOffset));
            }
            if (myNode.getId().startsWith(HOURS)) {
                myHourOffset++;
                myDayOffset = 0;
            } else {
                myDayOffset++;
            }
        }
        return myScheduledHours;
    }
}
