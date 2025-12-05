package com.ticketblitz.common.dto;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ApiResponseTest {

    @Test
    void shouldCreateSuccessResponse() {
        String data = "test data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo(data);
        assertThat(response.getError()).isNull();
    }

    @Test
    void shouldCreateErrorResponse() {
        ApiResponse<Void> response = ApiResponse.error("TEST_ERROR", "Error message");

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getError()).isNotNull();
        assertThat(response.getError().getCode()).isEqualTo("TEST_ERROR");
    }
}