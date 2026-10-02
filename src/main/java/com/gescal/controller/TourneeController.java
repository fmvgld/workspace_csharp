package com.gescal.controller;

import com.gescal.entity.TourneeEntity;
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
@RequestMapping("/tournees")
public class TourneeController {
    private final TourneeRepository tournees;
    private final ArchiveRepository archives;
    private final WorkingYearService workingYear;
    public TourneeController(TourneeRepository tournees, ArchiveRepository archives, WorkingYearService workingYear) {
        this.tournees = tournees;
        this.archives = archives;
        this.workingYear = workingYear;
    }

    @GetMapping
    public String liste(@RequestParam(required = false) String annee, Model model) {
        String selectedYear = annee == null || annee.isBlank()
                ? String.valueOf(workingYear.get()) : annee.trim();
        model.addAttribute("annee", selectedYear);
        model.addAttribute("tournees", tournees.findAll(selectedYear));
        model.addAttribute("archived", selectedYear.matches("\\d{4}")
                && archives.isArchived(Integer.parseInt(selectedYear)));
        return "tournees";
    }
    @GetMapping("/nouvelle")
    public String nouveau(Model model) {
        int currentYear = workingYear.get();
        if (archives.isArchived(currentYear)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "L'année " + currentYear + " est archivée.");
        }
        model.addAttribute("tournee", new TourneeEntity(0, null, "", String.valueOf(currentYear)));
        return "tournee-form";
    }
    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable long id, Model model) {
        TourneeEntity tournee = tournees.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tournée introuvable"));
        if (tournee.annee() != null && archives.isArchived(Integer.parseInt(tournee.annee()))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette tournée appartient à une année archivée.");
        }
        model.addAttribute("tournee", tournee);
        return "tournee-form";
    }
    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id, @RequestParam Integer numero,
            @RequestParam String libelle, @RequestParam String annee, RedirectAttributes redirect) {
        try {
            tournees.save(id, numero, libelle.trim(), annee.trim());
            redirect.addFlashAttribute("success", id == null ? "Tournée créée." : "Tournée mise à jour.");
            return "redirect:/tournees?annee=" + annee;
        } catch (IllegalArgumentException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return id == null ? "redirect:/tournees/nouvelle" : "redirect:/tournees/" + id + "/modifier";
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/tournees?annee=" + annee;
        }
    }
    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable long id, RedirectAttributes redirect) {
        try {
            if (tournees.deleteById(id)) redirect.addFlashAttribute("success", "Tournée supprimée.");
            else redirect.addFlashAttribute("error", "Cette tournée est utilisée et ne peut pas être supprimée.");
        } catch (IllegalStateException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/tournees";
    }
}
