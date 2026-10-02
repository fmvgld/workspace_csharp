package com.gescal.controller;

import com.gescal.entity.OperationEntity;
import com.gescal.repository.ClientRepository;
import com.gescal.repository.OperationRepository;
import com.gescal.repository.ReglementRepository;
import com.gescal.repository.TourneeRepository;
import com.gescal.repository.ArchiveRepository;
import com.gescal.service.WorkingYearService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/operations")
public class OperationController {
    private final OperationRepository operations;
    private final ClientRepository clients;
    private final TourneeRepository tournees;
    private final ReglementRepository reglements;
    private final ArchiveRepository archives;
    private final WorkingYearService workingYear;

    public OperationController(OperationRepository operations, ClientRepository clients,
                               TourneeRepository tournees, ReglementRepository reglements,
                               ArchiveRepository archives, WorkingYearService workingYear) {
        this.operations = operations;
        this.clients = clients;
        this.tournees = tournees;
        this.reglements = reglements;
        this.archives = archives;
        this.workingYear = workingYear;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) Integer annee,
            @RequestParam(required = false, defaultValue = "") String q, Model model) {
        int selected = annee == null ? workingYear.get() : annee;
        model.addAttribute("annee", selected);
        model.addAttribute("q", q);
        model.addAttribute("operations", operations.findAll(selected, q));
        model.addAttribute("archived", archives.isArchived(selected));
        return "operations";
    }

    @GetMapping("/nouvelle")
    public String nouveau(Model model, RedirectAttributes redirect) {
        int currentYear = workingYear.get();
        if (archives.isArchived(currentYear)) {
            redirect.addFlashAttribute("error", "L'année " + currentYear + " est archivée : aucune opération ne peut être ajoutée.");
            return "redirect:/operations?annee=" + currentYear;
        }
        model.addAttribute("operation", new OperationEntity(0, LocalDate.now().toString(), 0, "", "", 0, null, "", 0, 0, "", false, false, false, false, currentYear, ""));
        lists(model, currentYear);
        return "operation-form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable long id, Model model, RedirectAttributes redirect) {
        OperationEntity operation = operations.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Opération introuvable"));
        if (operation.annee() != null && archives.isArchived(operation.annee())) {
            redirect.addFlashAttribute("error", "L'année " + operation.annee() + " est archivée : cette opération est en lecture seule.");
            return "redirect:/operations?annee=" + operation.annee();
        }
        model.addAttribute("operation", operation);
        lists(model, operation.annee() == null ? workingYear.get() : operation.annee());
        return "operation-form";
    }

    private void lists(Model model, int annee) {
        model.addAttribute("clients", clients.findAll(null));
        model.addAttribute("tournees", tournees.findAll(String.valueOf(annee)));
        model.addAttribute("reglements", reglements.findAll());
        model.addAttribute("annee", annee);
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id, @RequestParam String dateSaisie,
            @RequestParam long clientId, @RequestParam long tourneeId, @RequestParam double montant,
            @RequestParam long reglementId, @RequestParam(defaultValue = "false") boolean recu,
            @RequestParam(defaultValue = "false") boolean enveloppe, @RequestParam(defaultValue = "false") boolean envoiRecu,
            @RequestParam(defaultValue = "false") boolean recuEnvoye, @RequestParam Integer annee,
            @RequestParam(defaultValue = "") String dateEnvoi, RedirectAttributes redirect) {
        try {
            if (envoiRecu) {
                var client = clients.findById(clientId)
                        .orElseThrow(() -> new IllegalArgumentException("Le client sélectionné est introuvable."));
                if (client.mail() == null || client.mail().isBlank()) {
                    throw new IllegalArgumentException("L'envoi du reçu nécessite une adresse courriel renseignée pour le client.");
                }
            }
            operations.save(id, dateSaisie, clientId, tourneeId, montant, reglementId, recu, enveloppe, envoiRecu, recuEnvoye, annee, dateEnvoi);
            redirect.addFlashAttribute("success", id == null ? "Opération créée." : "Opération mise à jour.");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/operations?annee=" + annee;
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable long id, RedirectAttributes redirect) {
        try {
            operations.deleteById(id);
            redirect.addFlashAttribute("success", "Opération supprimée.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/operations";
    }
}
