
package gmailcleaner;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.UnexpectedException;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import javax.swing.*;
/* class to demonstrate use of Gmail list labels API */

public class GmailCleaner{
  /**
   * Application name.
   */
  static long DAY =  86400;
  static long MONTH = 2629743;
  private static final String APPLICATION_NAME = "Gmail API Label Cleaner";
  /**
   * Directory to store authorization tokens for this application.
   */

  // Auth object holds both the credentials and the gmail service. can still expose these as regular fields too
          // in the login method
  private static Authenticator auth;

    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static String user;
  private static Credential cred;
  private static Gmail service;
    private JButton loginButton;
    private JTextArea introMessage;

    public GmailCleaner() {
        loginButton.addActionListener(new ActionListener()  {
            @Override
            public void actionPerformed(ActionEvent e)  {
                try {
                    login();
                    showMainMenu();
                }
                catch(IOException | GeneralSecurityException ex) {
                    return;
                }
            }
        }) ;
    }

    public void showMainMenu() {
        loginButton.removeAll();

    }
    /**
     * This method is responsible for populating the authenticator and makes the user authenticate
     * with their google account.
     * @throws IOException
     * @throws GeneralSecurityException
     */
  private static void login() throws IOException, GeneralSecurityException{
      auth = new Authenticator();
      final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
      Credential userCred = auth.getCredentials(HTTP_TRANSPORT);
      if (userCred != null) {
          user = "me";
          service = auth.getService(HTTP_TRANSPORT,APPLICATION_NAME);
      }
  }

  private static List<Label> labels;

    /**
     * gets the list of all labels tied to the user account
     * @return List\<Label\>
     * @throws NullPointerException
     * @throws IOException
     */
  private static List<Label> getLabels() throws NullPointerException, IOException {
    if (service == null) throw new NullPointerException("Can't get labels: gmail service not established");
    if(labels != null) return labels;
    else {
        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
        labels = listResponse.getLabels();
        return labels;
    }
  }

    private static List<String> selectedLabels;
    public static List<String> getSelectedLabels() {
        return selectedLabels;
    }

    public static void setSelectedLabels(List<String> selectedLabels) {
        GmailCleaner.selectedLabels = selectedLabels;
    }

    private void selectLabel(String labelId) throws UnexpectedException {
        for (Label lab : labels) {
            if (lab.getId().equals(labelId) && !selectedLabels.contains(lab.getId())) {
                selectedLabels.add(lab.getId());
                return;
            }
        }
        if (!selectedLabels.contains(labelId)) throw new UnexpectedException("unknown label tried to be added");
        else return;
    }

 public static void main(String... args) throws IOException, GeneralSecurityException {
    System.out.println("Starting...");
    login();
    List<Label> labels = getLabels();
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