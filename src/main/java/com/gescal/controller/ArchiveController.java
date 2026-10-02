package com.gescal.controller;

import com.gescal.entity.ArchiveAnneeEntity;
import com.gescal.repository.ArchiveRepository;
import com.gescal.service.BackupService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.file.Path;

@Controller
@RequestMapping("/outils/archive")
public class ArchiveController {
    private final ArchiveRepository archives;
    private final BackupService backups;

    public ArchiveController(ArchiveRepository archives, BackupService backups) {
        this.archives = archives;
        this.backups = backups;
    }

    @PostMapping
    public String archive(@RequestParam int annee, RedirectAttributes redirect) {
        try {
            Path backup = backups.backup();
            ArchiveAnneeEntity archive = archives.archive(annee);
            redirect.addFlashAttribute("success", "Année " + annee + " archivée : " + archive.nombreOperations() +
                    " opération(s), " + archive.nombreTournees() + " tournée(s). Sauvegarde : " + backup);
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Archivage impossible : " + ex.getMessage());
        }
        return "redirect:/outils?annee=" + annee;
    }
}

