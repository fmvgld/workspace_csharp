package com.gescal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.gescal.service.ExcelExportService;
import com.gescal.service.JasperReportService;
import com.gescal.service.ReceiptService;
import com.gescal.service.BackupService;
import com.gescal.service.WorkingYearService;
import com.gescal.service.DatabaseConfigurationService;
import com.gescal.repository.TourneeRepository;
import com.gescal.repository.ArchiveRepository;
import com.gescal.repository.OperationRepository;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@SpringBootTest
@AutoConfigureMockMvc
class GesCalApplicationTests {

	@Autowired
	private MockMvc mockMvc;
	@Autowired private ReceiptService receipts;
	@Autowired private ExcelExportService excel;
	@Autowired private JasperReportService reports;
	@Autowired private JdbcTemplate jdbc;
	@Autowired private TourneeRepository tournees;
	@Autowired private OperationRepository operations;
	@Autowired private ArchiveRepository archives;

	@Test
	void contextLoads() {
	}

	@Test
	void formulaireOperationProposeLeChoixClientAvecAdresse() throws Exception {
		mockMvc.perform(get("/operations/nouvelle"))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("id=\"selected-client\""),
						containsString("id=\"client-results\""),
						containsString("id=\"client-address\""),
						containsString("id=\"client-picker-dialog\""),
						containsString("id=\"validate-client-choice\""),
						containsString("id=\"reglement-id\""),
						containsString("id=\"montant\""))));
	}

	@Test
	void nouvelleOperationLimiteLesTourneesALAnneeCourante() throws Exception {
		mockMvc.perform(get("/operations/nouvelle"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("(2026)")))
				.andExpect(content().string(not(containsString("(2025)"))));
	}

	@Test
	void clientsOntUneRechercheAutomatiqueEtRestentModifiables() throws Exception {
		mockMvc.perform(get("/clients"))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("/js/clients.js?v=1"),
						containsString("class=\"client-row\""),
						containsString("data-edit-url="))));
		mockMvc.perform(get("/clients/676/modifier"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Modifier le client")));
	}

	@Test
	void tourneesSouvrentSurLAnneeCourante() throws Exception {
		mockMvc.perform(get("/tournees"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString(">2026<")))
				.andExpect(content().string(not(containsString(">2025<"))));
	}

	@Test
	void anneeDeTravailEstPersistante(@TempDir Path temporaryDirectory) throws Exception {
		Path configuration = temporaryDirectory.resolve("gescal.properties");
		Files.writeString(configuration, "gescal.database.path=C:/data/gescal.db\n");
		WorkingYearService service = new WorkingYearService(2026, configuration.toString());
		service.setAndPersist(2025);
		assertEquals(2025, service.get());
		assertTrue(Files.readString(configuration).contains("gescal.working-year=2025"));
	}

	@Test
	void cheminDeBaseEstValideEtPersistant(@TempDir Path temporaryDirectory) throws Exception {
		Path compatible = temporaryDirectory.resolve("compatible.db");
		createDatabaseForPathTest(compatible, false);
		Path oldDatabase = temporaryDirectory.resolve("ancienne.db");
		createDatabaseForPathTest(oldDatabase, true);
		Path configuration = temporaryDirectory.resolve("gescal.properties");
		Files.writeString(configuration, "gescal.database.path=C:/data/ancienne-config.db\n");

		DatabaseConfigurationService service = new DatabaseConfigurationService(
				oldDatabase.toString(), configuration.toString());
		service.setAndPersist(compatible);

		assertEquals(compatible.toAbsolutePath().normalize(), service.configuredPath());
		assertTrue(service.restartRequired());
		assertTrue(Files.readString(configuration).contains(
				"gescal.database.path=" + compatible.toAbsolutePath().toString().replace('\\', '/')));
		assertThrows(IllegalArgumentException.class, () -> service.setAndPersist(oldDatabase));
	}

	@Test
	void toutesLesPagesPrincipalesSontAccessibles() throws Exception {
		String[] pages = {
				"/", "/clients", "/clients/nouveau", "/tournees", "/tournees/nouvelle",
				"/distributions", "/distributions/nouvelle", "/operations",
				"/operations/nouvelle", "/statistiques", "/recus", "/outils"
		};
		for (String page : pages) {
			mockMvc.perform(get(page)).andExpect(status().isOk());
		}
	}

	@Test
	void afficheLesRecusEtLaisseLePdfDisponible() throws Exception {
		mockMvc.perform(get("/recus").param("annee", "2026"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("Reçus à traiter")));
	}

	@Test
	void afficheUneFenetreUniquementApresUnEchecDEnvoi() throws Exception {
		mockMvc.perform(get("/recus").param("annee", "2026"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("id=\"receipt-send-error-popup\""))));

		mockMvc.perform(post("/recus/envoyer")
				.param("ids", "999999999")
				.param("annee", "2026"))
				.andExpect(status().is3xxRedirection())
				.andExpect(flash().attributeExists("receiptSendError"));

		mockMvc.perform(get("/recus").param("annee", "2026")
				.flashAttr("receiptSendError", "Opération de test en erreur"))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("id=\"receipt-send-error-popup\""),
						containsString("Opération de test en erreur"))));
	}

	@Test
	@Transactional
	void refuseDOuvrirUneOperationDAnneeArchivee() throws Exception {
		if (!archives.isArchived(2025)) archives.archive(2025);
		long archivedOperationId = operations.findAll(2025, "").get(0).id();
		mockMvc.perform(get("/operations").param("annee", "2025"))
				.andExpect(status().isOk())
				.andExpect(content().string(allOf(
						containsString("Cette année est archivée"),
						not(containsString("/operations/" + archivedOperationId + "/modifier")))));

		mockMvc.perform(get("/operations/" + archivedOperationId + "/modifier"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/operations?annee=2025"));
	}

	private static void createDatabaseForPathTest(Path path, boolean oldAddressSchema) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:sqlite:" + path);
		JdbcTemplate database = new JdbcTemplate(dataSource);
		database.execute("create table adresses(idadresse integer primary key, " +
				(oldAddressSchema ? "numrue text," : "") + "nomrue text,complement text,cp text,ville text)");
		database.execute("create table clients(idclient integer primary key)");
		database.execute("create table operations(idoperation integer primary key)");
		database.execute("create table tournees(idtournee integer primary key)");
		database.execute("create table reglements(idreglement integer primary key)");
		database.execute("create table distribution(iddistrib integer primary key)");
	}

	@Test
	void genereLesDocumentsHistoriques() throws Exception {
		byte[] receiptPdf = receipts.pdf(1);
		byte[] statisticsPdf = reports.statistics("reglements", 2026);
		byte[] completeStatisticsPdf = reports.completeStatistics(2026);
		byte[] currentStatisticsPdf = reports.currentStatistics(2026);
		byte[] annualRevenuePdf = reports.annualRevenueStatistics();
		byte[] operationsXlsx = excel.operations(2026);
		byte[] clientsXlsx = excel.clients();
		byte[] tourneesXlsx = excel.tournees(2026);

		assertTrue(receiptPdf.length > 1_000);
		assertEquals("%PDF", new String(receiptPdf, 0, 4));
		assertEquals("%PDF", new String(statisticsPdf, 0, 4));
		assertEquals("%PDF", new String(completeStatisticsPdf, 0, 4));
		assertEquals("%PDF", new String(currentStatisticsPdf, 0, 4));
		assertEquals("%PDF", new String(annualRevenuePdf, 0, 4));
		assertEquals((byte) 'P', operationsXlsx[0]);
		assertEquals((byte) 'K', operationsXlsx[1]);
		assertEquals((byte) 'P', clientsXlsx[0]);
		assertEquals((byte) 'K', clientsXlsx[1]);
		assertEquals((byte) 'P', tourneesXlsx[0]);
		assertEquals((byte) 'K', tourneesXlsx[1]);
	}

	@Test
	void creeUneSauvegardeSqliteCoherente(@TempDir Path temporaryDirectory) throws Exception {
		Path backup = new BackupService(jdbc, temporaryDirectory.toString()).backup();
		assertTrue(Files.isRegularFile(backup));
		assertTrue(Files.size(backup) > 100_000);
	}

	@Test
	@Transactional
	void creeUnClientDepuisLeFormulaireOperationPuisAnnuleLaTransaction() throws Exception {
		mockMvc.perform(post("/api/clients")
				.param("nom", "CLIENT TEST AUTOCOMPLETION")
				.param("nomRue", "Rue de la République")
				.param("cp", "38560")
				.param("ville", "Jarrie"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").isNumber())
				.andExpect(jsonPath("$.label").value("CLIENT TEST AUTOCOMPLETION"))
				.andExpect(jsonPath("$.ville").value("Jarrie"));
	}

	@Test
	@Transactional
	void refuseUnClientSansAdresseObligatoire() throws Exception {
		mockMvc.perform(post("/api/clients")
				.param("nom", "CLIENT INCOMPLET")
				.param("ville", "Jarrie"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void refuseUneTourneeDupliqueePourLaMemeAnnee() {
		assertThrows(IllegalArgumentException.class,
				() -> tournees.save(null, 1, "Champ Village", "2026"));
	}

	@Test
	void archiveUneAnneeEtBloqueOperationsEtTournees(@TempDir Path temporaryDirectory) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource("jdbc:sqlite:" + temporaryDirectory.resolve("archive-test.db"));
		JdbcTemplate isolatedJdbc = new JdbcTemplate(dataSource);
		isolatedJdbc.execute("create table operations(idoperation integer primary key, anneeop integer)");
		isolatedJdbc.execute("create table tournees(idtournee integer primary key, numtournee integer, libtournee text, annee text)");
		isolatedJdbc.update("insert into operations(idoperation,anneeop) values(1,2026)");
		isolatedJdbc.update("insert into tournees(idtournee,numtournee,libtournee,annee) values(1,1,'Test','2026')");

		ArchiveRepository isolatedArchives = new ArchiveRepository(isolatedJdbc);
		isolatedArchives.archive(2026);
		OperationRepository isolatedOperations = new OperationRepository(isolatedJdbc, isolatedArchives);
		TourneeRepository isolatedTournees = new TourneeRepository(isolatedJdbc, isolatedArchives);

		assertTrue(isolatedArchives.isArchived(2026));
		assertThrows(IllegalStateException.class, () -> isolatedOperations.save(null, "2026-01-01", 1, 1, 10, 1, false, false, false, false, 2026, null));
		assertThrows(IllegalStateException.class, () -> isolatedTournees.save(null, 2, "Nouvelle", "2026"));
	}

	@Test
	void telechargeLeRecuPdfDepuisUneOperation() throws Exception {
		mockMvc.perform(get("/recus/1/pdf"))
				.andExpect(status().isOk())
				.andExpect(content().contentType(MediaType.APPLICATION_PDF));
	}

}
