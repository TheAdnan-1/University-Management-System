package com.example.ums;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class UserController {
    private final UserRepo repo;
    private final PasswordEncoder enc;

    UserController(UserRepo repo, PasswordEncoder enc) { this.repo = repo; this.enc = enc; }

    private Role role(String type) {
        return switch (type) {
            case "admins" -> Role.ADMIN;
            case "teachers" -> Role.TEACHER;
            case "students" -> Role.STUDENT;
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        };
    }

    private AppUser find(String type, Long id) {
        return repo.findById(id).filter(u -> u.getRole() == role(type))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/me")
    Map<String, String> me(Principal p) {
        AppUser u = repo.findByUsername(p.getName()).orElseThrow();
        return Map.of("username", u.getUsername(), "role", u.getRole().name());
    }

    @GetMapping("/{type}")
    List<AppUser> all(@PathVariable String type) { return repo.findByRole(role(type)); }

    @GetMapping("/{type}/{id}")
    AppUser one(@PathVariable String type, @PathVariable Long id) { return find(type, id); }

    @PostMapping("/{type}")
    ResponseEntity<AppUser> create(@PathVariable String type, @RequestBody AppUser u) {
        Role r = role(type);
        if (u.getUsername() == null || u.getPassword() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "username and password required");
        if (repo.existsByUsername(u.getUsername()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "username taken");
        u.setRole(r);
        u.setPassword(enc.encode(u.getPassword()));
        return ResponseEntity.status(HttpStatus.CREATED).body(repo.save(u));
    }

    @PutMapping("/{type}/{id}")
    AppUser update(@PathVariable String type, @PathVariable Long id, @RequestBody AppUser in) {
        AppUser u = find(type, id);
        if (in.getName() != null) u.setName(in.getName());
        if (in.getUsername() != null && !in.getUsername().equals(u.getUsername())) {
            if (repo.existsByUsername(in.getUsername()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "username taken");
            u.setUsername(in.getUsername());
        }
        if (in.getPassword() != null && !in.getPassword().isBlank()) u.setPassword(enc.encode(in.getPassword()));
        return repo.save(u);
    }

    @DeleteMapping("/{type}/{id}")
    ResponseEntity<Void> delete(@PathVariable String type, @PathVariable Long id) {
        repo.delete(find(type, id));
        return ResponseEntity.noContent().build();
    }
}
