package com.gescal.controller;

import com.gescal.repository.OperationRepository;
import com.gescal.repository.StatistiqueRepository;
import com.gescal.service.WorkingYearService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@Controller
public class HomeController {
    private final StatistiqueRepository statistiques;
    private final OperationRepository operations;
    private final WorkingYearService workingYear;

    public HomeController(StatistiqueRepository statistiques, OperationRepository operations, WorkingYearService workingYear) {
        this.statistiques = statistiques;
        this.operations = operations;
        this.workingYear = workingYear;
    }

    @GetMapping("/")
    public String accueil(@RequestParam(required = false) Integer annee, Model model) {
        int selected = annee == null ? workingYear.get() : annee;
        model.addAttribute("annee", selected);
        model.addAttribute("stats", statistiques.dashboard(selected));
        model.addAttribute("dernieresOperations", operations.findAll(selected, null).stream().limit(8).toList());
        return "index";
    }
}
