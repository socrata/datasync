package com.socrata.datasync;

import com.socrata.datasync.config.userpreferences.UserPreferences;
import com.socrata.datasync.config.userpreferences.UserPreferencesJava;
import com.sun.mail.smtp.SMTPTransport;

//import java.security.Security;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Sends emails using Google SMTP
 *
 * @author doraemon
 */
public class SMTPMailer {
	
    private SMTPMailer() {
    	
    }

    /**
     * Send email using GMail SMTP server.
     * NOTE: obtains SMTP settings from userPrefs
     *
     * @param recipientEmail TO recipient
     * @param title title of the message
     * @param message message to be sent
     * @throws AddressException if the email address parse failed
     * @throws MessagingException if the connection is dead or not in the connected state 
     * 							   or if the message is not a MimeMessage
     */
    public static void send(String recipientEmail, String title, String message)
    		throws AddressException, MessagingException {
        SMTPMailer.send(recipientEmail, "", title, message);
    }

    /**
     * Send email using an SMTP server.
     *
     * @param recipientEmail TO recipient
     * @param ccEmail CC recipient. Can be empty if there is no CC recipient
     * @param title title of the message
     * @param message message to be sent
     * @throws AddressException if the email address parse failed
     * @throws MessagingException if the connection is dead or not in the connected state
     * 							   or if the message is not a MimeMessage
     */
    public static void send(String recipientEmail, String ccEmail, String title, String message) 
    		throws AddressException, MessagingException {
    	UserPreferences userPrefs = new UserPreferencesJava();
    	
        //Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

        // Get a Properties object
        Properties props = System.getProperties();
        props.setProperty("mail.smtps.host", userPrefs.getOutgoingMailServer());
        props.setProperty("mail.smtp.socketFactory.fallback", "false");
        props.setProperty("mail.smtp.port", userPrefs.getSmtpPort());
        String sslPort = userPrefs.getSslPort();

        boolean useSSL = !(sslPort.equals(""));


        if(useSSL) {

        	props.setProperty("mail.smtp.socketFactory.class", SSL_FACTORY);
        	props.setProperty("mail.smtp.socketFactory.port", sslPort);
            props.setProperty("mail.smtps.auth", "true");
            /*
        If set to false, the QUIT command is sent and the connection is immediately closed. If set
        to true (the default), causes the transport to wait for the response to the QUIT command.

        ref :   http://java.sun.com/products/javamail/javadocs/com/sun/mail/smtp/package-summary.html
                http://forum.java.sun.com/thread.jspa?threadID=5205249
                smtpsend.java - demo program from javamail
        */
            props.put("mail.smtps.quitwait", "false");
        }

        Session session = Session.getInstance(props, null);

        // -- Create a new message --
        final MimeMessage msg = new MimeMessage(session);

        // -- Set the FROM and TO fields --
        msg.setFrom(new InternetAddress(userPrefs.getSmtpUsername()));
        msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail, false));

        if (ccEmail.length() > 0) {
            msg.setRecipients(Message.RecipientType.CC, InternetAddress.parse(ccEmail, false));
        }

        msg.setSubject(title);
        msg.setText(message, "utf-8");
        msg.setSentDate(new Date());


        SMTPTransport t = (SMTPTransport)session.getTransport("smtp");
        if (useSSL)
            t = (SMTPTransport)session.getTransport("smtps");
        
        t.connect(userPrefs.getOutgoingMailServer(), userPrefs.getSmtpUsername(), userPrefs.getSmtpPassword());
        t.sendMessage(msg, msg.getAllRecipients());      
        t.close();
    }
}
