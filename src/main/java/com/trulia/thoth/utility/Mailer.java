package com.trulia.thoth.utility;
import org.apache.log4j.Logger;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * User: dbraga - Date: 10/15/13
 */
//TODO: finish mailer
public class Mailer {

  private static final Logger LOG = Logger.getLogger(Mailer.class);

  private static final String sender = "dbraga@trulia.com";
  private static final String receiver = "dbraga@trulia.com";
  private static final String host = "thoth";

  private String subject;
  private String content;
  private int priority;

  public Mailer(String subject, String content, int priority){
    this.subject = subject;
    this.content = content;
    this.priority = priority;
  }

  public boolean sendMail() {
    Properties properties = System.getProperties();
    properties.setProperty("mail.smtp.host", host);
    Session session = Session.getDefaultInstance(properties);
    try {
      MimeMessage message = new MimeMessage(session);
      message.setFrom(new InternetAddress(sender));
      message.addRecipient(Message.RecipientType.TO,
              new InternetAddress(receiver));

      message.setSubject(subject);
      message.setContent(
              content,
              "text/html" );

      message.addHeader("X-Priority", String.valueOf(priority));
      Transport.send(message);

      return true;
    } catch (MessagingException mex) {
      mex.printStackTrace();
      return false;
    }

  }

}
