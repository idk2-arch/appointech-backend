package com.appointech.appointech_backend.infrastructure.dto;

import java.time.LocalDateTime;

public record ApiResponse<T>(boolean exito, T datos, String mensaje, LocalDateTime timestamp) {

    public static <T> ApiResponse<T> exito(T datos) {
        return new ApiResponse<>(true, datos, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> exito(T datos, String mensaje) {
        return new ApiResponse<>(true, datos, mensaje, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> error(String mensaje) {
        return new ApiResponse<>(false, null, mensaje, LocalDateTime.now());
    }
}