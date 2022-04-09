package com.mycompany.jwtdemo.service;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import javax.mail.Message;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;
import java.io.*;

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
        MimeMessagePreparator preparator = new MimeMessagePreparator()
        {
            public void prepare(MimeMessage mimeMessage) throws Exception
            {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
                mimeMessage.setFrom(new InternetAddress("info@obify.in"));
                mimeMessage.setSubject(subject);
                mimeMessage.setText(body);

                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
                helper.addAttachment(filename,  new ByteArrayResource(IOUtils.toByteArray(inputStream)), "application/vnd.ms-excel");
            }
        };

        try {
            javaMailSender.send(preparator);
        }
        catch (MailException ex) {
            // simply log it and go on...
            System.err.println(ex.getMessage());
        }
    }
}
