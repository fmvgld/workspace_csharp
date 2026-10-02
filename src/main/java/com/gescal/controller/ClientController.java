package com.gescal.controller;

import com.gescal.entity.AdresseEntity;
import com.gescal.entity.ClientEntity;
import com.gescal.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clients")
public class ClientController {
    private final ClientRepository clients;
    public ClientController(ClientRepository clients) { this.clients = clients; }

    @GetMapping
    public String liste(@RequestParam(required = false, defaultValue = "") String q, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("clients", clients.findAll(q));
        return "clients";
    }

    @GetMapping("/nouveau")
    public String nouveau(Model model) {
        model.addAttribute("client", new ClientEntity(0, "", "", "", "", new AdresseEntity(0, "", "", "", "")));
        return "client-form";
    }

    @GetMapping("/{id}/modifier")
    public String modifier(@PathVariable long id, Model model) {
        model.addAttribute("client", clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable")));
        return "client-form";
    }

    @PostMapping("/enregistrer")
    public String enregistrer(@RequestParam(required = false) Long id, @RequestParam String nom,
            @RequestParam(defaultValue = "") String prenom, @RequestParam(defaultValue = "") String mail,
            @RequestParam(defaultValue = "") String remarque, @RequestParam(defaultValue = "") String nomRue,
            @RequestParam(defaultValue = "") String complement,
            @RequestParam(defaultValue = "") String cp, @RequestParam(defaultValue = "") String ville,
            RedirectAttributes redirect) {
        validate(id, nom, nomRue, cp, ville);
        clients.save(id, nom.trim(), prenom.trim(), mail.trim(), remarque.trim(), nomRue.trim(), complement.trim(), cp.trim(), ville.trim());
        redirect.addFlashAttribute("success", id == null ? "Client créé." : "Client mis à jour.");
        return "redirect:/clients";
    }

    private void validate(Long id, String nom, String nomRue, String cp, String ville) {
        if (nom == null || nom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom est obligatoire.");
        }
        if (id == null && (nomRue == null || nomRue.isBlank() ||
                cp == null || !cp.matches("\\d{5}") || ville == null || ville.isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom, la rue, un code postal à 5 chiffres et la ville sont obligatoires.");
        }
        if (id != null && cp != null && !cp.isBlank() && !cp.matches("\\d{5}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code postal doit contenir 5 chiffres.");
        }
    }

    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable long id, RedirectAttributes redirect) {
        if (clients.deleteById(id)) redirect.addFlashAttribute("success", "Client supprimé.");
        else redirect.addFlashAttribute("error", "Ce client possède des opérations et ne peut pas être supprimé.");
        return "redirect:/clients";
    }
}
