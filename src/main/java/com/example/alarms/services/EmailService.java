package com.example.alarms.services;

import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class EmailService {

  @Autowired
  private JavaMailSender emailSender;
  @Autowired
  private TemplateEngine templateEngine;

  public void sendEmailWithHtmlTemplate(String to, String subject, String templateName, Context context) throws Exception {
    MimeMessage mimeMessage = emailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

    try {
        helper.setTo(to);
        helper.setSubject(subject);
        String htmlContent = templateEngine.process(templateName, context);
        helper.setText(htmlContent, true);
        emailSender.send(mimeMessage);
    } catch (Exception e) {
        throw new Exception("Mail not sent!");
    }

      System.out.println("This will send emails");
} 

}
