package com.gescal.service;

import com.gescal.dto.ReceiptData;
import com.gescal.repository.OperationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReceiptService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiptService.class);
    private final OperationRepository operations;
    private final JasperReportService reports;
    private final MailService mail;
    private final Path receiptDirectory;

    public ReceiptService(OperationRepository operations, JasperReportService reports, MailService mail,
                          @Value("${gescal.receipt.directory}") String receiptDirectory) {
        this.operations = operations;
        this.reports = reports;
        this.mail = mail;
        this.receiptDirectory = Path.of(receiptDirectory);
    }

    public List<ReceiptData> pending(int annee) { return operations.findPendingReceipts(annee); }
    public boolean mailEnabled() { return mail.isEnabled(); }

    public byte[] pdf(long operationId) throws Exception {
        return reports.receipt(required(operationId));
    }

    public SendResult send(List<Long> operationIds) {
        int sent = 0;
        List<String> errors = new ArrayList<>();
        for (Long operationId : operationIds) {
            try {
                ReceiptData receipt = required(operationId);
                if (!receipt.hasMail()) throw new IllegalArgumentException("adresse courriel manquante");
                byte[] pdf = reports.receipt(receipt);
                store(receipt, pdf);
                mail.sendReceipt(receipt, pdf);
                operations.markReceiptSent(operationId);
                LOGGER.info("Reçu envoyé - opération={}, année={}, client={}",
                        operationId, receipt.annee(), receipt.nomComplet());
                sent++;
            } catch (Exception ex) {
                String message = rootMessage(ex);
                LOGGER.error("Erreur lors de l'envoi du reçu - opération={} - {}", operationId, message, ex);
                errors.add("Opération " + operationId + " : " + message);
            }
        }
        return new SendResult(sent, errors);
    }

    private ReceiptData required(long operationId) {
        return operations.findReceipt(operationId).orElseThrow(() -> new IllegalArgumentException("Opération introuvable"));
    }

    private void store(ReceiptData receipt, byte[] pdf) throws Exception {
        Files.createDirectories(receiptDirectory);
        String name = (receipt.nom() + "_" + receipt.prenom() + "_" + receipt.operationId()).replaceAll("[^a-zA-Z0-9._-]", "_");
        Files.write(receiptDirectory.resolve(name + ".pdf"), pdf);
    }

    private String rootMessage(Throwable error) {
        Throwable current = error;
        while (current.getCause() != null) current = current.getCause();
        return current.getMessage() == null ? current.getClass().getSimpleName() : current.getMessage();
    }

    public record SendResult(int sent, List<String> errors) { }
}
