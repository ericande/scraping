package com.ericande;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.*;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

class WebScraper {
    static final String TUTOR_URL = "https://prv.tutor.com/nGEN/Tools/ScheduleManager_v2/setContactID.aspx?ProgramGUID=B611858B-4D02-4AFE-8053-D082BBC1C58E&UserGUID=1085d262-f0e2-47bb-9a43-09a987969d05";
    private static final List<String> theIgnoreWarningPatterns = Arrays.asList(".*Obsolete content type encountered.*");
    private static final Scanner SCANNER = new Scanner(System.in);
    private static final String CREDENTIALS_FILE = "credentials.txt";
    private static final String TUTOR_SCHEDULE_PAGE = "TutorSchedulePage";
    private final WebClient theClient;
    private final PageSerializer theSerializer;
    private final boolean theSerializeOutput;
    private final boolean theProcessNextWeek;
    private String theUserName;
    private String thePassword;

     WebScraper(boolean aSerializeOutput, boolean aProcessNextWeek) {
         theSerializeOutput = aSerializeOutput;
         theProcessNextWeek = aProcessNextWeek;
         theClient = new WebClient(BrowserVersion.CHROME);
        theClient.getOptions().setCssEnabled(true);
        theClient.getOptions().setJavaScriptEnabled(true); //Tutor.com schedule manager makes heavy use of js
        if (!aSerializeOutput) { //Cannot serialize lambda, so no suppressing warnings when serializing
             IncorrectnessListener myIncorrectnessListener = theClient.getIncorrectnessListener();
             theClient.setIncorrectnessListener(listenerFilteringPattern(myIncorrectnessListener));
        }
        theSerializer = new PageSerializer().setFname(TUTOR_SCHEDULE_PAGE);
        setCredentials();
    }

    private void setCredentials() {
        File myCredentialsFile = new File(CREDENTIALS_FILE);
        if (!myCredentialsFile.exists()) {
            System.out.print("Enter username:");
            theUserName = SCANNER.nextLine();
            if (theUserName.length() == 0) {
                return;
            }
            System.out.print("Enter password: ");
            thePassword = SCANNER.nextLine();
        } else {
            try {
                FileInputStream myFIS = new FileInputStream(myCredentialsFile);
                Scanner myScanner = new Scanner(myFIS);
                theUserName = myScanner.nextLine();
                thePassword = myScanner.nextLine();
            } catch (FileNotFoundException aE) {
                aE.printStackTrace();
            }
            if (theUserName.length() == 0 || thePassword.length() == 0) {
                System.out.println("ERROR: Credentials file found, but username or password missing!");
            }
        }
    }

    HtmlPage processPage(String aUrl) {
        URL myURL;
        try {
            myURL = new URL(aUrl);
            HtmlPage myPage = getPage(myURL);
            HtmlForm myForm = myPage.getForms().get(0);
            HtmlInput myTxtUserName = myForm.getInputByName("txtUserName");
            HtmlInput myTxtPassword = myForm.getInputByName("txtPassword");
            HtmlInput myButSignIn = myForm.getInputByName("butSignIn");
            System.out.println("INFO: Found login page, submitting request");

            myTxtUserName.setValueAttribute(theUserName);
            myTxtPassword.setValueAttribute(thePassword);
            HtmlPage mySchedulePage = myButSignIn.click();
            if (theProcessNextWeek) {
                DomElement myWeekAhead = mySchedulePage.getElementByName("weekAhead");
                if (myWeekAhead instanceof HtmlImage) {
                    HtmlImage myNextWeekBtn = (HtmlImage) myWeekAhead;
                    Page myNextWeek = myNextWeekBtn.click();
                    if (myNextWeek.isHtmlPage()) {
                        mySchedulePage = (HtmlPage) myNextWeek;
                    } else {
                        System.out.println("ERROR: Failed to load next week's information");
                    }
                }
            }
            if (theSerializeOutput) {
                theSerializer.savePage(mySchedulePage);
                theSerializer.xmlSave(mySchedulePage);
                theSerializer.stringSave(mySchedulePage);
            }
            return mySchedulePage;
        } catch (MalformedURLException aE) {
            System.out.println("URL invalid");
            aE.printStackTrace();
        } catch (NullPointerException aE) {
            System.out.println("Failed to load page");
            aE.printStackTrace();
        } catch (ElementNotFoundException aE) {
            System.out.println("Did not find expected login form");
            aE.printStackTrace();
        } catch (IOException aE) {
            System.out.println("Error when trying to sign in");
            aE.printStackTrace();
        }
        return null;
    }

    @Nullable
    private HtmlPage getPage(URL aURL) {
        return getPage(new WebRequest(aURL)); //GET by default
    }

    @Nullable
    private HtmlPage getPage(WebRequest aURL) {
        try {
            return theClient.getPage(aURL);
        } catch (IOException aE) {
            System.out.println("ERROR: could not load page for URL: " + aURL);
            aE.printStackTrace();
        }
        return null;
    }

    private IncorrectnessListener listenerFilteringPattern(IncorrectnessListener aDelegateListener) {
        return (aS, aO) -> {
            for (String myPattern : theIgnoreWarningPatterns) {
                if (aS.matches(myPattern)) {
                    return;
                }
            }
            aDelegateListener.notify(aS, aO);
        };
    }
}
