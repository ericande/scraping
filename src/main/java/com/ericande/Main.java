package com.ericande;

import com.gargoylesoftware.htmlunit.html.HtmlPage;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public class Main {
    private static final String SKIP_GOOGLE_CALENDAR = "skipGoogleCalendar";
    private static final String SERIALIZE_CALENDAR = "serializeScheduleManager";

    public static void main(String[] args) {
        WebScraper myWebScraper = new WebScraper(Arrays.asList(args).contains(SERIALIZE_CALENDAR));
        HtmlPage mySchedulePage = myWebScraper.processPage(WebScraper.TUTOR_URL);
        List<LocalDateTime> myHours = ScheduleParser.extractHours(mySchedulePage);
        System.out.println("Scheduled Hours: " + myHours);
        if (Arrays.asList(args).contains(SKIP_GOOGLE_CALENDAR)) {
            System.out.println("Skipped exporting schedule to google calendar");
        } else {
            new EventPlacer().createEvents(myHours);
        }
    }
}
