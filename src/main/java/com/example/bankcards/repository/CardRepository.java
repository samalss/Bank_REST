// в пакете com.example.bankcards.repository
package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface CardRepository extends JpaRepository<Card, UUID> {
    Page<Card> findByOwnerId(UUID ownerId, Pageable pageable);
    List<Card> findByExpiryDateBeforeAndStatusIs(LocalDate expiryDate, CardStatus status);
    Page<Card> findByOwnerIdAndStatusNot(UUID ownerId, CardStatus status, Pageable pageable);
}