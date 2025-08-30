package com.example.bankcards.controller;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.service.AdminService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AdminControllerTest{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAllCards_AdminRole_ReturnsOk() throws Exception {
        Page<CardResponse> mockPage = new PageImpl<>(Collections.emptyList());
        when(adminService.getAllCards(any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testGetAllUsers_AdminRole_ReturnsOk() throws Exception {
        Page<UserResponse> mockPage = new PageImpl<>(Collections.emptyList());
        when(adminService.getAllUsers(any())).thenReturn(mockPage);

        mockMvc.perform(get("/api/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBlockUser_AdminRole_ReturnsOk() throws Exception {
        UUID userId = UUID.randomUUID();
        UserResponse mockResponse = new UserResponse();
        when(adminService.blockUser(userId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/users/{id}/block", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBlockUser_AdminBlocksOwnAccount_ReturnsForbidden() throws Exception {
        UUID adminId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(adminService).blockUser(adminId);

        mockMvc.perform(post("/api/admin/users/{id}/block", adminId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_AdminRole_ReturnsNoContent() throws Exception {
        UUID userId = UUID.randomUUID();
        doNothing().when(adminService).deleteUser(userId);

        mockMvc.perform(delete("/api/admin/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testDeleteUser_AdminDeletesOwnAccount_ReturnsForbidden() throws Exception {
        UUID adminId = UUID.randomUUID();
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN)).when(adminService).deleteUser(adminId);

        mockMvc.perform(delete("/api/admin/users/{id}", adminId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = {"ADMIN"})
    void testBlockCard_AdminRole_ReturnsOk() throws Exception {
        UUID cardId = UUID.randomUUID();
        CardResponse mockResponse = new CardResponse();
        when(adminService.blockCard(cardId)).thenReturn(mockResponse);

        mockMvc.perform(post("/api/admin/cards/{id}/block", cardId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user", roles = {"USER"})
    void testAccessAdminEndpoints_UserRole_ReturnsForbidden() throws Exception {
        UUID cardId = UUID.randomUUID();

        mockMvc.perform(get("/api/admin/cards")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }
}