package com.neueda.__Code_Titans.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.neueda.__Code_Titans.entity.PaymentHistory;
import com.neueda.__Code_Titans.entity.Payments;
import com.neueda.__Code_Titans.service.PaymentService;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PaymentController(paymentService)).build();
    }

    @Test
    void createPayment_whenCreated_returnsJsonEnvelope() throws Exception {
        Payments created = new Payments();
        created.setPaymentId(10L);
        created.setStatus("CREATED");
        created.setAmount(new BigDecimal("120.50"));

        when(paymentService.createPayment(anyLong(), anyLong(), any(BigDecimal.class), any(), any(), any()))
                .thenReturn(created);

        String requestBody = """
                {
                  "sourceAccountId": 1,
                  "destinationAccountId": 2,
                  "amount": 120.50,
                  "currency": "USD",
                  "reference": "invoice",
                  "idempotencyKey": "idem-123"
                }
                """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment created"))
                .andExpect(jsonPath("$.data.paymentId").value(10))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void createPayment_whenValidationFails_returnsBadRequestEnvelope() throws Exception {
        Payments failed = new Payments();
        failed.setPaymentId(11L);
        failed.setStatus("FAILED");
        failed.setErrorCode("INVALID_AMOUNT");

        when(paymentService.createPayment(anyLong(), anyLong(), any(BigDecimal.class), any(), any(), any()))
                .thenReturn(failed);

        String requestBody = """
                {
                  "sourceAccountId": 1,
                  "destinationAccountId": 2,
                  "amount": -10,
                  "currency": "USD",
                  "reference": "bad",
                  "idempotencyKey": "idem-124"
                }
                """;

        mockMvc.perform(post("/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment validation failed"))
                .andExpect(jsonPath("$.data.errorCode").value("INVALID_AMOUNT"));
    }

    @Test
    void getPaymentById_whenMissing_returnsNotFoundEnvelope() throws Exception {
        when(paymentService.getPaymentById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/payments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Payment not found"))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void updatePaymentStatus_whenInvalidStatus_returnsBadRequestEnvelope() throws Exception {
        when(paymentService.updatePaymentStatus(eq(5L), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Unsupported status: BAD"));

        String requestBody = """
                {
                  "status": "BAD",
                  "changedBy": "qa",
                  "remarks": "invalid"
                }
                """;

        mockMvc.perform(put("/payments/5/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Unsupported status: BAD"));
    }

    @Test
    void getPaymentHistory_whenExists_returnsHistoryEnvelope() throws Exception {
        Payments payment = new Payments();
        payment.setPaymentId(7L);
        payment.setStatus("SENT");

        PaymentHistory history = new PaymentHistory();
        history.setPaymentId(7L);
        history.setOldStatus("VALIDATED");
        history.setNewStatus("SENT");

        when(paymentService.getPaymentById(7L)).thenReturn(Optional.of(payment));
        when(paymentService.getPaymentHistory(7L)).thenReturn(List.of(history));

        mockMvc.perform(get("/payments/7/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment history fetched"))
                .andExpect(jsonPath("$.data[0].paymentId").value(7))
                .andExpect(jsonPath("$.data[0].newStatus").value("SENT"));
    }
}

