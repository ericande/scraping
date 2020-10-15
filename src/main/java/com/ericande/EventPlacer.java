package com.ericande;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;

import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/*
All the communication with Google Calendar's API is quarantined here.

Much credit goes to the Google Calendar API Java Quickstart demo, available here
https://developers.google.com/calendar/quickstart/java
 */
class EventPlacer {
    private static final String DEFAULT_EVENT_TITLE = "Tutor.com Session";
    private static final String theCalendarID = "ericsteveanderson@gmail.com";
    private static final String CREDENTIALS_PATH = "client_secrets.json";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String TOKENS_PATH = "tokens";
    private Calendar theCalendarService;

    EventPlacer() {
        theCalendarService = createCalendarService();
    }

     void createEvents(List<LocalDateTime> aEventTimes) {
        if (theCalendarService == null) {
            throw new IllegalStateException("Calendar service not initialized, could not create events");
        }
        int myInsertedCount = 0;
        for (LocalDateTime myEventTime : aEventTimes) {
            try {
                Calendar.Events.Insert myRequest = theCalendarService.events()
                        .insert(theCalendarID, generateEvent(myEventTime));
                myRequest.execute();
                myInsertedCount++;
            } catch (IOException aE) {
                aE.printStackTrace();
                System.out.println("Aborting after " + myInsertedCount + " events created");
                return; //Errors likely to be correlated between different events, abort on error
            }
        }
        System.out.println("Successfully added " + myInsertedCount + " new events!");
    }

    private static Event generateEvent(LocalDateTime aEventTime) {
        Event myEvent = new Event();
        myEvent.setStart(new EventDateTime().setDateTime(new DateTime(toEasternUSTime(aEventTime))));
        myEvent.setEnd(new EventDateTime().setDateTime(new DateTime(toEasternUSTime(aEventTime.plusHours(1)))));
        myEvent.setSummary(DEFAULT_EVENT_TITLE);
        return myEvent;
    }

    private static Date toEasternUSTime(LocalDateTime aEventTime) {
        ZoneId myZoneId = ZoneId.of("America/New_York");
        ZonedDateTime myZonedDateTime = aEventTime.atZone(myZoneId);
        return Date.from(Instant.from(myZonedDateTime));
    }

    private static Credential getCredentials(final NetHttpTransport aHttpTransport) throws IOException {
        // Load client secrets.
        File myCredentialsFile = new File(CREDENTIALS_PATH);
        if (!myCredentialsFile.exists()) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_PATH);
        }
        FileInputStream myFileInputStream = new FileInputStream(myCredentialsFile);

        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(myFileInputStream));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                aHttpTransport,
                JSON_FACTORY,
                clientSecrets,
                SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    private Calendar createCalendarService() {
        try {
            NetHttpTransport myTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential myCredential = getCredentials(myTransport);
            return new Calendar.Builder(myTransport, JSON_FACTORY, myCredential)
                    .setApplicationName("Calendar Importer")
                    .build();
        } catch (Exception aE) {
            aE.printStackTrace();
            return null;
        }
    }
}
