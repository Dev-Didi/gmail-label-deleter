
package gmailcleaner;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.Month;
import java.time.temporal.ChronoUnit;
import java.util.*;

import gmailcleaner.CleanerQuery;
import gmailcleaner.Authenticator;
/* class to demonstrate use of Gmail list labels API */

public class GmailCleaner{
  /**
   * Application name.
   */
  static long DAY =  86400;
  static long MONTH = 2629743;
  private static final String APPLICATION_NAME = "Gmail API Label Cleaner";
  /**
   * Global instance of the JSON factory.
   */
  private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
  /**
   * Directory to store authorization tokens for this application.
   */
 public static void main(String... args) throws IOException, GeneralSecurityException {
    System.out.println("Starting...");
    // Build a new authorized API client service.
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    Credential userCred = Authenticator.getCredentials(HTTP_TRANSPORT);
    Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, userCred)
        .setApplicationName(APPLICATION_NAME)
        .build();

    // Print the labels in the user's account.
    String user = "me";
    ListLabelsResponse listResponse = service.users().labels().list(user).execute();

    // get the labels
    List<Label> labels = listResponse.getLabels();
    // List<String> wantedLabelNames = new ArrayList<>(Arrays.asList("CATEGORY_SOCIAL","CATEGORY_PROMOTIONS"));
    List<String> wantedLabelNames = new ArrayList<>(Arrays.asList("UBER_TEST"));
    List<String> wantedLabelIds = new ArrayList<>();
    if (labels.isEmpty()) {
      System.out.println("No labels found.");
      //return;
    }
    else {
      System.out.println("Number of labels: " + labels.size());
      for (Label l : labels) {
        System.out.println(l.getName() + "     " + l.getId());
        if(wantedLabelNames.contains(l.getName())) {
          wantedLabelIds.add(l.getId());
        }
      }
      System.out.println("Wanted ID's: " + wantedLabelIds.toString());
    }

    // get all the messages and filter out messages without interested labels
    List<Message> filterMessages = new ArrayList<>();
    ListMessagesResponse messages;
    Date timeSplit = new Date((Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli()));
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
    String strDate = dateFormat.format(timeSplit);
    System.out.println(strDate);

    for( String id : wantedLabelIds) {
      String pageToken = "";
      while(true){
        if (!pageToken.isEmpty())
          messages = service.users().messages().list(user).setPageToken(pageToken).setLabelIds(Arrays.asList(id,"UNREAD")).setQ("before:"+strDate).execute();
        else
          messages = service.users().messages().list(user).setLabelIds(Arrays.asList(id,"UNREAD")).setQ("before:"+strDate).execute();
        filterMessages.addAll(messages.getMessages());
        if (messages.getNextPageToken() == null) break;
        pageToken = messages.getNextPageToken();
      }
    }
    System.out.println("Total messages: " + filterMessages.size());
    BatchDeleteMessagesRequest deleteBatch = new BatchDeleteMessagesRequest();
    List<String> filterIds = new ArrayList<>();
    for (Message m : filterMessages) {
      filterIds.add(m.getId());
    }
    deleteBatch.setIds(filterIds);
    Void res = service.users().messages().batchDelete(user,deleteBatch).execute();
    System.out.println("Done. Removed " + filterIds.size() + " emails");
  }
}