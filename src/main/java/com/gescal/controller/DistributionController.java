package com.gescal.controller;

import com.gescal.entity.DistributionEntity;
import com.gescal.repository.DistributionRepository;
import com.gescal.repository.TourneeRepository;
import com.gescal.service.WorkingYearService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/distributions")
public class DistributionController {
    private final DistributionRepository distributions;
    private final TourneeRepository tournees;
    private final WorkingYearService workingYear;
    public DistributionController(DistributionRepository distributions, TourneeRepository tournees, WorkingYearService workingYear) {
        this.distributions = distributions;
        this.tournees = tournees;
        this.workingYear = workingYear;
    }
    @GetMapping
    public String liste(@RequestParam(required = false) Integer annee, Model model) {
        int selected = annee == null ? workingYear.get() : annee;
        model.addAttribute("annee", selected);
        model.addAttribute("distributions", distributions.findAll(selected));
        return "distributions";
    }
    @GetMapping("/nouvelle")
    public String nouveau(Model model) {
        int year = workingYear.get();
        model.addAttribute("distribution", new DistributionEntity(0, 0, year, 0, null, ""));
        model.addAttribute("tournees", tournees.findAll(String.valueOf(year)));
        return "distribution-form";
    }
    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable long id, Model model) {
        DistributionEntity distribution = distributions.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Distribution introuvable"));
        model.addAttribute("distribution", distribution);
        model.addAttribute("tournees", tournees.findAll(String.valueOf(distribution.annee())));
        return "distribution-form";
    }
    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id, @RequestParam long tourneeId,
            @RequestParam Integer annee, @RequestParam Integer nombre, RedirectAttributes redirect) {
        distributions.save(id, tourneeId, annee, nombre);
        redirect.addFlashAttribute("success", id == null ? "Distribution créée." : "Distribution mise à jour.");
        return "redirect:/distributions?annee=" + annee;
    }
    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable long id, RedirectAttributes redirect) {
        distributions.deleteById(id);
        redirect.addFlashAttribute("success", "Distribution supprimée.");
        return "redirect:/distributions";
    }
}
