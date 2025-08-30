package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CreateCardRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.service.CardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class CardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CardService cardService;

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testCreateCard_ValidRequest_ReturnsCreated() throws Exception {
        CardResponse mockResponse = new CardResponse();
        when(cardService.createCard(any(CreateCardRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetMyCards_AuthenticatedUser_ReturnsOk() throws Exception {
        Page<CardResponse> mockPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 10), 0);
        when(cardService.getCardsForCurrentUser(any(PageRequest.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetCardById_UserOwnsCard_ReturnsOk() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardResponse mockResponse = new CardResponse();
        when(cardService.getCardByIdForCurrentUser(cardId)).thenReturn(mockResponse);

        mockMvc.perform(get("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testBlockCard_UserOwnsCard_ReturnsOk() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardResponse mockResponse = new CardResponse();
        when(cardService.blockCard(cardId)).thenReturn(mockResponse);

        mockMvc.perform(patch("/api/cards/{id}/block", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testDeleteMyCard_UserOwnsCard_ReturnsNoContent() throws Exception {
        UUID cardId = UUID.randomUUID();
        doNothing().when(cardService).deleteMyCard(cardId);

        mockMvc.perform(delete("/api/cards/{id}", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testTransferFunds_ValidRequest_ReturnsOk() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setSourceCardId(UUID.randomUUID());
        request.setDestinationCardId(UUID.randomUUID());
        request.setAmount(new BigDecimal("10.00"));

        doNothing().when(cardService).transferFunds(any(TransferRequest.class));

        mockMvc.perform(post("/api/cards/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void testCreateCard_Unauthenticated_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/cards")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")) // Send an empty JSON object directly
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "testuser", roles = {"USER"})
    void testGetCardById_UserDoesNotOwnCard_ReturnsForbidden() throws Exception {
        UUID cardId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(cardService).getCardByIdForCurrentUser(cardId);

        mockMvc.perform(get("/api/cards/{id}", cardId))
                .andExpect(status().isForbidden());
    }
}