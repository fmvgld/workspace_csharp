package com.gescal.service;

import com.gescal.dto.ReceiptData;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Service
public class MailService {
    private final JavaMailSender sender;
    private final boolean enabled;
    private final String from;

    public MailService(JavaMailSender sender, @Value("${gescal.mail.enabled:false}") boolean enabled,
                       @Value("${spring.mail.username:}") String from) {
        this.sender = sender;
        this.enabled = enabled;
        this.from = from;
    }

    public boolean isEnabled() { 
    	return enabled && from != null && !from.isBlank(); 
    }

    public void sendReceipt(ReceiptData receipt, byte[] pdf) throws Exception {
        if (!isEnabled()) throw new IllegalStateException("L'envoi SMTP n'est pas configuré ou activé.");
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper mail = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
        mail.setFrom(from);
        //mail.setTo(receipt.mail());
        mail.setTo("fmvgld@gmail.com");
        
        mail.setSubject("Calendrier " + receipt.annee() + " - reçu de don");
        mail.setText("Bonjour " + receipt.nomComplet() + ",<br><br>Veuillez trouver ci-joint votre reçu de don." +
                "<br>Traitement le " + LocalDate.now() + ".<br><br>Bonne réception.<br>Amicale des Sapeurs-Pompiers de Jarrie", true);
        mail.addAttachment("recu_" + receipt.operationId() + ".pdf", new ByteArrayResource(pdf));
        sender.send(message);
    }
}

