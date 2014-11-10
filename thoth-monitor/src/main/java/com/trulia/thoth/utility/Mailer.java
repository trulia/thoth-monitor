package com.trulia.thoth.utility;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * User: dbraga - Date: 10/15/13
 */

@Component
public class Mailer {
  @Value("${thoth.mailer.sender}")
  private String sender;
  @Value("${thoth.mailer.receiver}")
  private String receiver;
  @Value("${thoth.mailer.host}")
  private String host;

  public Mailer(){} // Empty constructor

  private MimeMessage prepareMessage(String subject, String content){
    MimeMessage message = null;
    try {
      Properties props = System.getProperties();
      props.setProperty("mail.smtp.host", host);
      Session session = Session.getInstance(props);
      message = new MimeMessage(session);
      message.setFrom(new InternetAddress(sender));
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(receiver));
      message.setSubject(subject);
      message.setContent(
          content,
          "text/html" );

      // Message sent as important
      message.addHeader("X-Priority", "1");

    } catch (AddressException e) {
      e.printStackTrace();
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    finally {
      return message;
    }
  }

  public boolean sendMail(String subject, String content) {
    boolean sent = false;
    try {
      Transport.send(prepareMessage(subject, content));
      sent = true;
    } catch (MessagingException e) {
      e.printStackTrace();
    }
    finally {
      return sent;
    }
  }

}
