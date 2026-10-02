package com.gescal.service;

import com.gescal.dto.ReceiptData;
import com.gescal.entity.ClientEntity;
import com.gescal.entity.OperationEntity;
import com.gescal.entity.TourneeEntity;
import com.gescal.repository.ClientRepository;
import com.gescal.repository.OperationRepository;
import com.gescal.repository.TourneeRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;

@Service
public class ExcelExportService {
    private final OperationRepository operations;
    private final ClientRepository clients;
    private final TourneeRepository tournees;

    public ExcelExportService(OperationRepository operations, ClientRepository clients, TourneeRepository tournees) {
        this.operations = operations;
        this.clients = clients;
        this.tournees = tournees;
    }

    public byte[] pendingReceipts(int annee) throws Exception {
        List<ReceiptData> rows = operations.findPendingReceipts(annee);
        return workbook("Reçus à envoyer",
                new String[]{"N° opération", "Nom", "Prénom", "Adresse", "Courriel", "Année", "Montant", "Tournée", "Règlement"},
                rows.stream().map(r -> new Object[]{r.operationId(), r.nom(), r.prenom(), r.adresseComplete(), r.mail(), r.annee(), r.montant(), r.tournee(), r.reglement()}).toList());
    }

    public byte[] operations(int annee) throws Exception {
        List<OperationEntity> rows = operations.findAll(annee, null);
        return workbook("Opérations " + annee,
                new String[]{"N°", "Date", "Nom", "Prénom", "Tournée", "Montant", "Règlement", "Reçu", "Enveloppe", "Reçu envoyé", "Date d'envoi"},
                rows.stream().map(o -> new Object[]{o.id(), o.dateSaisie(), o.nom(), o.prenom(), o.libelleTournee(), o.montant(), o.libelleReglement(), yes(o.recu()), yes(o.enveloppe()), yes(o.recuEnvoye()), o.dateEnvoi()}).toList());
    }

    public byte[] clients() throws Exception {
        List<ClientEntity> rows = clients.findAll(null);
        return workbook("Clients", new String[]{"N°", "Nom", "Prénom", "Courriel", "Adresse", "Remarque"},
                rows.stream().map(c -> new Object[]{c.id(), c.nom(), c.prenom(), c.mail(), c.adresseComplete(), c.remarque()}).toList());
    }

    public byte[] tournees(int annee) throws Exception {
        List<TourneeEntity> rows = tournees.findAll(String.valueOf(annee));
        return workbook("Tournées " + annee,
                new String[]{"N°", "Année", "Numéro", "Libellé"},
                rows.stream().map(t -> new Object[]{t.id(), t.annee(), t.numero(), t.libelle()}).toList());
    }

    private String yes(boolean value) { return value ? "Oui" : "Non"; }

    private byte[] workbook(String sheetName, String[] headers, List<Object[]> rows) throws Exception {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Row header = sheet.createRow(0);
            for (int column = 0; column < headers.length; column++) {
                Cell cell = header.createCell(column);
                cell.setCellValue(headers[column]);
                cell.setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (Object[] values : rows) {
                Row row = sheet.createRow(rowIndex++);
                for (int column = 0; column < values.length; column++) setValue(row.createCell(column), values[column]);
            }
            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(0, rowIndex - 1), 0, headers.length - 1));
            for (int column = 0; column < headers.length; column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 700, 16000));
            }
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void setValue(Cell cell, Object value) {
        if (value == null) cell.setBlank();
        else if (value instanceof Number number) cell.setCellValue(number.doubleValue());
        else cell.setCellValue(value.toString());
    }
}
