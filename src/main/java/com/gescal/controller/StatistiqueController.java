package com.gescal.controller;

import com.gescal.repository.StatistiqueRepository;
import com.gescal.service.WorkingYearService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class StatistiqueController {
    private final StatistiqueRepository statistiques;
    private final WorkingYearService workingYear;
    public StatistiqueController(StatistiqueRepository statistiques, WorkingYearService workingYear) {
        this.statistiques = statistiques;
        this.workingYear = workingYear;
    }

    @GetMapping("/statistiques")
    public String statistiques(@RequestParam(required = false) Integer annee, Model model) {
        int selected = annee == null ? workingYear.get() : annee;
        model.addAttribute("annee", selected);
        model.addAttribute("stats", statistiques.dashboard(selected));
        model.addAttribute("parReglement", statistiques.parReglement(selected));
        model.addAttribute("parTournee", statistiques.parTournee(selected));
        model.addAttribute("parVille", statistiques.parVille(selected));
        return "statistiques";
    }
}
