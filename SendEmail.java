/**
 *
 * Expected email file format:
 *
 * Server: email server
 * User: email account (also used as the From: in the email)
 * Password: email account password
 * To: primary recipient
 * CC: comma separated list of secondary recipients
 * BCC: comma separated list of tertiary recipients
 * Subject: Email subject
 * Body: multiple lies of text representing the body of the email
 *
 */

import java.io.*;
import java.util.*;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * A Simple Emailer, which will send an email with the contents of a specified file and an optional attachment if specified
 * @author Tyler Matthews
 */
public class SendEmail {
   /**
    * Holds the parsed contents of an email to send
    */
   private EmailHolder email;
   /**
    * Properties used by the Session instance, to send an email
    */
   private Properties  props;

   /**
    * Constructs the emailer, parsing the specified email file, assigning attachment and preparing the properties file property
    * initialization taken from https://www.javatpoint.com/example-of-sending-email-using-java-mail-api-through-gmail-server
    *
    * @param emailFile The file containing the email fields to be parsed
    * @param attachmentFile The specified file to attach
    * @throws IOException on file error
    */
   public SendEmail(String emailFile, String attachmentFile) throws IOException {
      this.email = loadEmail(emailFile); // parse the email
      this.email.setAttachmentFile(attachmentFile);
      props = new Properties(); // initialize properties
      props.put("mail.smtp.host", email.server); // server specified in text file
      props.put("mail.smtp.socketFactory.port", "465");
      props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
      props.put("mail.smtp.auth", "true");
      props.put("mail.smtp.port", "465");
   }

   /**
    * Loads a email file into memory from the specified file
    *
    * @param fileName The file containing the email
    * @return A parsed email holder
    * @throws IOException On file io exception
    */
   public void sendEmail() throws MessagingException {
      Transport.send(createMessage(Session.getDefaultInstance(props, new Authenticator() {
         protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
            return new javax.mail.PasswordAuthentication(email.user, email.password);
         };
      })));
      System.out.println("Email Away!");
   }

   /**
    * Creates a mime message from the parsed email file, and the specified attachment initialization code from
    * https://www.javatpoint.com/example-of-sending-email-using-java-mail-api-through-gmail-server
    *
    * @param session The session for the message
    * @return The mime message
    * @throws MessagingException on message exception
    */
   private MimeMessage createMessage(Session session) throws MessagingException {
      MimeMessage message = new MimeMessage(session);
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.to));
      for (String cc : email.cc) { // for every cc, add
         message.addRecipient(Message.RecipientType.CC, new InternetAddress(cc));
      }
      for (String bcc : email.bcc) { // for every bcc, add
         message.addRecipient(Message.RecipientType.BCC, new InternetAddress(bcc));
      }
      if (validField(email.subject)) { // make sure valid .. allowing null
         message.setSubject(email.subject);
      }
      // creat a multi part, which will be composed of bodyparts - body text and attachment
      Multipart multipart = new MimeMultipart();
      BodyPart bodyPart = new MimeBodyPart();
      bodyPart.setText(email.body); // set body text
      multipart.addBodyPart(bodyPart);

      if (validField(email.attachmentFile)) {
         bodyPart = new MimeBodyPart(); // set attachment
         bodyPart.setDataHandler(new DataHandler(new FileDataSource(email.attachmentFile)));
         bodyPart.setFileName(email.attachmentFile);
         multipart.addBodyPart(bodyPart);
      }
      message.setContent(multipart); // set content to multi part
      return message;
   }

   /**
    * Simple null or empty check
    *
    * @param field The field to cehck
    * @return if valid
    */
   private boolean validField(String field) {
      if (field != null && !field.isEmpty()) {
         return true;
      }
      return false;
   }

   /**
    * Loads a email file into memory from the specified file
    *
    * @param fileName The file containing the email
    * @return A parsed email holder
    * @throws IOException On file io exception
    */
   private EmailHolder loadEmail(String fileName) throws IOException {
      StringBuilder builder = new StringBuilder();
      EmailHolder holder = new EmailHolder();
      // opens file input stream into a buffered reader
      try (BufferedReader in = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(fileName)), "UTF-8"))) {
         // for every line, read in and parse out type
         for (String entry = in.readLine(); entry != null; entry = in.readLine()) {
            if (!holder.isBody(entry)) { // if not body, info can only be a single line, pass to be parsed
               holder.init(entry);
            } else {
               // if body has been reached, we need to capture all line, append and readd the newline that's removed from the reader
               builder.append(entry + "\n");
            }
         }
      }
      holder.init(builder.toString()); // pass body
      return holder;
   }

   /**
    * Simple container for the logical email data to send
    *
    * @author Tyler Matthews
    */
   public static class EmailHolder {
      /**
       * Server to user, for emailing
       */
      private String       server;
      /**
       * The user at server
       */
      private String       user;
      /**
       * The password for server
       */
      private String       password;
      /**
       * Addressed to
       */
      private String       to;
      /**
       * List of cc's
       */
      private List<String> cc          = new ArrayList<>();
      /**
       * LIst of bcc's
       */
      private List<String> bcc         = new ArrayList<>();
      /**
       * The email subject
       */
      private String       subject;
      /**
       * The email body
       */
      private String       body;
      /**
       * Flag to indicate body has been reached during parsing
       */
      private boolean      enteredBody = false;
      /**
       * The file to attach
       */
      private String       attachmentFile;

      public EmailHolder() {}

      private void init(String line) {
         String[] contents = line.split(":"); // split from title and content
         // assign proper field
         switch (contents[0].toLowerCase().trim()) {
            case "server":
               this.server = contents[1].trim();
               break;
            case "user":
               this.user = contents[1].trim();
               break;
            case "password":
               this.password = contents[1].trim();
               break;
            case "to":
               this.to = contents[1].trim();
               break;
            case "subject":
               this.subject = contents[1].trim();
               break;
            case "body":
               // account for possible colons within the body
               for (int x = 1; x < contents.length; x++) {
                  this.body += contents[x];
               }
               break;
            // cc and bcc can have multiple items
            case "cc":
               addItems(this.cc, contents[1].split(","));
               break;
            case "bcc":
               addItems(this.bcc, contents[1].split(","));
               break;
         }
      }

      /**
       * Adds items to a list
       *
       * @param list The list to add items too
       * @param items The items
       */
      private void addItems(List<String> list, String[] items) {
         for (String item : items) {
            list.add(item.trim());
         }
      }

      /**
       * Check if line is the start of the body or apart of the body
       *
       * @param line The body
       * @return is body
       */
      public boolean isBody(String line) {
         if (enteredBody) {
            return true;
         } else {
            if (line.toLowerCase().startsWith("body")) {
               enteredBody = true;
            }
         }
         return enteredBody;
      }

      /**
       * Sets the attachment file location
       *
       * @param attachmentFile The attachment file
       */
      public void setAttachmentFile(String attachmentFile) {
         this.attachmentFile = attachmentFile;
      }
   }

   /**
    * SendEmail expects a file containing the email, well formatted, and can also send an attachment
    * @param args The arguments, to contain the email file an optional attachment file
    */
   public static void main(String[] args) {
      try {
         if (args.length > 2 || args.length < 1) {
            throw new ArrayIndexOutOfBoundsException();
         }
         SendEmail emailer = new SendEmail(args[0], args.length == 2 ? args[1] : null);
         emailer.sendEmail();
      } catch (ArrayIndexOutOfBoundsException ex) {
         System.out.println("Usage: java SendEmail email_file attachment_file");
      } catch (Exception ex) {
         System.out.println(ex.getMessage());
         ex.printStackTrace();
      }
   }
}
