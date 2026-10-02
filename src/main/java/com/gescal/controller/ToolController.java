package com.gescal.controller;

import com.gescal.service.BackupService;
import com.gescal.service.ExcelExportService;
import com.gescal.service.JasperReportService;
import com.gescal.service.ReceiptService;
import com.gescal.service.WorkingYearService;
import com.gescal.service.DatabaseConfigurationService;
import com.gescal.repository.ArchiveRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@Controller
@RequestMapping("/outils")
public class ToolController {
    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final BackupService backups;
    private final ExcelExportService excel;
    private final JasperReportService reports;
    private final ReceiptService receipts;
    private final ArchiveRepository archives;
    private final WorkingYearService workingYear;
    private final DatabaseConfigurationService databaseConfiguration;

    public ToolController(BackupService backups, ExcelExportService excel, JasperReportService reports,
                          ReceiptService receipts, ArchiveRepository archives, WorkingYearService workingYear,
                          DatabaseConfigurationService databaseConfiguration) {
        this.backups = backups;
        this.excel = excel;
        this.reports = reports;
        this.receipts = receipts;
        this.archives = archives;
        this.workingYear = workingYear;
        this.databaseConfiguration = databaseConfiguration;
    }

    @GetMapping
    public String outils(Model model) {
        int year = workingYear.get();
        model.addAttribute("annee", year);
        model.addAttribute("mailEnabled", receipts.mailEnabled());
        model.addAttribute("archives", archives.findAll());
        model.addAttribute("selectedYearArchived", archives.isArchived(year));
        model.addAttribute("databaseActivePath", databaseConfiguration.activePath());
        model.addAttribute("databaseConfiguredPath", databaseConfiguration.configuredPath());
        model.addAttribute("databaseRestartRequired", databaseConfiguration.restartRequired());
        return "tools";
    }

    @PostMapping("/base-donnees/choisir")
    public String choisirBaseDeDonnees(RedirectAttributes redirect) {
        try {
            var selected = databaseConfiguration.chooseAndPersist();
            if (selected.isPresent()) {
                redirect.addFlashAttribute("success", "Base sélectionnée : " + selected.get() + ". Redémarrez GesCal pour l'utiliser.");
            } else {
                redirect.addFlashAttribute("error", "Sélection de la base annulée.");
            }
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Impossible de sélectionner cette base : " + ex.getMessage());
        }
        return "redirect:/outils";
    }

    @PostMapping("/annee-travail")
    public String anneeTravail(@RequestParam int annee, RedirectAttributes redirect) {
        try {
            workingYear.setAndPersist(annee);
            redirect.addFlashAttribute("success", "L'année de travail " + annee + " est maintenant utilisée dans toute l'application.");
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Impossible d'enregistrer l'année de travail : " + ex.getMessage());
        }
        return "redirect:/outils";
    }

    @PostMapping("/sauvegarde")
    public String sauvegarde(RedirectAttributes redirect) {
        try {
            Path file = backups.backup();
            redirect.addFlashAttribute("success", "Sauvegarde créée : " + file);
        } catch (Exception ex) {
            redirect.addFlashAttribute("error", "Échec de la sauvegarde : " + ex.getMessage());
        }
        return "redirect:/outils";
    }

    @GetMapping("/export/operations.xlsx")
    public ResponseEntity<byte[]> operations(@RequestParam int annee) throws Exception {
        return download(excel.operations(annee), XLSX, "operations_" + annee + ".xlsx");
    }
    @GetMapping("/export/clients.xlsx")
    public ResponseEntity<byte[]> clients() throws Exception {
        return download(excel.clients(), XLSX, "clients.xlsx");
    }
    @GetMapping("/export/tournees.xlsx")
    public ResponseEntity<byte[]> tournees(@RequestParam int annee) throws Exception {
        return download(excel.tournees(annee), XLSX, "tournees_" + annee + ".xlsx");
    }
    @GetMapping("/export/recus.xlsx")
    public ResponseEntity<byte[]> recus(@RequestParam int annee) throws Exception {
        return download(excel.pendingReceipts(annee), XLSX, "recus_a_envoyer_" + annee + ".xlsx");
    }
    @GetMapping("/statistiques/completes.pdf")
    public ResponseEntity<byte[]> statistiquesCompletes(@RequestParam int annee) throws Exception {
        return download(reports.completeStatistics(annee), MediaType.APPLICATION_PDF, "statistiques_completes_" + annee + ".pdf");
    }

    @GetMapping("/statistiques/actuelles.pdf")
    public ResponseEntity<byte[]> statistiquesActuelles(@RequestParam int annee) throws Exception {
        return download(reports.currentStatistics(annee), MediaType.APPLICATION_PDF, "statistiques_actuelles_" + annee + ".pdf");
    }

    @GetMapping("/statistiques/evolution-ca.pdf")
    public ResponseEntity<byte[]> evolutionChiffreAffaires() throws Exception {
        return download(reports.annualRevenueStatistics(), MediaType.APPLICATION_PDF, "evolution_chiffre_affaires.pdf");
    }

    @GetMapping("/statistiques/{type}.pdf")
    public ResponseEntity<byte[]> statistiques(@PathVariable String type, @RequestParam int annee) throws Exception {
        return download(reports.statistics(type, annee), MediaType.APPLICATION_PDF, "statistiques_" + type + "_" + annee + ".pdf");
    }

    private ResponseEntity<byte[]> download(byte[] content, MediaType type, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(type);
        headers.setContentDisposition(ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build());
        return ResponseEntity.ok().headers(headers).contentLength(content.length).body(content);
    }
}
