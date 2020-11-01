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
import com.google.api.services.calendar.model.Events;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/*
All the communication with Google Calendar's API is quarantined here.

Much credit goes to the Google Calendar API Java Quickstart demo, available here
https://developers.google.com/calendar/quickstart/java
 */
class EventPlacer {
    private static final String DEFAULT_EVENT_TITLE = "Tutor.com Session";
    private static final String THE_GOOGLE_ID = "ericsteveanderson@gmail.com";
    private static final String CREDENTIALS_PATH = "client_secrets.json";
    private static final String EMAIL_TO_PATH = "email_recipients.txt";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    //private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_SEND, CalendarScopes.CALENDAR);
    private static final String TOKENS_PATH = "tokens";
    private static final String NAME = "Calendar Importer";
    private static final String EMAIL_PATTERN = "..*@.*\\..*";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMMM d - hh:mm a");
    private Calendar theCalendarService;
    private Gmail theGmailService;

    EventPlacer() {
        createServices();
    }

     void createEvents(List<LocalDateTime> aEventTimes) {
        if (theCalendarService == null) {
            throw new IllegalStateException("Calendar service not initialized, could not create events");
        }
        if (aEventTimes.isEmpty()) {
            System.out.println("No hours scheduled, aborting export to Google Calendar");
            return;
        }
        Set<EventDateTime> myExistingEventTimes = new HashSet<>();
         try {
             Events myEvents = theCalendarService.events().list("primary")
                     .setTimeMax(new DateTime(toEasternUSTime(Collections.max(aEventTimes).plusDays(1))))
                     .setTimeMin(new DateTime(toEasternUSTime(Collections.min(aEventTimes).minusDays(1))))
                     .execute();
             for (Event myEvent : myEvents.getItems()) {
                 if (myEvent.getSummary().equals(DEFAULT_EVENT_TITLE)) {
                     myExistingEventTimes.add(myEvent.getStart());
                 }
             }
         } catch (IOException aE) {
             System.out.println("Could not retrieve existing events, duplicate events may be placed");
             aE.printStackTrace();
         }
         int myInsertedCount = 0;
        for (LocalDateTime myEventTime : aEventTimes) {
            try {
                Event myEvent = generateEvent(myEventTime);
                if (myExistingEventTimes.contains(myEvent.getStart())) {
                    continue;
                }
                Calendar.Events.Insert myRequest = theCalendarService.events()
                        .insert(THE_GOOGLE_ID, myEvent);
                myRequest.execute();
                myInsertedCount++;
            } catch (IOException aE) {
                aE.printStackTrace();
                System.out.println("Aborting after " + myInsertedCount + " events created");
                return; //Errors likely to be correlated between different events, abort on error
            }
        }
        System.out.println("Successfully added " + myInsertedCount + " new events!");
        sendEmail(aEventTimes);
     }

    private void sendEmail(List<LocalDateTime> aScheduledHours) {
        try {
            File myRecipientsFile = new File(EMAIL_TO_PATH);
            if (!myRecipientsFile.exists()) {
                return;
            }
            List<String> myRecipients = new ArrayList<>();
            FileInputStream myFIS = new FileInputStream(myRecipientsFile);
            Scanner myScanner = new Scanner(myFIS);
            while (myScanner.hasNextLine()) {
                String myNext = myScanner.nextLine();
                if (myNext.matches(EMAIL_PATTERN)) {
                    myRecipients.add(myNext);
                } else {
                    System.out.println("WARN: Invalid address for recipient: " + myNext + ". Email not sent.");
                }
            }
            MimeMessage myMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
            myMessage.setFrom(THE_GOOGLE_ID);
            for (String myTo : myRecipients) {
                myMessage.addRecipients(javax.mail.Message.RecipientType.TO, myTo);
            }
            Collections.sort(aScheduledHours);
            StringJoiner myHours = new StringJoiner("\n", "Hours scheduled this week:\n","");
            aScheduledHours.forEach(aHour -> myHours.add(DATE_FORMAT.format(aHour)));
            myMessage.setText(myHours.toString());
            myMessage.setSubject("Tutor.com Scheduled Hours");
            Message myExecute = theGmailService.users().messages().send("me", createMessageWithEmail(myMessage)).execute();
            System.out.println("Sent message:\n" + myExecute.toPrettyString());
        } catch (Exception aE) {
            System.out.println("ERROR while attempting to send notification emails");
            aE.printStackTrace();
        }
    }

    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
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

    private void createServices() {
        try {
            NetHttpTransport myTransport = GoogleNetHttpTransport.newTrustedTransport();
            Credential myCredential = getCredentials(myTransport);
            theCalendarService = new Calendar.Builder(myTransport, JSON_FACTORY, myCredential)
                    .setApplicationName(NAME)
                    .build();
            theGmailService = new Gmail.Builder(myTransport, JSON_FACTORY, myCredential)
                    .setApplicationName(NAME)
                    .build();
        } catch (Exception aE) {
            aE.printStackTrace();
        }
    }
}
