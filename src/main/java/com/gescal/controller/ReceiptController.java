package com.gescal.controller;

import com.gescal.service.ReceiptService;
import com.gescal.service.WorkingYearService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/recus")
public class ReceiptController {
    private final ReceiptService receipts;
    private final WorkingYearService workingYear;
    public ReceiptController(ReceiptService receipts, WorkingYearService workingYear) {
        this.receipts = receipts;
        this.workingYear = workingYear;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) Integer annee, Model model) {
        int selected = annee == null ? workingYear.get() : annee;
        model.addAttribute("annee", selected);
        model.addAttribute("recus", receipts.pending(selected));
        model.addAttribute("mailEnabled", receipts.mailEnabled());
        return "receipts";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable long id) throws Exception {
        byte[] content = receipts.pdf(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline().filename("recu_" + id + ".pdf", StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).body(content);
    }

    @PostMapping("/envoyer")
    public String envoyer(@RequestParam(required = false) List<Long> ids,
                          @RequestParam int annee, RedirectAttributes redirect) {
        if (ids == null || ids.isEmpty()) {
            redirect.addFlashAttribute("error", "Sélectionnez au moins un reçu.");
            return "redirect:/recus?annee=" + annee;
        }
        ReceiptService.SendResult result = receipts.send(ids);
        if (result.sent() > 0) redirect.addFlashAttribute("success", result.sent() + " reçu(s) envoyé(s).");
        if (!result.errors().isEmpty()) {
            String errorMessage = String.join(" | ", result.errors());
            redirect.addFlashAttribute("error", errorMessage);
            redirect.addFlashAttribute("receiptSendError", errorMessage);
        }
        return "redirect:/recus?annee=" + annee;
    }
}
