package com.mycompany.jwtdemo.service;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.io.InputStream;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    /**
     * This method will send compose and send the message
     * */
    public void sendMail(String to, String subject, String body)
    {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setFrom("info@obify.in");
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }

    public void sendMailWithAttachment(String to, String subject, String body, String filename, InputStream inputStream)
    {
        MimeMessagePreparator preparator = mimeMessage -> {
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(new InternetAddress("info@obify.in"));
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body);
            helper.addAttachment(filename,  new ByteArrayResource(IOUtils.toByteArray(inputStream)), "application/vnd.ms-excel");
        };

        try {
            javaMailSender.send(preparator);
        }
        catch (MailException ex) {
            // simply log it and go on...
            System.err.println("In exception sending email");
            System.err.println(ex.getMessage());
            ex.printStackTrace();
        }
    }
}
