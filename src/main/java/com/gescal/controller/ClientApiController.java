package com.gescal.controller;

import com.gescal.entity.ClientEntity;
import com.gescal.repository.ClientRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients")
public class ClientApiController {
    private final ClientRepository clients;

    public ClientApiController(ClientRepository clients) { this.clients = clients; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClientOption create(@RequestParam String nom,
            @RequestParam(defaultValue = "") String prenom,
            @RequestParam(defaultValue = "") String mail,
            @RequestParam(defaultValue = "") String remarque,
            @RequestParam(defaultValue = "") String nomRue,
            @RequestParam(defaultValue = "") String complement,
            @RequestParam(defaultValue = "") String cp,
            @RequestParam(defaultValue = "") String ville) {
        if (nom == null || nom.isBlank() || nomRue == null || nomRue.isBlank() ||
                cp == null || !cp.matches("\\d{5}") || ville == null || ville.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le nom, la rue, un code postal à 5 chiffres et la ville sont obligatoires.");
        }
        long id = clients.save(null, nom.trim(), prenom.trim(), mail.trim(), remarque.trim(),
                nomRue.trim(), complement.trim(), cp.trim(), ville.trim());
        ClientEntity client = clients.findById(id).orElseThrow();
        return new ClientOption(client.id(), client.nomComplet().trim(), client.ville());
    }

    public record ClientOption(long id, String label, String ville) { }
}
