package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/cards")
    public ResponseEntity<Page<CardResponse>> getAllCards(Pageable pageable) {
        Page<CardResponse> cards = adminService.getAllCards(pageable);
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/cards/{id}/block")
    public ResponseEntity<CardResponse> blockCard(@PathVariable UUID id) {
        CardResponse blockedCard = adminService.blockCard(id);
        return ResponseEntity.ok(blockedCard);
    }

    @PostMapping("/cards/{id}/activate")
    public ResponseEntity<CardResponse> activateCard(@PathVariable UUID id) {
        CardResponse activatedCard = adminService.activateCard(id);
        return ResponseEntity.ok(activatedCard);
    }

    @DeleteMapping("/cards/{id}")
    public ResponseEntity<Void> deleteCard(@PathVariable UUID id) {
        adminService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    public ResponseEntity<Page<UserResponse>> getAllUsers(Pageable pageable) {
        Page<UserResponse> users = adminService.getAllUsers(pageable);
        return ResponseEntity.ok(users);
    }

    @PostMapping("/users/{id}/block")
    public ResponseEntity<UserResponse> blockUser(@PathVariable UUID id) {
        UserResponse blockedUser = adminService.blockUser(id);
        return ResponseEntity.ok(blockedUser);
    }

    @PostMapping("/users/{id}/activate")
    public ResponseEntity<UserResponse> activateUser(@PathVariable UUID id) {
        UserResponse activatedUser = adminService.activateUser(id);
        return ResponseEntity.ok(activatedUser);
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        adminService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/{userId}/cards")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponse>> getCardsByUserId(@PathVariable UUID userId, Pageable pageable) {
        Page<CardResponse> cards = adminService.getCardsByUserId(userId, pageable);
        return ResponseEntity.ok(cards);
    }
}