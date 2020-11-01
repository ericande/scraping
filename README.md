# Tutor calendar exporter tool
This project is set up to access tutor.com's schedule manager, pull your scheduled hours from there, and
optionally create google calendar events for all of your scheduled hours in the current week.

## Setup Instructions
TUTOR_URL at the top of WebScraper.java will need to be set to your own URL for Tutor.com's Schedule Manager.

If you wish to save your tutor.com credentials, rather than prompting for them each time, you can supply a file named
credentials.txt in the top level folder for this project, with the first line containing your username/email and the
second containing your password.

The EventPlacer code which saves your events to your Google Calendar uses OAuth2 for authentication. You may need to 
do additional setup to authorize the application to interact with your gmail and calendar accounts, including 
registering your copy of the application with Google's console and downloading a client_secrets.json file from there.  
As such, do not expect google calendar event placing to work "out of the box."  If you want to get it to work for you, 
the Calendar API Java Quickstart Guide should be extremely helpful, and I direct you there for questions.
https://developers.google.com/calendar/quickstart/java

You can optionally send a digest of your scheduled hours for the week to recipients listed in the top-level file 
email_recipients.txt

## Running the exporter
In addition to extracting your scheduled hours, this program can
    - Create google calendar events for those hours (Default: ON, program arg skipGoogleCalendar for OFF)
    - Save the Schedule Manager calendar page to file (Default: OFF, program arg serializeScheduleManager for ON)
    - Extract hours for next week (Sun-Sat) rather than the current (Default: OFF, program arg nextWeek for ON)

## Notices
This program is provided as-is, with no guarantees of meeting any particular specification or function. This is not
an official product of Tutor.com, nor of Google. You should only give your login credentials to applications you
trust, don't give your Tutor.com password out to sites or programs other than Tutor.com. This program has not been
verified by google as a trusted app to modify your Google Calendar.

## Acknowledgements

Google's Calendar API Java Quickstart Guide was extremely helpful to me in getting OAuth authentication working for
this project. See the guide here: https://developers.google.com/calendar/quickstart/java