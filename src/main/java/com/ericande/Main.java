package com.ericande;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String SKIP_GOOGLE_CALENDAR = "skipGoogleCalendar";
    private static final String SERIALIZE_CALENDAR = "serializeScheduleManager";
    private static final String NEXT_WEEK = "nextWeek";

    public static void main(String args[]) {
        List<String> myArguments = Arrays.asList(args);
        WebScraper myWebScraper = new WebScraper(myArguments.contains(SERIALIZE_CALENDAR), myArguments.contains(NEXT_WEEK));
        HtmlPage mySchedulePage = myWebScraper.processPage(WebScraper.TUTOR_URL);
        List<LocalDateTime> myHours = ScheduleParser.extractHours(mySchedulePage);
        System.out.println("Scheduled Hours: " + myHours);
        if (myArguments.contains(SKIP_GOOGLE_CALENDAR)) {
            System.out.println("Skipped exporting schedule to google calendar");
        } else {
            new EventPlacer().createEvents(myHours);
        }
    }
}
