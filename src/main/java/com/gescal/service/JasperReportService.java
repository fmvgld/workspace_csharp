package com.gescal.service;

import com.gescal.dto.ReceiptData;
import com.gescal.dto.StatLine;
import com.gescal.dto.AnnualRevenueLine;
import com.gescal.repository.StatistiqueRepository;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class JasperReportService {
    private final StatistiqueRepository statistics;
    private final Path logoPath;
    private final Path signaturePath;
    private volatile JasperReport receiptReport;
    private volatile JasperReport statisticsReport;
    private volatile JasperReport completeStatisticsReport;
    private volatile JasperReport annualRevenueReport;
    private volatile JasperReport currentStatisticsReport;

    public JasperReportService(StatistiqueRepository statistics,
            @Value("${gescal.receipt.logo:}") String logo,
            @Value("${gescal.receipt.signature:}") String signature) {
        this.statistics = statistics;
        this.logoPath = logo == null || logo.isBlank() ? null : Path.of(logo);
        this.signaturePath = signature == null || signature.isBlank() ? null : Path.of(signature);
    }

    public byte[] receipt(ReceiptData data) throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("NOM", data.nomComplet());
        params.put("ADRESSE1", value(data.nomRue()));
        params.put("ADRESSE2", value(data.complement()));
        params.put("VILLE", join(data.cp(), data.ville()));
        params.put("MONTANT", data.montant());
        params.put("REGLEMENT", value(data.reglement()));
        params.put("TOURNEE", value(data.tournee()));
        params.put("ANNEE", data.annee());
        try (InputStream logo = openIfExists(logoPath); InputStream signature = openIfExists(signaturePath)) {
            params.put("LOGO", logo);
            params.put("SIGNATURE", signature);
            JasperPrint print = JasperFillManager.fillReport(receiptTemplate(), params, new JREmptyDataSource(1));
            return JasperExportManager.exportReportToPdf(print);
        }
    }

    public byte[] statistics(String type, int annee) throws Exception {
        String normalized = type.toLowerCase(Locale.ROOT);
        List<Map<String, Object>> source;
        String title;
        switch (normalized) {
            case "reglements" -> { source = statistics.parReglement(annee); title = "Statistiques par règlement"; }
            case "tournees" -> { source = statistics.parTournee(annee); title = "Statistiques par tournée"; }
            case "villes" -> { source = statistics.parVille(annee); title = "Statistiques par ville"; }
            default -> throw new IllegalArgumentException("Type de statistique inconnu : " + type);
        }
        double total = statistics.dashboard(annee).get("montant").doubleValue();
        List<StatLine> lines = lines(source, total);
        Map<String, Object> params = new HashMap<>();
        params.put("TITLE", title);
        params.put("YEAR", annee);
        params.put("TOTAL", total);
        JasperPrint print = JasperFillManager.fillReport(statisticsTemplate(), params, new JRBeanCollectionDataSource(lines));
        return JasperExportManager.exportReportToPdf(print);
    }

    public byte[] completeStatistics(int annee) throws Exception {
        double total = statistics.dashboard(annee).get("montant").doubleValue();
        Map<String, Object> params = new HashMap<>();
        params.put("YEAR", annee);
        params.put("TOTAL", total);
        params.put("VILLES", lines(statistics.parVille(annee), total));
        params.put("REGLEMENTS", lines(statistics.parReglement(annee), total));
        params.put("TOURNEES", lines(statistics.parTournee(annee), total));
        params.put(JRParameter.REPORT_LOCALE, Locale.FRANCE);
        JasperPrint print = JasperFillManager.fillReport(completeStatisticsTemplate(), params, new JREmptyDataSource(1));
        return JasperExportManager.exportReportToPdf(print);
    }

    public byte[] currentStatistics(int annee) throws Exception {
        Map<String, Number> dashboard = statistics.dashboard(annee);
        double total = dashboard.get("montant").doubleValue();
        Map<String, Object> params = new HashMap<>();
        params.put("YEAR", annee);
        params.put("OPERATIONS", dashboard.get("operations").longValue());
        params.put("TOTAL", total);
        params.put("RECEIPTS_PENDING", dashboard.get("recusAEnvoyer").longValue());
        params.put("REGLEMENTS", lines(statistics.parReglement(annee), total));
        params.put("TOURNEES", lines(statistics.parTournee(annee), total));
        params.put("VILLES", lines(statistics.parVille(annee), total));
        params.put(JRParameter.REPORT_LOCALE, Locale.FRANCE);
        JasperPrint print = JasperFillManager.fillReport(currentStatisticsTemplate(), params, new JREmptyDataSource(1));
        return JasperExportManager.exportReportToPdf(print);
    }

    public byte[] annualRevenueStatistics() throws Exception {
        List<AnnualRevenueLine> annualLines = annualRevenueLines();
        double total = annualLines.stream().mapToDouble(AnnualRevenueLine::getMontant).sum();
        Map<String, Object> params = new HashMap<>();
        params.put("TOTAL", total);
        params.put("ANNUAL_LINES", annualLines);
        params.put(JRParameter.REPORT_LOCALE, Locale.FRANCE);
        JasperPrint print = JasperFillManager.fillReport(annualRevenueTemplate(), params, new JREmptyDataSource(1));
        return JasperExportManager.exportReportToPdf(print);
    }

    private List<AnnualRevenueLine> annualRevenueLines() {
        List<Map<String, Object>> source = statistics.chiffreAffairesParAnnee();
        java.util.ArrayList<AnnualRevenueLine> result = new java.util.ArrayList<>();
        Double previous = null;
        for (Map<String, Object> row : source) {
            double amount = number(row.get("montant")).doubleValue();
            Double evolution = previous == null || previous == 0 ? null : (amount - previous) / previous;
            result.add(new AnnualRevenueLine(number(row.get("annee")).intValue(),
                    number(row.get("nombre")).intValue(), amount, evolution));
            previous = amount;
        }
        return result;
    }

    private List<StatLine> lines(List<Map<String, Object>> source, double total) {
        return source.stream().map(row -> {
            double amount = number(row.get("montant")).doubleValue();
            return new StatLine(String.valueOf(row.get("libelle")), number(row.get("nombre")).intValue(), amount,
                    total == 0 ? 0 : amount / total);
        }).toList();
    }

    private Number number(Object value) { return value instanceof Number n ? n : 0; }
    private String join(String first, String second) { return (value(first) + " " + value(second)).trim(); }
    private String value(String value) { return value == null ? "" : value; }
    private InputStream openIfExists(Path path) throws Exception { return path != null && Files.isRegularFile(path) ? Files.newInputStream(path) : null; }

    private JasperReport receiptTemplate() throws Exception {
        if (receiptReport == null) synchronized (this) {
            if (receiptReport == null) receiptReport = compile("reports/receipt.jrxml");
        }
        return receiptReport;
    }

    private JasperReport statisticsTemplate() throws Exception {
        if (statisticsReport == null) synchronized (this) {
            if (statisticsReport == null) statisticsReport = compile("reports/statistics.jrxml");
        }
        return statisticsReport;
    }

    private JasperReport completeStatisticsTemplate() throws Exception {
        if (completeStatisticsReport == null) synchronized (this) {
            if (completeStatisticsReport == null) completeStatisticsReport = compile("reports/statistics-complete.jrxml");
        }
        return completeStatisticsReport;
    }

    private JasperReport annualRevenueTemplate() throws Exception {
        if (annualRevenueReport == null) synchronized (this) {
            if (annualRevenueReport == null) annualRevenueReport = compile("reports/statistics-annual-revenue.jrxml");
        }
        return annualRevenueReport;
    }

    private JasperReport currentStatisticsTemplate() throws Exception {
        if (currentStatisticsReport == null) synchronized (this) {
            if (currentStatisticsReport == null) currentStatisticsReport = compile("reports/statistics-current.jrxml");
        }
        return currentStatisticsReport;
    }

    private JasperReport compile(String resource) throws Exception {
        try (InputStream input = new ClassPathResource(resource).getInputStream()) {
            return JasperCompileManager.compileReport(input);
        }
    }
}
